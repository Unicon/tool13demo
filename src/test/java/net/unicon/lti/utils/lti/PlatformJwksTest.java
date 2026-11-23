package net.unicon.lti.utils.lti;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PlatformJwksTest {

    private static final String KID = "platform-key";

    private final AtomicInteger requests = new AtomicInteger();

    private HttpServer server;
    private KeyPair keys;
    private String jwksUrl;

    @BeforeEach
    public void setUp() throws Exception {
        PlatformJwks.clear();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        byte[] jwks = new JWKSet(new RSAKey.Builder((RSAPublicKey) keys.getPublic()).keyID(KID).build()).toString().getBytes(StandardCharsets.UTF_8);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        server.start();
        jwksUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
        PlatformJwks.clear();
    }

    @Test
    public void testReturnsThePlatformKeyAndCachesTheKeySet() throws Exception {
        assertEquals(keys.getPublic(), PlatformJwks.publicKey(jwksUrl, KID));
        assertEquals(keys.getPublic(), PlatformJwks.publicKey(jwksUrl, KID));
        assertEquals(keys.getPublic(), PlatformJwks.publicKey(jwksUrl, KID));

        assertEquals(1, requests.get());
    }

    @Test
    public void testUnknownKeyIdReturnsNull() throws Exception {
        assertNull(PlatformJwks.publicKey(jwksUrl, "rotated-away"));
    }

    @Test
    public void testMissingKeyIdReturnsNullWithoutFetching() throws Exception {
        assertNull(PlatformJwks.publicKey(jwksUrl, null));
        assertEquals(0, requests.get());
    }

    @Test
    public void testInvalidEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> PlatformJwks.publicKey("not a url", KID));
    }

    @Test
    public void testUnreachableEndpoint() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        assertThrows(JOSEException.class, () -> PlatformJwks.publicKey("http://127.0.0.1:" + closedPort + "/jwks", KID));
    }

    @Test
    public void testCannotBeInstantiated() throws Exception {
        Constructor<PlatformJwks> constructor = PlatformJwks.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

}
