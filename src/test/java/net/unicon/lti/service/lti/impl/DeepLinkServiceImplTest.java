package net.unicon.lti.service.lti.impl;

import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.ToolLink;
import net.unicon.lti.model.lti.dto.DeepLinkDTO;
import net.unicon.lti.model.lti.dto.DeepLinkJWTDTO;
import net.unicon.lti.repository.ToolLinkRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeepLinkServiceImplTest {
    private static final String LOCAL_URL = "https://tool.example.com";

    private static KeyPair keyPair;
    private static String privateKeyPem;

    @Mock
    ToolLinkRepository toolLinkRepository;

    @Mock
    LTIJWTServiceImpl ltijwtService;

    @Mock
    LTIDataService ltiDataService;

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    private DeepLinkServiceImpl deepLinkService;

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
        deepLinkService = new DeepLinkServiceImpl(toolLinkRepository, ltijwtService, ltiDataService, exceptionMessageGenerator);
    }

    private Jws<Claims> idToken() {
        Claims body = Jwts.claims()
                .issuer("https://platform.example.com")
                .audience().add("client-id").and()
                .add(LtiStrings.LTI_NONCE, "nonce-value")
                .add(LtiStrings.LTI_DEPLOYMENT_ID, "deployment-id")
                .add(LtiStrings.DEEP_LINKING_SETTINGS, Map.of(LtiStrings.DEEP_LINK_DATA, "dl-data"))
                .build();
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getBody()).thenReturn(body);
        return jws;
    }

    @Test
    public void testGetDeepLinks() {
        when(toolLinkRepository.findAll()).thenReturn(List.of(
                new ToolLink("1", "Title 1", "Desc 1", true, 10f),
                new ToolLink("2", "Title 2", "Desc 2", false, null)));

        List<DeepLinkDTO> result = deepLinkService.getDeepLinks();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getToolLinkId());
        assertEquals("Title 1", result.get(0).getTitle());
        assertEquals("Desc 1", result.get(0).getDescription());
        assertEquals("2", result.get(1).getToolLinkId());
        assertEquals("Title 2", result.get(1).getTitle());
        assertEquals("Desc 2", result.get(1).getDescription());
    }

    @Test
    public void testGetDeepLinksEmpty() {
        when(toolLinkRepository.findAll()).thenReturn(Collections.emptyList());
        assertTrue(deepLinkService.getDeepLinks().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGenerateDeepLinkJWT() throws Exception {
        when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKeyPem);
        when(ltiDataService.getLocalUrl()).thenReturn(LOCAL_URL);
        ToolLink assignment = new ToolLink("a1", "Assignment", "desc", true, 25f);
        ToolLink page = new ToolLink("p1", "Page", "desc", false, null);
        when(toolLinkRepository.findById("a1")).thenReturn(Optional.of(assignment));
        when(toolLinkRepository.findById("p1")).thenReturn(Optional.of(page));
        when(toolLinkRepository.findById("missing")).thenReturn(Optional.empty());

        Date before = new Date(System.currentTimeMillis() - 1000);
        DeepLinkJWTDTO dto = deepLinkService.generateDeepLinkJWT(List.of("a1", "missing", "p1"), idToken());

        List<Map<String, Object>> json = dto.getJSONString();
        assertEquals(2, json.size());
        Map<String, Object> first = json.get(0);
        assertEquals(LtiStrings.DEEP_LINK_LTIRESOURCELINK, first.get(LtiStrings.DEEP_LINK_TYPE));
        assertEquals("Assignment", first.get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals(LOCAL_URL + "/lti3?link=a1", first.get(LtiStrings.DEEP_LINK_URL));
        Map<String, Object> lineItem = (Map<String, Object>) first.get("lineItem");
        assertEquals(25f, lineItem.get("scoreMaximum"));
        assertEquals("Assignment", lineItem.get("label"));
        assertEquals("a1", lineItem.get("resourceId"));
        Map<String, Object> second = json.get(1);
        assertEquals(LOCAL_URL + "/lti3?link=p1", second.get(LtiStrings.DEEP_LINK_URL));
        assertFalse(second.containsKey("lineItem"));

        Jws<Claims> jws = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(dto.getJWTString());
        assertEquals(TextConstants.DEFAULT_KID, jws.getHeader().getKeyId());
        assertEquals(LtiStrings.JWT, jws.getHeader().getType());
        assertEquals(LtiStrings.RS256, jws.getHeader().getAlgorithm());
        Claims claims = jws.getPayload();
        assertEquals("client-id", claims.getIssuer());
        assertEquals("https://platform.example.com", Iterables.getOnlyElement(claims.getAudience()));
        assertEquals("https://platform.example.com", claims.get(LtiStrings.LTI_AZP));
        assertEquals("nonce-value", claims.get(LtiStrings.LTI_NONCE));
        assertEquals("deployment-id", claims.get(LtiStrings.LTI_DEPLOYMENT_ID));
        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE, claims.get(LtiStrings.LTI_MESSAGE_TYPE));
        assertEquals(LtiStrings.LTI_VERSION_3, claims.get(LtiStrings.LTI_VERSION));
        assertEquals("dl-data", claims.get(LtiStrings.LTI_DATA));
        assertTrue(!claims.getIssuedAt().before(before));
        assertEquals(3600_000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        List<Map<String, Object>> items = (List<Map<String, Object>>) claims.get(LtiStrings.LTI_CONTENT_ITEMS);
        assertEquals(2, items.size());
        assertEquals("Assignment", items.get(0).get(LtiStrings.DEEP_LINK_TITLE));
        assertEquals(25.0, ((Number) ((Map<String, Object>) items.get(0).get("lineItem")).get("scoreMaximum")).doubleValue());
        assertEquals("Page", items.get(1).get(LtiStrings.DEEP_LINK_TITLE));
    }

    @Test
    public void testGenerateDeepLinkJWTInvalidKey() {
        when(ltiDataService.getOwnPrivateKey()).thenReturn("garbage");
        assertThrows(GeneralSecurityException.class, () -> deepLinkService.generateDeepLinkJWT(List.of("a1"), idToken()));
    }

    @Test
    public void testCreateDeepLinkJsonAndLineItem() {
        ToolLink toolLink = new ToolLink("x", "X title", "d", true, 5f);
        Map<String, Object> deepLink = DeepLinkServiceImpl.createDeepLinkJson(LOCAL_URL, toolLink);
        assertEquals(LOCAL_URL + "/lti3?link=x", deepLink.get(LtiStrings.DEEP_LINK_URL));
        assertEquals(Map.of("scoreMaximum", 5f, "label", "X title", "resourceId", "x"), deepLink.get("lineItem"));
        assertEquals(deepLink.get("lineItem"), DeepLinkServiceImpl.lineItem(toolLink));
    }

    @Test
    public void testListMapToJsonSerializesAndHandlesFailure() throws Exception {
        Method method = DeepLinkServiceImpl.class.getDeclaredMethod("listMapToJson", List.class);
        method.setAccessible(true);

        String json = (String) method.invoke(null, List.of(Map.<String, Object>of("k", "v")));
        assertTrue(json.contains("\"k\" : \"v\""));
        assertTrue(json.startsWith("["));

        String failed = (String) method.invoke(null, List.of(Collections.<String, Object>singletonMap("bad", new FailingBean())));
        assertSame("", failed);
    }

    public static class FailingBean {
        public String getValue() {
            throw new IllegalStateException("cannot serialize");
        }
    }
}
