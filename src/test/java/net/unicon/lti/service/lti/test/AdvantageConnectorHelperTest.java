package net.unicon.lti.service.lti.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.service.lti.impl.AdvantageConnectorHelperImpl;
import net.unicon.lti.utils.AGSScope;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Captor
    ArgumentCaptor<MappingJackson2HttpMessageConverter> converterArgumentCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTokenizedRequestEntityForScores() {
        LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
        ltiAdvantageToken.setAccess_token("test-token");
        Score score = new Score();
        HttpEntity<Score> httpEntity = advantageConnectorHelper.createTokenizedRequestEntity(ltiAdvantageToken, score);

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
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ResponseEntity<LTIAdvantageToken> responseEntity = new ResponseEntity<>(ltiAdvantageToken, HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class))).thenReturn(responseEntity);

            LTIAdvantageToken ltiAdvantageTokenResponse = advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());

            verify(ltijwtService).generateTokenRequestJWT(platformDeployment);
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class));
            assertEquals(ltiAdvantageTokenResponse, ltiAdvantageToken);
        } catch (Exception e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testGetTokenForAGSScoresInMoodleFormat() {
        try {
            PlatformDeployment platformDeployment = new PlatformDeployment();
            platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
            when(ltijwtService.generateTokenRequestJWT(platformDeployment)).thenReturn("jwt");
            List<HttpMessageConverter<?>> messageConverters = Mockito.mock(ArrayList.class);
            when(restTemplate.getMessageConverters()).thenReturn(messageConverters);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.TEXT_HTML);
            ResponseEntity<LTIAdvantageToken> responseEntity = new ResponseEntity<>(ltiAdvantageToken, responseHeaders, HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class))).thenReturn(responseEntity);

            LTIAdvantageToken ltiAdvantageTokenResponse = advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());

            verify(ltijwtService).generateTokenRequestJWT(platformDeployment);
            verify(messageConverters).add(converterArgumentCaptor.capture());
            MappingJackson2HttpMessageConverter converter = converterArgumentCaptor.getValue();
            assertTrue(converter.getSupportedMediaTypes().contains(MediaType.TEXT_HTML));
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class));
            assertEquals(ltiAdvantageTokenResponse, ltiAdvantageToken);
        } catch (Exception e) {
            e.printStackTrace();
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
                LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
                ResponseEntity<LTIAdvantageToken> responseEntity = new ResponseEntity<>(ltiAdvantageToken, HttpStatus.BAD_REQUEST);
                when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class))).thenReturn(responseEntity);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });

            verify(ltijwtService).generateTokenRequestJWT(any(PlatformDeployment.class));
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class));
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
                when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class))).thenReturn(null);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });

            verify(ltijwtService).generateTokenRequestJWT(any(PlatformDeployment.class));
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class));
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
            verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(LTIAdvantageToken.class));
            verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the token. Exception"), any(GeneralSecurityException.class));
        } catch (GeneralSecurityException | IOException e) {
            fail("Should not throw exception.");
        }
    }
}
