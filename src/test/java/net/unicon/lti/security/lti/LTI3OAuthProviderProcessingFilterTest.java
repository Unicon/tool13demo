package net.unicon.lti.security.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;

import static net.unicon.lti.utils.TextConstants.LTI_NONCE_COOKIE_NAME;
import static net.unicon.lti.utils.TextConstants.LTI_STATE_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class LTI3OAuthProviderProcessingFilterTest {
    private static final String TEST_STATE = "test-state";
    private static final Cookie NONCE_COOKIE = new Cookie(LTI_NONCE_COOKIE_NAME, "nonce-value");
    private static final Cookie JSESSIONID_COOKIE = new Cookie("JSESSIONID", "test");

    private LTI3OAuthProviderProcessingFilter lti3OAuthFilter;

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private LTIJWTService ltijwtService;

    @Mock
    FilterChain mockChain;

    @Mock
    private ServletRequest servletRequest;

    @Mock
    LoginInitiationDTO loginInitiationDTO;

    private MockHttpServletRequest req = new MockHttpServletRequest();

    private MockHttpServletResponse res = new MockHttpServletResponse();

    private KeyPair kp;

    @BeforeEach()
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        lti3OAuthFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
    }

    @Test
    public void testDoFilterWithoutHttpServletRequest() {
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {lti3OAuthFilter.doFilter(servletRequest, res, mockChain);}
        );
        assertEquals("LTI request MUST be an HttpServletRequest (cannot only be a ServletRequest)", exception.getMessage());
    }

    @Test
    public void testDoFilterWithStorageTarget() {
        req.setParameter("lti_storage_target", "_parent");
        Cookie[] cookies = req.getCookies();
        try {

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            String validState = LtiOidcUtils.generateState(ltiDataService, Collections.singletonMap("nonce", "nonce-value"), loginInitiationDTO, "client-id", "deployment-id");
            req.setParameter("state", validState);
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(validState);
            when(ltijwtService.validateState(validState)).thenReturn(finalClaims);

            lti3OAuthFilter.doFilter(req, res, mockChain);
        } catch (GeneralSecurityException | ServletException | IOException e) {
            e.printStackTrace();
        }
        assertEquals(cookies, null); // because we have lti_storage_target, cookies being null should not throw an error.
    }

    @Test
    public void testDoFilterWithoutCookies() {
        req.setParameter("lti_storage_target");

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {lti3OAuthFilter.doFilter(req, res, mockChain);}
        );
        assertEquals("LTI request doesn't contain any cookies", exception.getMessage());
    }

    @Test
    public void testDoFilterWithEmptyStateCookie() {
        Cookie[] cookies = {NONCE_COOKIE, JSESSIONID_COOKIE};
        req.setCookies(cookies);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {lti3OAuthFilter.doFilter(req, res, mockChain);}
        );
        assertEquals("LTI state could not be found", exception.getMessage());
    }

    @Test
    public void testDoFilterWithStateCookieThatDoesNotMatchRequest() {
        Cookie[] cookies = {NONCE_COOKIE, JSESSIONID_COOKIE, new Cookie(LTI_STATE_COOKIE_NAME, TEST_STATE)};
        req.setCookies(cookies);
        req.setParameter("state", "different-test-state");

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {lti3OAuthFilter.doFilter(req, res, mockChain);}
        );
        assertEquals("LTI request doesn't contain the expected state", exception.getMessage());
    }

    @Test
    public void testDoFilterWithInvalidState() {
        Cookie[] cookies = {NONCE_COOKIE, JSESSIONID_COOKIE, new Cookie(LTI_STATE_COOKIE_NAME, TEST_STATE)};
        req.setCookies(cookies);
        req.setParameter("state", TEST_STATE);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {lti3OAuthFilter.doFilter(req, res, mockChain);}
        );

        assertEquals("LTI state is invalid", exception.getMessage());
        Mockito.verify(ltijwtService).validateState(eq(TEST_STATE));
    }

    @Test
    public void testDoFilterWithValidState() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            String validState = LtiOidcUtils.generateState(ltiDataService, Collections.singletonMap("nonce", "nonce-value"), loginInitiationDTO, "client-id", "deployment-id");
            Cookie[] cookies = {NONCE_COOKIE, JSESSIONID_COOKIE, new Cookie(LTI_STATE_COOKIE_NAME, validState)};
            req.setCookies(cookies);
            req.setParameter("state", validState);
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(validState);
            when(ltijwtService.validateState(validState)).thenReturn(finalClaims);

            lti3OAuthFilter.doFilter(req, res, mockChain);

            Mockito.verify(ltijwtService).validateState(eq(validState));
            Mockito.verify(mockChain).doFilter(eq(req), eq(res));

        } catch (GeneralSecurityException | IOException | ServletException e) {
            fail();
        }

    }
}
