package net.unicon.lti.utils.oauth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OAuthUtilsTest {
    private static KeyPair keyPair;

    @BeforeAll
    public static void generateKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
    }

    private static String base64(byte[] bytes) {
        return Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(bytes);
    }

    @Test
    public void testLoadPublicKeyWithRealNewlines() throws Exception {
        String pem = "-----BEGIN PUBLIC KEY-----\n" + base64(keyPair.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n";
        RSAPublicKey key = OAuthUtils.loadPublicKey(pem);
        assertArrayEquals(keyPair.getPublic().getEncoded(), key.getEncoded());
    }

    @Test
    public void testLoadPublicKeyWithEscapedNewlines() throws Exception {
        String pem = "-----BEGIN PUBLIC KEY-----\\n" + base64(keyPair.getPublic().getEncoded()).replace("\n", "\\n") + "\\n-----END PUBLIC KEY-----";
        RSAPublicKey key = OAuthUtils.loadPublicKey(pem);
        assertEquals(((RSAPublicKey) keyPair.getPublic()).getModulus(), key.getModulus());
    }

    @Test
    public void testLoadPublicKeyInvalid() {
        String pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString("not a key".getBytes()) + "\n-----END PUBLIC KEY-----";
        assertThrows(GeneralSecurityException.class, () -> OAuthUtils.loadPublicKey(pem));
    }

    @Test
    public void testLoadPrivateKey() throws Exception {
        String pem = "-----BEGIN PRIVATE KEY-----\n" + base64(keyPair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
        PrivateKey key = OAuthUtils.loadPrivateKey(pem);
        assertEquals("RSA", key.getAlgorithm());
        assertArrayEquals(keyPair.getPrivate().getEncoded(), key.getEncoded());
    }

    @Test
    public void testLoadPrivateKeyUnsupportedFormat() {
        String pem = "-----BEGIN RSA PRIVATE KEY-----\nabc\n-----END RSA PRIVATE KEY-----";
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, () -> OAuthUtils.loadPrivateKey(pem));
        assertEquals("Not supported format of a private key", e.getMessage());
    }

    @Test
    public void testConstructorThrows() throws Exception {
        Constructor<OAuthUtils> constructor = OAuthUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException e = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertEquals("Utility class", e.getCause().getMessage());
    }
}
