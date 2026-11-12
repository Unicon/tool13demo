package net.unicon.lti.service.lti;

import static org.mockito.ArgumentMatchers.anyString;
import com.nimbusds.jose.KeySourceException;
import net.unicon.lti.utils.lti.PlatformJwks;
import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.impl.LTIJWTServiceImpl;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class LTIJWTServiceImplTest {

    private static final String ISS = "https://platform.example.com";
    private static final String CLIENT_ID = "client-1";
    private static final String KID = "platform-kid";
    private static final String JWKS_URL = "https://platform.example.com/jwks";

    private static KeyPair toolKeys;
    private static KeyPair otherKeys;

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    private LTIJWTServiceImpl service;

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
        AllRepositories repos = new AllRepositories(null, null, null, null, null, null, null, platformDeploymentRepository, null, null);
        when(ltiDataService.getRepos()).thenReturn(repos);
        when(ltiDataService.getOwnPrivateKey()).thenReturn(privatePem(toolKeys));
        when(ltiDataService.getOwnPublicKey()).thenReturn(publicPem(toolKeys));
        service = new LTIJWTServiceImpl(ltiDataService);
    }

    private static String privatePem(KeyPair keys) {
        return "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
    }

    private static String publicPem(KeyPair keys) {
        return "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n";
    }

    private static String token(KeyPair keys, Date expiration) {
        return Jwts.builder().header().keyId(KID).and()
                .issuer(ISS).subject("user-1").audience().add(CLIENT_ID).and()
                .expiration(expiration)
                .claim("custom", "value")
                .signWith(keys.getPrivate()).compact();
    }

    private static Date inOneHour() {
        return new Date(System.currentTimeMillis() + 3600_000L);
    }

    private PlatformDeployment deploymentWithJwks(String jwks) {
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setJwksEndpoint(jwks);
        when(platformDeploymentRepository.findByIssAndClientId(ISS, CLIENT_ID)).thenReturn(List.of(deployment));
        return deployment;
    }

    @Test
    public void validateStateAcceptsATokenSignedWithTheToolKey() {
        Jws<Claims> jws = service.validateState(token(toolKeys, inOneHour()));
        assertEquals("user-1", jws.getPayload().getSubject());
        assertEquals("value", jws.getPayload().get("custom"));
    }

    @Test
    public void validateStateRejectsAnotherSignature() {
        assertThrows(SignatureException.class, () -> service.validateState(token(otherKeys, inOneHour())));
    }

    @Test
    public void validateStateRejectsAnExpiredToken() {
        assertThrows(ExpiredJwtException.class, () -> service.validateState(token(toolKeys, new Date(System.currentTimeMillis() - 60_000L))));
    }

    @Test
    public void validateStateFailsWhenTheToolPublicKeyIsInvalid() {
        when(ltiDataService.getOwnPublicKey()).thenReturn("-----BEGIN PUBLIC KEY-----AAAA-----END PUBLIC KEY-----");
        assertThrows(JwtException.class, () -> service.validateState(token(toolKeys, inOneHour())));
    }

    @Test
    public void validateNonceStateAcceptsATokenSignedWithTheToolKey() {
        Jws<Claims> jws = service.validateNonceState(token(toolKeys, inOneHour()));
        assertEquals(ISS, jws.getPayload().getIssuer());
    }

    @Test
    public void validateNonceStateRejectsAnotherSignature() {
        assertThrows(SignatureException.class, () -> service.validateNonceState(token(otherKeys, inOneHour())));
    }

    @Test
    public void validateNonceStateFailsWhenTheToolPublicKeyIsInvalid() {
        when(ltiDataService.getOwnPublicKey()).thenReturn("-----BEGIN PUBLIC KEY-----AAAA-----END PUBLIC KEY-----");
        assertThrows(JwtException.class, () -> service.validateNonceState(token(toolKeys, inOneHour())));
    }

    @Test
    public void validateJWTUsesTheKeyFromThePlatformJwks() throws Exception {
        deploymentWithJwks(JWKS_URL);
        String jwt = token(otherKeys, inOneHour());

        try (MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenReturn(otherKeys.getPublic());
            Jws<Claims> jws = service.validateJWT(jwt, CLIENT_ID);
            assertEquals("user-1", jws.getPayload().getSubject());
            assertEquals(CLIENT_ID, Iterables.getOnlyElement(jws.getPayload().getAudience()));
        }
    }

    @Test
    public void validateJWTFailsWhenThePlatformIsUnknown() {
        when(platformDeploymentRepository.findByIssAndClientId(ISS, CLIENT_ID)).thenReturn(new ArrayList<>());
        assertThrows(JwtException.class, () -> service.validateJWT(token(otherKeys, inOneHour()), CLIENT_ID));
    }

    @Test
    public void validateJWTFailsWithoutAJwksEndpoint() {
        deploymentWithJwks(null);
        assertThrows(JwtException.class, () -> service.validateJWT(token(otherKeys, inOneHour()), CLIENT_ID));
    }

    @Test
    public void validateJWTFailsWhenTheJwksCannotBeLoaded() {
        deploymentWithJwks(JWKS_URL);
        String jwt = token(otherKeys, inOneHour());
        try (MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenThrow(new KeySourceException("unreachable"));
            assertThrows(JwtException.class, () -> service.validateJWT(jwt, CLIENT_ID));
        }
    }

    @Test
    public void validateJWTFailsWhenTheKidIsNotInTheJwks() throws Exception {
        deploymentWithJwks(JWKS_URL);
        String jwt = token(otherKeys, inOneHour());
        try (MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenReturn(null);
            assertThrows(JwtException.class, () -> service.validateJWT(jwt, CLIENT_ID));
        }
    }

    @Test
    public void generateClientAssertionUsesTheTokenAudWhenSet() throws Exception {
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setClientId(CLIENT_ID);
        deployment.setoAuth2TokenUrl("https://platform.example.com/token");
        deployment.setoAuth2TokenAud("https://platform.example.com/aud");

        String jwt = service.generateStateOrClientAssertionJWT(deployment);

        Jws<Claims> jws = Jwts.parser().verifyWith(toolKeys.getPublic()).build().parseSignedClaims(jwt);
        assertEquals(TextConstants.DEFAULT_KID, jws.getHeader().getKeyId());
        assertEquals("JWT", jws.getHeader().getType());
        assertEquals(CLIENT_ID, jws.getPayload().getIssuer());
        assertEquals(CLIENT_ID, jws.getPayload().getSubject());
        assertEquals("https://platform.example.com/aud", Iterables.getOnlyElement(jws.getPayload().getAudience()));
        assertNotNull(jws.getPayload().get("jti"));
        assertEquals(3600_000L, jws.getPayload().getExpiration().getTime() - jws.getPayload().getIssuedAt().getTime());
    }

    @Test
    public void generateClientAssertionFallsBackToTheTokenUrl() throws Exception {
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setClientId(CLIENT_ID);
        deployment.setoAuth2TokenUrl("https://platform.example.com/token");

        String jwt = service.generateStateOrClientAssertionJWT(deployment);

        Jws<Claims> jws = Jwts.parser().verifyWith(toolKeys.getPublic()).build().parseSignedClaims(jwt);
        assertEquals("https://platform.example.com/token", Iterables.getOnlyElement(jws.getPayload().getAudience()));
    }

    @Test
    public void generateStateNonceTokenCarriesTheHash() throws Exception {
        String jwt = service.generateStateNonceTokenJWT("the-hash");

        Jws<Claims> jws = service.validateNonceState(jwt);
        assertEquals("the-hash", jws.getPayload().get("expected_hash"));
        assertEquals("Our own tool", jws.getPayload().getIssuer());
        assertEquals("Our own tool", jws.getPayload().getSubject());
        assertEquals("Our own tool", Iterables.getOnlyElement(jws.getPayload().getAudience()));
        assertEquals(TextConstants.DEFAULT_KID, jws.getHeader().getKeyId());
    }

    @Test
    public void generateFailsWithAnUnsupportedPrivateKey() {
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not a pem");
        assertThrows(java.security.GeneralSecurityException.class, () -> service.generateStateNonceTokenJWT("hash"));
        assertThrows(java.security.GeneralSecurityException.class, () -> service.generateStateOrClientAssertionJWT(mock(PlatformDeployment.class)));
    }
}
