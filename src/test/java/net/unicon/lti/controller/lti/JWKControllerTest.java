package net.unicon.lti.controller.lti;

import jakarta.servlet.http.HttpServletRequest;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class JWKControllerTest {

    @InjectMocks
    private JWKController jwkController;

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private HttpServletRequest req;

    private RSAPublicKey publicKey;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        publicKey = (RSAPublicKey) kp.getPublic();
        String pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(publicKey.getEncoded()) + "\n-----END PUBLIC KEY-----\n";
        when(ltiDataService.getOwnPublicKey()).thenReturn(pem);
    }

    @Test
    public void testServesTheToolPublicKey() throws Exception {
        Map<String, List<Map<String, Object>>> keys = jwkController.jwk(req, new ExtendedModelMap());

        assertEquals(1, keys.size());
        List<Map<String, Object>> keyList = keys.get("keys");
        assertEquals(1, keyList.size());
        Map<String, Object> key = keyList.get(0);
        assertEquals("RSA", key.get("kty"));
        assertEquals(TextConstants.DEFAULT_KID, key.get("kid"));
        assertEquals("RS256", key.get("alg"));
        assertEquals("sig", key.get("use"));
        byte[] modulus = Base64.getUrlDecoder().decode((String) key.get("n"));
        // the leading sign byte is dropped for D2L
        assertEquals(256, modulus.length);
        assertEquals(publicKey.getModulus(), new BigInteger(1, modulus));
        assertEquals(publicKey.getPublicExponent(), new BigInteger(Base64.getUrlDecoder().decode((String) key.get("e"))));
    }
}
