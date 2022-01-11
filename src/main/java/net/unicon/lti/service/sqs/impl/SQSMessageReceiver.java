package net.unicon.lti.service.sqs.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.model.sqs.SQSLineItem;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.utils.AGSScope;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.Visibility;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@NoArgsConstructor
public class SQSMessageReceiver {
    private static final String END_OF_LINEITEMS_URL_LOWER_CASE = "lineitems";
    private static final String END_OF_LINEITEMS_URL_LOWER_CASE_CANVAS = "line_items";

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    AdvantageAGSService advantageAGSServiceImpl;

    @SqsListener(value = "${lti13.grade-passback-queue}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveMessage(
            String sqsLineItemJson,
            @Header("ApproximateReceiveCount") int receiveCount,
            Visibility visibility,
            Acknowledgment acknowledgment
    ) {
        log.debug("message received {}", sqsLineItemJson);
        log.debug("Current time in millis: {}", System.currentTimeMillis());
        String lineItemUrl = "";

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SQSLineItem sqsLineItem = objectMapper.readValue(sqsLineItemJson, SQSLineItem.class);

            // Save lineitem url for logging
            lineItemUrl = sqsLineItem.getLineItemUrl();

            // Validation
            if (sqsLineItem.getScore() < 0 || sqsLineItem.getScore() > 1) {
                throw new IllegalArgumentException("Score given for this line item must be between 0.0 and 1.0, and it is " + sqsLineItem.getScore());
            }

            // Convert from SQS to IMS AGS format
            Score score = sqsLineItem.toScore();

            // get the registration/deployment authorized to post scores
            List<PlatformDeployment> platformDeployment = platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(
                    sqsLineItem.getIssuer(),
                    sqsLineItem.getClientId(),
                    sqsLineItem.getDeploymentId()
            );

            if (!platformDeployment.isEmpty()) {
                // 1. Get the auth token for the scores endpoint
                LTIToken scoresToken = advantageAGSServiceImpl.getToken(AGSScope.AGS_SCORES_SCOPE, platformDeployment.get(0));

                // 2. Post score
                if (scoresToken != null && !StringUtils.isEmpty(scoresToken.getAccess_token())) {
                    log.debug(TextConstants.TOKEN + scoresToken.getAccess_token());
                    ResponseEntity<Void> response = advantageAGSServiceImpl.postScore(scoresToken, sqsLineItem.getLineItemUrl(), score);
                    HttpStatus status = response.getStatusCode();
                    log.debug(status.name());
                    if (status.is2xxSuccessful()) {
                        acknowledgment.acknowledge(); // Remove message from queue upon success
                    } else {
                        log.error("Response Status: {}: {}", status.value(), status.name());
                        log.error("Response Status Reason: {}", status.getReasonPhrase());
                        log.error("Response Body: {}", response.getBody());
                        throw new ConnectionException("Can't post score");
                    }
                } else {
                    throw new BadTokenException("No authorization token for permission to post score.");
                }
            } else {
                throw new DataServiceException("No registration/deployment to retrieve authorization token for permission to post score.");
            }
        } catch (Exception ex) {
            int newTimeout = (int) Math.pow(2,receiveCount - 1) * 30;
            newTimeout = newTimeout < 0 || newTimeout > 43199 ? 43199 : newTimeout;
            visibility.extend(newTimeout);

            log.error(ex.toString());
            log.error("Score failed to post to LMS: {}", sqsLineItemJson);
            String lineItemsUrl = lineItemUrlToLineItemsUrl(lineItemUrl);
            LtiContextEntity course = ltiContextRepository.findByLineitems(lineItemsUrl);
            if (course != null) {
                log.error("LTI Middleware Course ID: {}", course.getContextId());
                log.error("LMS Course/Context ID: {}", course.getContextKey());
                log.error("Course/Context Title: {}", course.getTitle());
            } else {
                log.error("Context/Course Was Not Found");
            }
            log.error("This score has been received from SQS {} times.", receiveCount);
            log.debug("New visibility timeout: {}", newTimeout);
            ex.printStackTrace();
        }
    }

    private String lineItemUrlToLineItemsUrl(String lineitemUrl) {
        /*
        Moodle AGS Format:
            lineitems=http://localhost:8000/mod/lti/services.php/3/lineitems?type_id=14
            lineitem=http://localhost:8000/mod/lti/services.php/3/lineitems/6/lineitem?type_id=14
        Blackboard AGS Format (from https://docs.blackboard.com/lti/core/id-token):
            lineitems=https://example.com/learn/api/v1/lti/courses/_122_1/lineItems
            lineitem=https://example.com/learn/api/v1/lti/courses/_122_1/lineItems/_7454_1
        Canvas AGS Format:
            lineitems=https://test.instructure.com/api/lti/courses/3348/line_items
            lineitem=https://test.instructure.com/api/lti/courses/3348/line_items/503
        D2l/Brightspace AGS Format (from https://documentation.brightspace.com/EN/integrations/ipsis/LTI%20Advantage/lti_1.3_assignments_grade_services.htm):
            lineitems=https://test.brightspace.com/d2l/api/lti/ags/2.0/deployment/a748e0da-6cd3-4f8d-acf3-73f2e11457c9/orgunit/6836/lineitems
            lineitem=https://test.brightspace.com/d2l/api/lti/ags/2.0/deployment/a748e0da-6cd3-4f8d-acf3-73f2e11457c9/orgunit/6836/lineitems/1
        */

        try {
            String lowerCaseLineitemUrl = lineitemUrl.toLowerCase(Locale.ROOT);
            boolean isCanvasFormat = lowerCaseLineitemUrl.indexOf(END_OF_LINEITEMS_URL_LOWER_CASE_CANVAS) > 0;
            int endIdx = isCanvasFormat ? lowerCaseLineitemUrl.indexOf(END_OF_LINEITEMS_URL_LOWER_CASE_CANVAS) + END_OF_LINEITEMS_URL_LOWER_CASE_CANVAS.length() :
                    lowerCaseLineitemUrl.indexOf(END_OF_LINEITEMS_URL_LOWER_CASE) + END_OF_LINEITEMS_URL_LOWER_CASE.length();
            String atEndIdxPlusLineitems = lineitemUrl.substring(0, endIdx);
            String params = lineitemUrl.indexOf("?") > 0 ? lineitemUrl.substring(lineitemUrl.indexOf("?")) : "";
            log.debug("Calculated lineitems Url: {}", atEndIdxPlusLineitems + params);
            return atEndIdxPlusLineitems + params;
        } catch (Exception e) {
            log.error("Unable to calculate lineItems URL to find course because");
            log.error(e.toString());
            return "";
        }
    }
}
