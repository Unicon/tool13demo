package net.unicon.lti.service.lti.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.service.lti.impl.AdvantageConnectorHelperImpl;
import net.unicon.lti.utils.AGSScope;
import net.unicon.lti.utils.TextConstants;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdvantageConnectorHelperTest {

    @InjectMocks
    AdvantageConnectorHelper advantageConnectorHelper = new AdvantageConnectorHelperImpl();

    @Mock
    LTIJWTService ltijwtService;

    @Mock
    RestTemplate restTemplate;

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTokenizedRequestEntityForScores() {
        LTIToken ltiToken = new LTIToken();
        ltiToken.setAccess_token("test-token");
        Score score = new Score();
        HttpEntity<Score> httpEntity = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score);

        assertEquals(httpEntity.getBody(), score);
        assertEquals(httpEntity.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0), TextConstants.BEARER + "test-token");
        assertEquals(httpEntity.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0), "application/vnd.ims.lis.v1.score+json");
    }

    @Test
    public void testCreateTokenizedRequestEntityForScoresThrowsNPE() {
        Score score = new Score();

        assertThrows(NullPointerException.class, () -> {
            advantageConnectorHelper.createTokenizedRequestEntity(null, score);
        });
    }

    @Test
    public void testGetTokenForAGSScores() {
        try {
            PlatformDeployment platformDeployment = new PlatformDeployment();
            platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
            when(ltijwtService.generateTokenRequestJWT(platformDeployment)).thenReturn("jwt");
            LTIToken ltiToken = new LTIToken();
            ResponseEntity<LTIToken> responseEntity = new ResponseEntity<>(ltiToken, HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(responseEntity);

            LTIToken ltiTokenResponse = advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());

            verify(ltijwtService).generateTokenRequestJWT(platformDeployment);
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            assertEquals(ltiTokenResponse, ltiToken);
        } catch (Exception e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testGetTokenForAGSScoresPostIsUnsuccessful() {
        try {
            Exception exception = assertThrows(ConnectionException.class, () -> {
                PlatformDeployment platformDeployment = new PlatformDeployment();
                platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
                when(ltijwtService.generateTokenRequestJWT(platformDeployment)).thenReturn("jwt");
                LTIToken ltiToken = new LTIToken();
                ResponseEntity<LTIToken> responseEntity = new ResponseEntity<>(ltiToken, HttpStatus.BAD_REQUEST);
                when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(responseEntity);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });

            verify(ltijwtService).generateTokenRequestJWT(any(PlatformDeployment.class));
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            assertEquals(exception.getMessage(), "Can't get the token: " + HttpStatus.BAD_REQUEST.getReasonPhrase());
        } catch (GeneralSecurityException | IOException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testGetTokenForAGSScoresPostIsNull() {
        try {
            Exception exception = assertThrows(ConnectionException.class, () -> {
                PlatformDeployment platformDeployment = new PlatformDeployment();
                platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
                when(ltijwtService.generateTokenRequestJWT(platformDeployment)).thenReturn("jwt");
                when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(null);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });

            verify(ltijwtService).generateTokenRequestJWT(any(PlatformDeployment.class));
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            assertEquals(exception.getMessage(), "Problem getting the token");
        } catch (GeneralSecurityException | IOException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testGetTokenForAGSScoresOAuthFails() {
        try {
            assertThrows(ConnectionException.class, () -> {
                PlatformDeployment platformDeployment = new PlatformDeployment();
                platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
                when(ltijwtService.generateTokenRequestJWT(platformDeployment)).thenThrow(GeneralSecurityException.class);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });
            verify(ltijwtService).generateTokenRequestJWT(any(PlatformDeployment.class));
            verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the token. Exception"), any(GeneralSecurityException.class));
        } catch (GeneralSecurityException | IOException e) {
            fail("Should not throw exception.");
        }
    }
}
