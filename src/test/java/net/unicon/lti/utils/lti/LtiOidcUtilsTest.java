package net.unicon.lti.utils.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

public class LtiOidcUtilsTest {
    @Mock
    LTIDataService ltiDataService;

    @Mock
    PlatformDeployment platformDeployment;

    @Mock
    LoginInitiationDTO loginInitiationDTO;

    @Configuration
    static class ContextConfiguration {

    }

    @BeforeEach()
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateState() {
        LocalDateTime currentLocalDate = LocalDateTime.now(ZoneId.of("Z"));
        try (MockedStatic<LocalDateTime> topDateTimeUtilMock = Mockito.mockStatic(LocalDateTime.class)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(platformDeployment.getIss()).thenReturn("test-iss");
            when(platformDeployment.getClientId()).thenReturn("client-id");
            when(loginInitiationDTO.getIss()).thenReturn("test-iss");
            when(loginInitiationDTO.getLoginHint()).thenReturn("login-hint");
            when(loginInitiationDTO.getLtiMessageHint()).thenReturn("lti-message-hint");
            when(loginInitiationDTO.getTargetLinkUri()).thenReturn("target-link-uri");
            Map<String, String> authRequestMap = Collections.singletonMap("nonce", "nonce-value");
            topDateTimeUtilMock.when(() -> LocalDateTime.now(ZoneId.of("Z"))).thenReturn(currentLocalDate);

            String state = LtiOidcUtils.generateState(ltiDataService, authRequestMap, loginInitiationDTO, "client-id", "deployment-id");

            // validate that ltiToken was signed using private key and contains expected payload
            Jws<Claims> parsedLtiToken = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(state);
            assertEquals(TextConstants.DEFAULT_KID, parsedLtiToken.getHeader().get("kid"));
            assertEquals("JWT", parsedLtiToken.getHeader().getType());
            Claims ltiTokenClaims = parsedLtiToken.getBody();
            assertEquals("ltiStarter", ltiTokenClaims.getIssuer());
            assertEquals("test-iss", ltiTokenClaims.getSubject());
            assertEquals("client-id", ltiTokenClaims.getAudience());
            assertEquals("nonce-value", ltiTokenClaims.getId());
            assertEquals("test-iss", ltiTokenClaims.get("original_iss"));
            assertEquals("login-hint", ltiTokenClaims.get("loginHint"));
            assertEquals("lti-message-hint", ltiTokenClaims.get("ltiMessageHint"));
            assertEquals("target-link-uri", ltiTokenClaims.get("targetLinkUri"));
            assertEquals("client-id", ltiTokenClaims.get("clientId"));
            assertEquals("deployment-id", ltiTokenClaims.get("ltiDeploymentId"));
            assertEquals("/oidc/login_initiations", ltiTokenClaims.get("controller"));
            // Dates are equal to the nearest minute
            assertEquals(DateUtils.round(Date.from(currentLocalDate.plusHours(1).toInstant(ZoneOffset.UTC)), Calendar.MINUTE), DateUtils.round(ltiTokenClaims.getExpiration(), Calendar.MINUTE));
            assertEquals(DateUtils.round(Date.from(currentLocalDate.toInstant(ZoneOffset.UTC)), Calendar.MINUTE), DateUtils.round(ltiTokenClaims.getNotBefore(), Calendar.MINUTE));
            assertEquals(DateUtils.round(Date.from(currentLocalDate.toInstant(ZoneOffset.UTC)), Calendar.MINUTE), DateUtils.round(ltiTokenClaims.getIssuedAt(), Calendar.MINUTE));

        } catch (GeneralSecurityException e) {
            fail("GeneralSecurityException should not be thrown.");
        }
    }

}