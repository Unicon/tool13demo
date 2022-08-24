package net.unicon.lti.service.sqs.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.sqs.impl.SQSMessageReceiver;
import net.unicon.lti.utils.AGSScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.Visibility;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Profile("!no-aws")
public class SQSMessageReceiverTest {

    @InjectMocks
    private SQSMessageReceiver sqsMessageReceiver = new SQSMessageReceiver();

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private LtiContextRepository ltiContextRepository;

    @Mock
    private AdvantageAGSService advantageAGSService;

    @Mock
    private Visibility visibility;

    @Mock
    private Acknowledgment acknowledgment;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testReceiveMessageWithNegativeScore() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"lineitem_url\": \"test5\", \"score\": -0.2}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithScoreGreaterThanOne() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"lineitem_url\": \"test5\", \"score\": 1.1}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithInvalidJson() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": ";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithInvalidSchema() {
        try {
            String sqsLineItemJson = "{\\\"clientId\\\": \\\"test1\\\", \\\"userId\\\": \\\"test2\\\", \\\"deploymentId\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineItemUrl\\\": \\\"test5\\\", \\\"score\\\": 0.6}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithOldLineItemUrlSchema() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"line_item_url\\\": \\\"test5\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithInvalidPlatformDeployment() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"test5\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithoutAccessToken() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"test5\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreFailure() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"test5\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq(""));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreFailureWithMoodleLineitemUrlFormat() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"http://localhost:8000/mod/lti/services.php/3/lineitems/6/lineitem?type_id=14\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq("http://localhost:8000/mod/lti/services.php/3/lineitems?type_id=14"));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreFailureWithBlackboardLineitemUrlFormat() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"https://example.com/learn/api/v1/lti/courses/_122_1/lineItems/_7454_1\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq("https://example.com/learn/api/v1/lti/courses/_122_1/lineItems"));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreFailureWithCanvasLineitemUrlFormat() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"https://test.instructure.com/api/lti/courses/3348/line_items/503\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq("https://test.instructure.com/api/lti/courses/3348/line_items"));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreFailureWithD2LBrightspaceLineitemUrlFormat() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"https://test.brightspace.com/d2l/api/lti/ags/2.0/deployment/a748e0da-6cd3-4f8d-acf3-73f2e11457c9/orgunit/6836/lineitems/1\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository).findByLineitems(eq("https://test.brightspace.com/d2l/api/lti/ags/2.0/deployment/a748e0da-6cd3-4f8d-acf3-73f2e11457c9/orgunit/6836/lineitems"));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithoutSNSFormatFailure() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"lineitem_url\": \"test5\", \"score\": 0.8}";

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository, times(1)).findByLineitems(any(String.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreSuccess() {
        try {
            String sqsLineItemJson = "{\\\"client_id\\\": \\\"test1\\\", \\\"user_id\\\": \\\"test2\\\", \\\"deployment_id\\\": \\\"test3\\\", \\\"issuer\\\": \\\"test4\\\", \\\"lineitem_url\\\": \\\"test5\\\", \\\"score\\\": 0.8}";
            String snsLineItemJson = generateSNSLineItemJSON(sqsLineItemJson);

            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiAdvantageToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
            when(advantageAGSService.postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(snsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIAdvantageToken.class), anyString(), any(Score.class));
            verify(ltiContextRepository, never()).findByLineitems(any(String.class));
            verify(acknowledgment, times(1)).acknowledge();
            verify(visibility, never()).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    private String generateSNSLineItemJSON(String sqsLineItemJson) {
        String snsLineItemJson =
                "{\n" +
                "\"Type\" : \"Notification\",\n" +
                "\"MessageId\" : \"f85561b6-dd51-5de6-8d75-3b128d9598d5\",\n" +
                "\"TopicArn\" : \"arn:aws:sns:us-west-2:725843923591:develop-lti13_grade\",\n" +
                "\"Message\" : \"" + sqsLineItemJson + "\",\n" +
                "\"Timestamp\" : \"2022-03-10T19:16:20.157Z\",\n" +
                "\"SignatureVersion\" : \"1\",\n" +
                "\"Signature\" : \"j48wQktGN/UUjwsUK+w7tpn68ANS7/nM8Kwbb848sEZ1Hl7+N1/D8CoDutFHzaT2GbQ+Vh6NFCXtEMyX2LF/UwqqTRqLYknIm7NVCob9/2hADuE6Ix5mPcJKD+Y7nwl6GGu/6I9o+JzVzO2DqOVz7rL3QGnZrJi35RU/SGAxE1NcKl24y6bR+kGPK6O+O3WlyFIPkAP74qIgqdVGtq6v7OJtsDirxBg0JrX4eRNTVMWT+1CK/n78yG8EWs9SARHaLtN+donriYuw59G7ofkwKVmjYniJixpz1hIsyTzP/TZpi+Fn8ZobYMB5WsuUkjnqZTsV45Q/Wm14Ul8rMvUYEg==\",\n" +
                "\"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/SimpleNotificationService-7ff5318490ec183fbaddaa2a969abfda.pem\",\n" +
                "\"UnsubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-west-2:725843923591:develop-lti13_grade:e4402d2e-ab0c-4fdf-b3a7-dc0d745466e5\"\n" +
                "}";

        return  snsLineItemJson;
    }
}
