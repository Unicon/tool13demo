package net.unicon.lti.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seed files and the /config API read and write the OAuth2 token settings as oAuth2TokenUrl and
 * oAuth2TokenAud, which Jackson 3 doesn't infer from getoAuth2TokenUrl/setoAuth2TokenUrl.
 */
public class PlatformDeploymentJsonTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    public void testReadsTheOAuth2TokenSettings() {
        PlatformDeployment platformDeployment = jsonMapper.readValue(
            "{\"iss\":\"https://canvas.instructure.com\",\"clientId\":\"client-1\",\"oAuth2TokenUrl\":\"https://sso.example.com/token\",\"oAuth2TokenAud\":\"token-aud\"}",
            PlatformDeployment.class
        );

        assertEquals("https://sso.example.com/token", platformDeployment.getoAuth2TokenUrl());
        assertEquals("token-aud", platformDeployment.getoAuth2TokenAud());
    }

    @Test
    public void testWritesTheOAuth2TokenSettings() {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        platformDeployment.setoAuth2TokenUrl("https://sso.example.com/token");
        platformDeployment.setoAuth2TokenAud("token-aud");

        String json = jsonMapper.writeValueAsString(platformDeployment);

        assertTrue(json.contains("\"oAuth2TokenUrl\":\"https://sso.example.com/token\""), json);
        assertTrue(json.contains("\"oAuth2TokenAud\":\"token-aud\""), json);
    }

}
