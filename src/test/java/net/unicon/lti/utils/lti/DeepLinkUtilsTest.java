package net.unicon.lti.utils.lti;

import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class DeepLinkUtilsTest {
    private static final String LOCAL_URL = "https://tool.example.com";
    private static final String CLIENT_ID = "client-id";
    private static final String ISS = "https://platform.example.com";

    private static KeyPair keyPair;
    private static String privateKeyPem;

    @Mock
    LTIDataService ltiDataService;

    @Mock
    PlatformDeployment platformDeployment;

    @Mock
    LTI3Request lti3Request;

    @BeforeAll
    public static void generateKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKeyPem);
        when(platformDeployment.getClientId()).thenReturn(CLIENT_ID);
        when(lti3Request.getIss()).thenReturn(ISS);
        when(lti3Request.getNonce()).thenReturn("nonce-value");
        when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id");
        lti3Request.deepLinkData = "deep-link-data";
    }

    private Claims parse(String jwt) {
        Jws<Claims> jws = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(jwt);
        assertEquals(TextConstants.DEFAULT_KID, jws.getHeader().getKeyId());
        assertEquals(LtiStrings.JWT, jws.getHeader().getType());
        assertEquals(LtiStrings.RS256, jws.getHeader().getAlgorithm());
        Claims claims = jws.getPayload();
        assertEquals(CLIENT_ID, claims.getIssuer());
        assertEquals(ISS, Iterables.getOnlyElement(claims.getAudience()));
        assertEquals("nonce-value", claims.get(LtiStrings.LTI_NONCE));
        assertEquals(ISS, claims.get(LtiStrings.LTI_AZP));
        assertEquals("deployment-id", claims.get(LtiStrings.LTI_DEPLOYMENT_ID));
        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE, claims.get(LtiStrings.LTI_MESSAGE_TYPE));
        assertEquals(LtiStrings.LTI_VERSION_3, claims.get(LtiStrings.LTI_VERSION));
        assertEquals("deep-link-data", claims.get(LtiStrings.LTI_DATA));
        assertEquals(3600_000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        return claims;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> contentItems(Claims claims) {
        return (List<Map<String, Object>>) claims.get(LtiStrings.LTI_CONTENT_ITEMS);
    }

    private List<Map<String, Object>> parseJson(String json) {
        return new ObjectMapper().readValue(json, new TypeReference<List<Map<String, Object>>>() {});
    }

    @Test
    public void testGenerateDeepLinkJWTProducesAllExamples() throws Exception {
        Map<String, List<String>> result = DeepLinkUtils.generateDeepLinkJWT(ltiDataService, platformDeployment, lti3Request, LOCAL_URL);

        assertEquals(List.of(
                "Example 1: EMPTY json",
                "Example 2: ONE Standard LTI Core Link (ltiResourceLink)",
                "Example 3: ONE External (YouTube) Link (NON ltiResourceLink)",
                "Example 4: ONE File Link (NON ltiResourceLink)",
                "Example 4b: A different file Link (NON ltiResourceLink)",
                "Example 5: TWO Standard LTI Core Links (ltiResourceLinks) and TWO external links",
                "Example 6: TWO Standard LTI Core Links (ltiResourceLinks)"), List.copyOf(result.keySet()));
        result.values().forEach(v -> assertEquals(2, v.size()));

        // Example 1: empty content items
        List<String> ex1 = result.get("Example 1: EMPTY json");
        assertEquals("{}", ex1.get(1));
        Object emptyItems = parse(ex1.get(0)).get(LtiStrings.LTI_CONTENT_ITEMS);
        assertTrue(emptyItems == null || ((Map<?, ?>) emptyItems).isEmpty());

        // Example 2: one resource link with grades
        List<String> ex2 = result.get("Example 2: ONE Standard LTI Core Link (ltiResourceLink)");
        List<Map<String, Object>> items2 = contentItems(parse(ex2.get(0)));
        assertEquals(1, items2.size());
        Map<String, Object> link = items2.get(0);
        assertEquals(LtiStrings.DEEP_LINK_LTIRESOURCELINK, link.get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals("My test link", link.get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals(LOCAL_URL + "/lti3?link=1234", link.get(LtiStrings.DEEP_LINK_URL));
        Map<?, ?> lineItem = (Map<?, ?>) link.get("lineItem");
        assertEquals(87, ((Number) lineItem.get("scoreMaximum")).intValue());
        assertEquals("LTI 1234 Quiz", lineItem.get("label"));
        assertEquals("1234", lineItem.get("resourceId"));
        assertEquals("myquiztest", lineItem.get("tag"));
        assertEquals("2018-03-07T20:00:03Z", ((Map<?, ?>) link.get("available")).get("startDateTime"));
        assertEquals("2022-03-07T20:00:03Z", ((Map<?, ?>) link.get("available")).get("endDateTime"));
        assertEquals("2019-03-07T20:00:03Z", ((Map<?, ?>) link.get("submission")).get("startDateTime"));
        assertEquals("2021-08-07T20:00:03Z", ((Map<?, ?>) link.get("submission")).get("endDateTime"));
        assertEquals("$Resource.submission.endDateTime", ((Map<?, ?>) link.get("custom")).get("dueDate"));
        assertEquals(items2.size(), parseJson(ex2.get(1)).size());
        assertEquals(link.get(LtiStrings.DEEP_LINK_URL), parseJson(ex2.get(1)).get(0).get(LtiStrings.DEEP_LINK_URL));

        // Example 3: external youtube link
        List<String> ex3 = result.get("Example 3: ONE External (YouTube) Link (NON ltiResourceLink)");
        List<Map<String, Object>> items3 = contentItems(parse(ex3.get(0)));
        assertEquals(1, items3.size());
        assertEquals("link", items3.get(0).get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals("https://www.youtube.com/watch?v=corV3-WsIro", items3.get(0).get(LtiStrings.DEEP_LINK_URL));
        assertEquals(items3, parseJson(ex3.get(1)));

        // Example 4 and 4b: file links
        List<String> ex4 = result.get("Example 4: ONE File Link (NON ltiResourceLink)");
        List<Map<String, Object>> items4 = contentItems(parse(ex4.get(0)));
        assertEquals("file", items4.get(0).get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals(LOCAL_URL + "/files/TestSubmission.pdf", items4.get(0).get(LtiStrings.DEEP_LINK_URL));
        assertEquals(items4, parseJson(ex4.get(1)));

        List<String> ex4b = result.get("Example 4b: A different file Link (NON ltiResourceLink)");
        List<Map<String, Object>> items4b = contentItems(parse(ex4b.get(0)));
        assertEquals("file", items4b.get(0).get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals(LOCAL_URL + "/files/TestSubmission2.pdf", items4b.get(0).get(LtiStrings.DEEP_LINK_URL));
        assertEquals(items4b, parseJson(ex4b.get(1)));

        // Example 5: mixed links
        List<String> ex5 = result.get("Example 5: TWO Standard LTI Core Links (ltiResourceLinks) and TWO external links");
        List<Map<String, Object>> items5 = contentItems(parse(ex5.get(0)));
        assertEquals(4, items5.size());
        assertEquals(List.of(LtiStrings.DEEP_LINK_LTIRESOURCELINK, "link", LtiStrings.DEEP_LINK_LTIRESOURCELINK, "file"),
                items5.stream().map(m -> m.get(LtiStrings.DEEP_LINK_TYPE)).toList());
        Map<?, ?> iframe = (Map<?, ?>) items5.get(1).get("iframe");
        assertEquals(560, ((Number) iframe.get("width")).intValue());
        assertEquals(315, ((Number) iframe.get("height")).intValue());
        assertEquals("youtube-corV3-WsIro", ((Map<?, ?>) items5.get(1).get("window")).get("targetName"));
        assertTrue(((String) ((Map<?, ?>) items5.get(1).get("embed")).get("html")).startsWith("<iframe"));
        assertEquals(LOCAL_URL + "/lti3?link=4567", items5.get(2).get(LtiStrings.DEEP_LINK_URL));
        assertEquals("application/pdf", items5.get(3).get("mediaType"));
        assertEquals(4, parseJson(ex5.get(1)).size());

        // Example 6: two resource links
        List<String> ex6 = result.get("Example 6: TWO Standard LTI Core Links (ltiResourceLinks)");
        List<Map<String, Object>> items6 = contentItems(parse(ex6.get(0)));
        assertEquals(2, items6.size());
        assertEquals("My test link", items6.get(0).get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals("Another deep link", items6.get(1).get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals(items6, parseJson(ex6.get(1)));
    }

    @Test
    public void testGenerateDeepLinkJWTWithInvalidKey() {
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not a pem key");
        assertThrows(GeneralSecurityException.class,
                () -> DeepLinkUtils.generateDeepLinkJWT(ltiDataService, platformDeployment, lti3Request, LOCAL_URL));
    }

    @Test
    public void testCreateOneDeepLink() {
        List<Map<String, Object>> links = DeepLinkUtils.createOneDeepLink(LOCAL_URL);
        assertEquals(1, links.size());
        assertEquals(LtiStrings.DEEP_LINK_LTIRESOURCELINK, links.get(0).get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals("My test link", links.get(0).get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals(LOCAL_URL + "/lti3?link=1234", links.get(0).get(LtiStrings.DEEP_LINK_URL));
        assertFalse(links.get(0).containsKey("lineItem"));
    }

    @Test
    public void testListMapToJsonSerializesAndHandlesFailure() throws Exception {
        Method method = DeepLinkUtils.class.getDeclaredMethod("listMapToJson", List.class);
        method.setAccessible(true);

        String json = (String) method.invoke(null, List.of(Map.<String, Object>of("a", "b")));
        assertEquals(List.of(Map.of("a", "b")), parseJson(json));

        String failed = (String) method.invoke(null, List.of(Collections.<String, Object>singletonMap("bad", new FailingBean())));
        assertEquals("", failed);
    }

    @Test
    public void testConstructorThrows() throws Exception {
        Constructor<DeepLinkUtils> constructor = DeepLinkUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException e = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertEquals("Utility class", e.getCause().getMessage());
    }

    public static class FailingBean {
        public String getValue() {
            throw new IllegalStateException("cannot serialize");
        }
    }
}
