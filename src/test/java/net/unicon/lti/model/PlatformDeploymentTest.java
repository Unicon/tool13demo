package net.unicon.lti.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class PlatformDeploymentTest {

    private PlatformDeployment deployment(long keyId, String iss, String clientId, String deploymentId) {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        platformDeployment.setKeyId(keyId);
        platformDeployment.setIss(iss);
        platformDeployment.setClientId(clientId);
        platformDeployment.setDeploymentId(deploymentId);
        return platformDeployment;
    }

    @Test
    public void testAccessors() {
        PlatformDeployment platformDeployment = deployment(1L, "iss", "client", "dep");
        Set<LtiContextEntity> contexts = Set.of();
        platformDeployment.setOidcEndpoint("https://oidc");
        platformDeployment.setJwksEndpoint("https://jwks");
        platformDeployment.setoAuth2TokenUrl("https://token");
        platformDeployment.setoAuth2TokenAud("aud");
        platformDeployment.setContexts(contexts);

        assertEquals(1L, platformDeployment.getKeyId());
        assertEquals("iss", platformDeployment.getIss());
        assertEquals("client", platformDeployment.getClientId());
        assertEquals("dep", platformDeployment.getDeploymentId());
        assertEquals("https://oidc", platformDeployment.getOidcEndpoint());
        assertEquals("https://jwks", platformDeployment.getJwksEndpoint());
        assertEquals("https://token", platformDeployment.getoAuth2TokenUrl());
        assertEquals("aud", platformDeployment.getoAuth2TokenAud());
        assertSame(contexts, platformDeployment.getContexts());
    }

    @Test
    public void testContextsAreNotSerialized() {
        PlatformDeployment platformDeployment = deployment(1L, "iss", "client", "dep");
        platformDeployment.setContexts(Set.of(new LtiContextEntity()));

        String json = JsonMapper.builder().build().writeValueAsString(platformDeployment);

        assertFalse(json.contains("contexts"), json);
    }

    @Test
    public void testEquals() {
        PlatformDeployment platformDeployment = deployment(1L, "iss", "client", "dep");
        PlatformDeployment same = deployment(1L, "iss", "client", "dep");
        same.setOidcEndpoint("ignored by equals");

        assertEquals(platformDeployment, platformDeployment);
        assertEquals(platformDeployment, same);
        assertNotEquals(platformDeployment, deployment(2L, "iss", "client", "dep"));
        assertNotEquals(platformDeployment, deployment(1L, "other", "client", "dep"));
        assertNotEquals(platformDeployment, deployment(1L, "iss", "other", "dep"));
        assertNotEquals(platformDeployment, deployment(1L, "iss", "client", "other"));
        assertNotEquals(platformDeployment, null);
        assertNotEquals(platformDeployment, "iss");
    }

    @Test
    public void testHashCode() {
        PlatformDeployment platformDeployment = deployment(1L, "iss", "client", "dep");
        platformDeployment.setOidcEndpoint("oidc");
        platformDeployment.setoAuth2TokenUrl("token");

        assertEquals(java.util.Objects.hash(1L, "iss", "client", "dep"), platformDeployment.hashCode());

        // equal deployments have equal hash codes, even when fields equals() ignores differ
        PlatformDeployment same = deployment(1L, "iss", "client", "dep");
        same.setOidcEndpoint("other-oidc");
        same.setoAuth2TokenUrl("other-token");
        assertEquals(platformDeployment, same);
        assertEquals(platformDeployment.hashCode(), same.hashCode());
        assertEquals(java.util.Objects.hash(3L, null, null, null), deployment(3L, null, null, null).hashCode());
    }
}
