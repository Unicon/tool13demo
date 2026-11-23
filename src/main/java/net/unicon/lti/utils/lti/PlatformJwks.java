package net.unicon.lti.utils.lti;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import net.unicon.lti.utils.HttpClients;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platforms' public keys, fetched from their JWKS endpoints and cached, so validating a launch or a
 * service token doesn't download the key set every time. Nimbus's key source refreshes the cache on its
 * own schedule, and early when a token names a key it doesn't have yet, which is how a platform
 * rotating its keys gets picked up.
 */
public final class PlatformJwks {

    // the largest key set accepted, well above any real platform's
    private static final int SIZE_LIMIT = 512 * 1024;

    private static final Map<String, JWKSource<SecurityContext>> SOURCES = new ConcurrentHashMap<>();

    private PlatformJwks() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @return the platform's public key with this key ID, or null if it has none
     */
    public static Key publicKey(String jwksEndpoint, String keyId) throws JOSEException, MalformedURLException {
        if (keyId == null) {
            return null;
        }

        JWKSource<SecurityContext> source = SOURCES.get(jwksEndpoint);

        if (source == null) {
            source = SOURCES.computeIfAbsent(jwksEndpoint, PlatformJwks::createSource);
        }

        List<JWK> keys = source.get(new JWKSelector(new JWKMatcher.Builder().keyID(keyId).build()), null);

        return keys.isEmpty() ? null : ((AsymmetricJWK) keys.get(0)).toPublicKey();
    }

    static void clear() {
        SOURCES.clear();
    }

    private static JWKSource<SecurityContext> createSource(String jwksEndpoint) {
        try {
            DefaultResourceRetriever retriever = new DefaultResourceRetriever(
                (int) HttpClients.CONNECT_TIMEOUT.toMillis(),
                (int) HttpClients.READ_TIMEOUT.toMillis(),
                SIZE_LIMIT
            );

            return JWKSourceBuilder.create(URI.create(jwksEndpoint).toURL(), retriever).build();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Not a valid JWKS endpoint: " + jwksEndpoint, e);
        }
    }

}
