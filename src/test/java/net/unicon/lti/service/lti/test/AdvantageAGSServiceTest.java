package net.unicon.lti.service.lti.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.service.lti.impl.AdvantageAGSServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdvantageAGSServiceTest {

    @InjectMocks
    AdvantageAGSService advantageAGSService = new AdvantageAGSServiceImpl();

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    AdvantageConnectorHelper advantageConnectorHelper;

    @Mock
    RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPostScoreFromSQS() {
        try {
            LTIToken ltiToken = new LTIToken();
            ltiToken.setAccess_token("test-scores-token");
            Score score = new Score();
            when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
            HttpEntity<Score> httpEntity = new HttpEntity<>(score);
            when(advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score)).thenReturn(httpEntity);
            ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenReturn(responseEntity);

            ResponseEntity<Void> response = advantageAGSService.postScore(ltiToken, "https://lms.com/line_item/456", score);
            verify(advantageConnectorHelper).createRestTemplate();
            verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiToken, score);
            verify(restTemplate).exchange(eq("https://lms.com/line_item/456/scores"), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
            assertEquals(response, responseEntity);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testPostScoreFromSQSWithMoodleURLFormat() {
        try {
            LTIToken ltiToken = new LTIToken();
            ltiToken.setAccess_token("test-scores-token");
            Score score = new Score();
            when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
            HttpEntity<Score> httpEntity = new HttpEntity<>(score);
            when(advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score)).thenReturn(httpEntity);
            ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenReturn(responseEntity);

            ResponseEntity<Void> response = advantageAGSService.postScore(ltiToken, "https://lms.com/mod/lti/services.php/3/lineitems/6/lineitem?type_id=17", score);
            verify(advantageConnectorHelper).createRestTemplate();
            verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiToken, score);
            verify(restTemplate).exchange(eq("https://lms.com/mod/lti/services.php/3/lineitems/6/lineitem/scores?type_id=17"), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
            assertEquals(response, responseEntity);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testPostScoreFromSQSThrowsException() {
        LTIToken ltiToken = new LTIToken();
        ltiToken.setAccess_token("test-scores-token");
        Score score = new Score();
        when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
        HttpEntity<Score> httpEntity = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score)).thenReturn(httpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenThrow(RestClientException.class);

        assertThrows(RestClientException.class, () -> {
            advantageAGSService.postScore(ltiToken, "https://lms.com/line_item/456", score);
        });

        verify(advantageConnectorHelper).createRestTemplate();
        verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiToken, score);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
        verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
    }

}
