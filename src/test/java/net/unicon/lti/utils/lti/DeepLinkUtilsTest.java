package net.unicon.lti.utils.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import net.unicon.lti.model.lti.dto.DeepLinkingContentItemDTO;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static net.unicon.lti.utils.LtiStrings.LTI_AZP;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTENT_ITEMS;
import static net.unicon.lti.utils.LtiStrings.LTI_DATA;
import static net.unicon.lti.utils.LtiStrings.LTI_DEPLOYMENT_ID;
import static net.unicon.lti.utils.LtiStrings.LTI_MESSAGE_TYPE;
import static net.unicon.lti.utils.LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE;
import static net.unicon.lti.utils.LtiStrings.LTI_NONCE;
import static net.unicon.lti.utils.LtiStrings.LTI_VERSION;
import static net.unicon.lti.utils.LtiStrings.LTI_VERSION_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

public class DeepLinkUtilsTest {
    private final String SAMPLE_CLIENT_ID = "sample-client-id";
    private final String SAMPLE_ISS = "https://lms.com";
    private final String SAMPLE_NONCE = "sample-nonce";
    private final String SAMPLE_DEPLOYMENT_ID = "sample-deployment-id";
    private final String SAMPLE_DEEP_LINK_DATA = "sample-deep-link-data";

    List<DeepLinkingContentItemDTO> contentItems = new ArrayList<>();

    KeyPair kp;

    String privateKey;

    @Mock
    LTI3Request lti3Request;

    @Mock
    LTIDataService ltiDataService;

    @Configuration
    static class ContextConfiguration {

    }

    @BeforeEach()
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        kp = kpg.generateKeyPair();
        privateKey = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";

        HarmonyContentItemDTO contentItem1 = new HarmonyContentItemDTO();
        contentItem1.setTitle("Reading 1");
        contentItem1.setUrl("https://tool.com/reading1");
        HarmonyContentItemDTO contentItem2 = new HarmonyContentItemDTO();
        contentItem2.setTitle("Quiz 1");
        contentItem2.setUrl("https://tool.com/quiz1");
        contentItem2.setScoreMaximum(100f);
        contentItem2.setLabel("Quiz 1 Label");
        contentItem2.setResourceId("q1");
        contentItem2.setTag("quiz");
        List<HarmonyContentItemDTO> harmonyContentItems = new ArrayList<>();
        harmonyContentItems.add(contentItem1);
        harmonyContentItems.add(contentItem2);
        harmonyContentItems.forEach((harmonyContentItem) -> contentItems.add(harmonyContentItem.toDeepLinkingContentItem()));

    }

    @Test
    public void testGenerateDeepLinkingResponseJWT() {
        try {
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISS);
            when(lti3Request.getNonce()).thenReturn(SAMPLE_NONCE);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(lti3Request.getDeepLinkData()).thenReturn(SAMPLE_DEEP_LINK_DATA);

            String responseJwt = DeepLinkUtils.generateDeepLinkingResponseJWT(ltiDataService, lti3Request, contentItems);
            assertNotNull(responseJwt);
            validateDeepLinkingResponse(responseJwt);

        } catch (GeneralSecurityException e) {
            fail("Exception should not be thrown");
        }
    }

    private void validateDeepLinkingResponse(String response) {
        Jws<Claims> claims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(response);
        assertNotNull(claims);
        assertEquals(TextConstants.DEFAULT_KID, claims.getHeader().get("kid"));
        assertEquals("JWT", claims.getHeader().get("typ"));
        assertEquals(SAMPLE_CLIENT_ID, claims.getBody().getIssuer());
        assertEquals(SAMPLE_ISS, claims.getBody().getAudience());
        assertNotNull(claims.getBody().getExpiration());
        assertNotNull(claims.getBody().getIssuedAt());
        assertEquals(SAMPLE_NONCE, claims.getBody().get(LTI_NONCE));
        assertEquals(SAMPLE_ISS, claims.getBody().get(LTI_AZP));
        assertEquals(SAMPLE_DEPLOYMENT_ID, claims.getBody().get(LTI_DEPLOYMENT_ID));
        assertEquals(LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE, claims.getBody().get(LTI_MESSAGE_TYPE));
        assertEquals(LTI_VERSION_3, claims.getBody().get(LTI_VERSION));
        assertEquals(SAMPLE_DEEP_LINK_DATA, claims.getBody().get(LTI_DATA));

        List<Map<String, Object>> responseContentItems = claims.getBody().get(LTI_CONTENT_ITEMS, List.class);

        Map<String, Object> respContentItem1 = responseContentItems.get(0);
        assertEquals(contentItems.get(0).getTitle(), respContentItem1.get("title"));
        assertEquals(contentItems.get(0).getUrl(), respContentItem1.get("url"));
        assertNull(respContentItem1.get("lineItem"));
        assertNull(respContentItem1.get("scoreMaximum"));
        assertNull(respContentItem1.get("label"));
        assertNull(respContentItem1.get("resourceId"));
        assertNull(respContentItem1.get("tag"));

        Map<String, Object> respContentItem2 = responseContentItems.get(1);
        assertEquals(contentItems.get(1).getTitle(), respContentItem2.get("title"));
        assertEquals(contentItems.get(1).getUrl(), respContentItem2.get("url"));
        assertNull(respContentItem1.get("scoreMaximum"));
        assertNull(respContentItem1.get("label"));
        assertNull(respContentItem1.get("resourceId"));
        assertNull(respContentItem1.get("tag"));
        Map<String, Object> lineItem = (Map) respContentItem2.get("lineItem");
        assertEquals(contentItems.get(1).getLineItem().getScoreMaximum(), ((Double) lineItem.get("scoreMaximum")).floatValue());
        assertEquals(contentItems.get(1).getLineItem().getLabel(), lineItem.get("label"));
        assertEquals(contentItems.get(1).getLineItem().getResourceId(), lineItem.get("resourceId"));
        assertEquals(contentItems.get(1).getLineItem().getTag(), lineItem.get("tag"));
    }

}
