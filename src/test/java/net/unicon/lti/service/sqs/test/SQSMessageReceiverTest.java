package net.unicon.lti.service.sqs.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
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

public class SQSMessageReceiverTest {

    @InjectMocks
    private SQSMessageReceiver sqsMessageReceiver = new SQSMessageReceiver();

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

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
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"line_item_url\": \"test5\", \"score\": -0.2}";

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithScoreGreaterThanOne() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"line_item_url\": \"test5\", \"score\": 1.1}";

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIToken.class), anyString(), any(Score.class));
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

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithInvalidSchema() {
        try {
            String sqsLineItemJson = "{\"clientId\": \"test1\", \"userId\": \"test2\", \"deploymentId\": \"test3\", \"issuer\": \"test4\", \"lineItemUrl\": \"test5\", \"score\": 0.6}";

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithInvalidPlatformDeployment() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"line_item_url\": \"test5\", \"score\": 0.8}";

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, never()).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessageWithoutAccessToken() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"line_item_url\": \"test5\", \"score\": 0.8}";
            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, never()).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreFailure() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"line_item_url\": \"test5\", \"score\": 0.8}";
            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIToken ltiToken = new LTIToken();
            ltiToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            when(advantageAGSService.postScore(any(LTIToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, never()).acknowledge();
            verify(visibility, times(1)).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testReceiveMessagePostScoreSuccess() {
        try {
            String sqsLineItemJson = "{\"client_id\": \"test1\", \"user_id\": \"test2\", \"deployment_id\": \"test3\", \"issuer\": \"test4\", \"line_item_url\": \"test5\", \"score\": 0.8}";
            PlatformDeployment platformDeployment = new PlatformDeployment();
            List<PlatformDeployment> platformDeploymentList = Collections.singletonList(platformDeployment);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString())).thenReturn(platformDeploymentList);
            LTIToken ltiToken = new LTIToken();
            ltiToken.setAccess_token("test-token");
            when(advantageAGSService.getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class))).thenReturn(ltiToken);
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
            when(advantageAGSService.postScore(any(LTIToken.class), anyString(), any(Score.class))).thenReturn(response);

            sqsMessageReceiver.receiveMessage(sqsLineItemJson, 1, visibility, acknowledgment);
            verify(platformDeploymentRepository, times(1)).findByIssAndClientIdAndDeploymentId(anyString(), anyString(), anyString());
            verify(advantageAGSService, times(1)).getToken(eq(AGSScope.AGS_SCORES_SCOPE), any(PlatformDeployment.class));
            verify(advantageAGSService, times(1)).postScore(any(LTIToken.class), anyString(), any(Score.class));
            verify(acknowledgment, times(1)).acknowledge();
            verify(visibility, never()).extend(30);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }
}
