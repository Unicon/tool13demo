package net.unicon.lti.service.app;

import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.oauth2.Roles;
import net.unicon.lti.model.oauth2.SecuredInfo;
import net.unicon.lti.service.app.impl.APIJWTServiceImpl;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objenesis.ObjenesisStd;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APIJWTServiceImplTest {

    private static final String LOCAL_URL = "https://tool.example.com";
    private static final List<String> ROLES = List.of(Roles.MEMBERSHIP_INSTRUCTOR);

    private static KeyPair toolKeys;
    private static KeyPair otherKeys;

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private APIDataService apiDataService;

    private APIJWTServiceImpl service;

    @BeforeAll
    public static void generateKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        toolKeys = kpg.generateKeyPair();
        otherKeys = kpg.generateKeyPair();
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ltiDataService.getOwnPrivateKey()).thenReturn("-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(toolKeys.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n");
        when(ltiDataService.getOwnPublicKey()).thenReturn("-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(toolKeys.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n");
        when(ltiDataService.getLocalUrl()).thenReturn(LOCAL_URL);
        service = new APIJWTServiceImpl(ltiDataService, apiDataService);
    }

    private static SecuredInfo withRoles(String... roles) {
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setRoles(List.of(roles));
        return securedInfo;
    }

    @Test
    public void buildJwtCreatesALongLivedTokenWithTheLaunchClaims() throws Exception {
        String token = service.buildJwt(false, ROLES, 10L, "context-key", 20L, "user-1", "nonce-1");

        Jws<Claims> jws = service.validateToken(token);
        Claims claims = jws.getPayload();
        assertEquals(TextConstants.DEFAULT_KID, jws.getHeader().getKeyId());
        assertEquals("JWT", jws.getHeader().getType());
        assertEquals("ISSUER", claims.getIssuer());
        assertEquals("user-1", claims.getSubject());
        assertEquals(LOCAL_URL, Iterables.getOnlyElement(claims.getAudience()));
        assertEquals(10, claims.get("contextId"));
        assertEquals("context-key", claims.get("contextKey"));
        assertEquals(20, claims.get("platformDeploymentId"));
        assertEquals("user-1", claims.get("userId"));
        assertEquals(ROLES, claims.get("roles"));
        assertEquals(false, claims.get("oneUse"));
        assertEquals("nonce-1", claims.get("nonce"));
        assertEquals(3600_000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        verify(apiDataService, never()).addOneUseToken(anyString());
    }

    @Test
    public void buildJwtRegistersAShortLivedOneUseToken() throws Exception {
        String token = service.buildJwt(true, ROLES, 10L, "context-key", 20L, "user-1", "nonce-1");

        Claims claims = service.validateToken(token).getPayload();
        assertEquals(true, claims.get("oneUse"));
        assertEquals(300_000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        verify(apiDataService).addOneUseToken(token);
    }

    @Test
    public void buildJwtFromAnLtiRequest() throws Exception {
        LTI3Request lti = new ObjenesisStd().newInstance(LTI3Request.class);
        LtiContextEntity context = new LtiContextEntity("context-key", new PlatformDeployment(), "title", null);
        context.setContextId(10L);
        lti.setContext(context);
        PlatformDeployment key = new PlatformDeployment();
        key.setKeyId(20L);
        lti.setKey(key);
        lti.setUser(new LtiUserEntity("user-1", null, null));
        lti.setLtiRoles(ROLES);
        lti.setNonce("nonce-1");

        Claims claims = service.validateToken(service.buildJwt(false, lti)).getPayload();

        assertEquals(10, claims.get("contextId"));
        assertEquals("context-key", claims.get("contextKey"));
        assertEquals(20, claims.get("platformDeploymentId"));
        assertEquals("user-1", claims.get("userId"));
        assertEquals("nonce-1", claims.get("nonce"));
        assertEquals(ROLES, claims.get("roles"));
    }

    @Test
    public void validateTokenRejectsAnotherSignature() {
        String token = Jwts.builder().subject("user-1").signWith(otherKeys.getPrivate()).compact();
        assertThrows(SignatureException.class, () -> service.validateToken(token));
    }

    @Test
    public void validateTokenFailsWhenTheToolPublicKeyIsInvalid() throws Exception {
        String token = service.buildJwt(false, ROLES, 10L, "context-key", 20L, "user-1", "nonce-1");
        when(ltiDataService.getOwnPublicKey()).thenReturn("-----BEGIN PUBLIC KEY-----AAAA-----END PUBLIC KEY-----");
        assertThrows(JwtException.class, () -> service.validateToken(token));
    }

    @Test
    public void refreshTokenIssuesANewReusableToken() throws Exception {
        String token = service.buildJwt(false, ROLES, 10L, "context-key", 20L, "user-1", "nonce-1");

        String refreshed = service.refreshToken(token);

        Jws<Claims> jws = service.validateToken(refreshed);
        Claims claims = jws.getPayload();
        assertEquals(TextConstants.DEFAULT_KID, jws.getHeader().getKeyId());
        assertEquals("ISSUER", claims.getIssuer());
        assertEquals("user-1", claims.getSubject());
        assertEquals(LOCAL_URL, Iterables.getOnlyElement(claims.getAudience()));
        assertEquals(10, claims.get("contextId"));
        assertEquals("context-key", claims.get("contextKey"));
        assertEquals(20, claims.get("platformDeploymentId"));
        assertEquals("user-1", claims.get("userId"));
        assertEquals(ROLES, claims.get("roles"));
        assertEquals(false, claims.get("oneUse"));
        assertEquals("nonce-1", claims.get("nonce"));
        // an hour, like a token buildJwt issues
        assertEquals(3600_000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
    }

    @Test
    public void refreshTokenRejectsAOneUseToken() throws Exception {
        // buildJwt stores oneUse as a boolean
        String token = service.buildJwt(true, ROLES, 10L, "context-key", 20L, "user-1", "nonce-1");
        BadTokenException exception = assertThrows(BadTokenException.class, () -> service.refreshToken(token));
        assertEquals("Trying to refresh an one use token", exception.getMessage());
    }

    @Test
    public void refreshTokenRejectsAOneUseTokenWithAStringClaim() {
        String token = Jwts.builder().subject("user-1").claim("oneUse", "true").signWith(toolKeys.getPrivate()).compact();
        assertThrows(BadTokenException.class, () -> service.refreshToken(token));
    }

    @Test
    public void refreshTokenRefreshesATokenWithoutAOneUseClaim() throws Exception {
        String token = Jwts.builder().subject("user-1").audience().add(LOCAL_URL).and().signWith(toolKeys.getPrivate()).compact();

        Claims claims = service.validateToken(service.refreshToken(token)).getPayload();

        assertEquals(false, claims.get("oneUse"));
        assertEquals("user-1", claims.getSubject());
    }

    @Test
    public void extractJwtStringValueReadsTheBearerHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer  abc.def.ghi ");
        request.setParameter("token", "from-query");
        assertEquals("abc.def.ghi", service.extractJwtStringValue(request, true));
        assertEquals("abc.def.ghi", service.extractJwtStringValue(request, false));
    }

    @Test
    public void extractJwtStringValueIgnoresOtherAuthorizationSchemes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        assertNull(service.extractJwtStringValue(request, false));
    }

    @Test
    public void extractJwtStringValueFallsBackToTheQueryParameterWhenAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "   ");
        request.setParameter("token", " from-query ");
        assertEquals("from-query", service.extractJwtStringValue(request, true));
        assertNull(service.extractJwtStringValue(request, false));
    }

    @Test
    public void extractValuesBuildsSecuredInfoFromTheToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + service.buildJwt(false, ROLES, 10L, "context-key", 20L, "user-1", "nonce-1"));

        SecuredInfo securedInfo = service.extractValues(request, false);

        assertEquals("user-1", securedInfo.getUserId());
        assertEquals(20L, securedInfo.getPlatformDeploymentId());
        assertEquals(10L, securedInfo.getContextId());
        assertEquals("context-key", securedInfo.getContextKey());
        assertEquals(ROLES, securedInfo.getRoles());
        assertEquals("nonce-1", securedInfo.getNonce());
    }

    @Test
    public void extractValuesReturnsNullWithoutClaims() {
        APIJWTServiceImpl spied = spy(service);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("token", "abc");
        doReturn(null).when(spied).validateToken("abc");
        assertNull(spied.extractValues(request, true));
    }

    @Test
    public void roleChecks() {
        SecuredInfo admin = withRoles(Roles.ADMIN);
        SecuredInfo instructor = withRoles(Roles.INSTRUCTOR);
        SecuredInfo membershipInstructor = withRoles(Roles.MEMBERSHIP_INSTRUCTOR);
        SecuredInfo learner = withRoles(Roles.LEARNER);
        SecuredInfo membershipLearner = withRoles(Roles.MEMBERSHIP_LEARNER);
        SecuredInfo general = withRoles(Roles.GENERAL);

        assertTrue(service.isAdmin(admin));
        assertFalse(service.isAdmin(general));

        assertTrue(service.isInstructor(instructor));
        assertTrue(service.isInstructor(membershipInstructor));
        assertFalse(service.isInstructor(learner));

        assertTrue(service.isLearner(learner));
        assertTrue(service.isLearner(membershipLearner));
        assertFalse(service.isLearner(instructor));

        assertTrue(service.isGeneral(general));
        assertFalse(service.isGeneral(admin));

        assertTrue(service.isInstructorOrHigher(instructor));
        assertTrue(service.isInstructorOrHigher(admin));
        assertFalse(service.isInstructorOrHigher(learner));

        assertTrue(service.isLearnerOrHigher(learner));
        assertTrue(service.isLearnerOrHigher(admin));
        assertFalse(service.isLearnerOrHigher(general));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void extractTimestampParsesIsoDatesAndIgnoresAnythingElse() throws Exception {
        // private helper that nothing calls yet
        Method extractTimestamp = APIJWTServiceImpl.class.getDeclaredMethod("extractTimestamp", Jws.class, String.class);
        extractTimestamp.setAccessible(true);
        String token = Jwts.builder().claim("dueAt", "2024-01-02T03:04:05").claim("lockAt", "not a date").signWith(toolKeys.getPrivate()).compact();
        Jws<Claims> jws = service.validateToken(token);

        assertEquals(Timestamp.valueOf(LocalDateTime.of(2024, 1, 2, 3, 4, 5)), extractTimestamp.invoke(service, jws, "dueAt"));
        assertNull(extractTimestamp.invoke(service, jws, "lockAt"));
        assertNull(extractTimestamp.invoke(service, jws, "missing"));
    }
}
