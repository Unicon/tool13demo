package net.unicon.lti.service.sqs.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.model.sqs.SQSLineItem;
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

@Slf4j
@Service
@NoArgsConstructor
public class SQSMessageReceiver {
    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    AdvantageAGSService advantageAGSServiceImpl;

    @SqsListener(value = "${cloud.aws.end-point.uri}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveMessage(
            String sqsLineItemJson,
            @Header("ApproximateReceiveCount") int receiveCount,
            Visibility visibility,
            Acknowledgment acknowledgment
    ) {
        log.debug("message received {}", sqsLineItemJson);
        log.debug("Current time in millis: {}", System.currentTimeMillis());

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SQSLineItem sqsLineItem = objectMapper.readValue(sqsLineItemJson, SQSLineItem.class);

            if (sqsLineItem.getScore() < 0 || sqsLineItem.getScore() > 1) {
                throw new IllegalArgumentException("Score given for this line item must be between 0.0 and 1.0, and it is " + sqsLineItem.getScore());
            }

            Score score = sqsLineItem.toScore();

            // get the deployment authorized to post scores
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
                        throw new ConnectionException("Can't post score");
                    }
                } else {
                    throw new BadTokenException("No authorization token for permission to post score.");
                }
            } else {
                throw new DataServiceException("No deployment to retrieve authorization token for permission to post score.");
            }
        } catch (Exception ex) {
            int newTimeout = (int) Math.pow(2,receiveCount - 1) * 30;
            newTimeout = newTimeout < 0 || newTimeout > 43199 ? 43199 : newTimeout;
            visibility.extend(newTimeout);
            log.error("Score failed to post to LMS: {}", sqsLineItemJson);
            log.error("This score has been received from SQS {} times.", receiveCount);
            log.debug("New visibility timeout: {}", newTimeout);
            ex.printStackTrace();
        }
    }
}
