package net.unicon.lti.model.lti.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistrationDtoTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final JsonMapper parsingMapper = jsonMapper;

    // a bare Object has no properties, so Jackson refuses to serialize it
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<String> unserializableList() {
        List list = new ArrayList();
        list.add(new Object());
        return list;
    }

    @Test
    public void testPlatformRegistrationParsesDiscoveryDocument() {
        String json = "{\"issuer\":\"https://canvas\",\"authorization_endpoint\":\"https://auth\","
                + "\"token_endpoint\":\"https://token\",\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\"],"
                + "\"jwks_uri\":\"https://jwks\",\"registration_endpoint\":\"https://reg\",\"scopes_supported\":[\"s\"],"
                + "\"response_types_supported\":[\"id_token\"],\"subject_types_supported\":[\"public\"],"
                + "\"id_token_signing_alg_values_supported\":[\"RS256\"],\"claims_supported\":[\"sub\"],"
                + "\"authorization_server\":\"https://as\",\"unknown\":\"ignored\","
                + "\"https://purl.imsglobal.org/spec/lti-platform-configuration\":{\"product_family_code\":\"canvas\","
                + "\"version\":\"1.0\",\"variables\":[\"v\"],\"messages_supported\":["
                + "{\"type\":\"LtiResourceLinkRequest\",\"placements\":[\"course_navigation\"]},\"LtiDeepLinkingRequest\"]}}";

        PlatformRegistrationDTO dto = parsingMapper.readValue(json, PlatformRegistrationDTO.class);

        assertEquals("https://canvas", dto.getIssuer());
        assertEquals("https://auth", dto.getAuthorization_endpoint());
        assertEquals("https://token", dto.getToken_endpoint());
        assertEquals(List.of("private_key_jwt"), dto.getToken_endpoint_auth_methods_supported());
        assertEquals("https://jwks", dto.getJwks_uri());
        assertEquals("https://reg", dto.getRegistration_endpoint());
        assertEquals(List.of("s"), dto.getScopes_supported());
        assertEquals(List.of("id_token"), dto.getResponse_types_supported());
        assertEquals(List.of("public"), dto.getSubject_types_supported());
        assertEquals(List.of("RS256"), dto.getId_token_signing_alg_values_supported());
        assertEquals(List.of("sub"), dto.getClaims_supported());
        assertEquals("https://as", dto.getAuthorization_server());

        PlatformConfigurationDTO config = dto.getPlatformConfiguration();
        assertEquals("canvas", config.getProduct_family_code());
        assertEquals("1.0", config.getVersion());
        assertEquals(List.of("v"), config.getVariables());
        assertEquals(2, config.getMessages_supported().size());
        assertEquals("LtiResourceLinkRequest", config.getMessages_supported().get(0).getType());
        assertEquals(List.of("course_navigation"), config.getMessages_supported().get(0).getPlacements());
        assertEquals("LtiDeepLinkingRequest", config.getMessages_supported().get(1).getType());
        assertNull(config.getMessages_supported().get(1).getPlacements());
    }

    @Test
    public void testDefaultMapperReadsPlatformRegistration() {
        PlatformRegistrationDTO dto = jsonMapper.readValue("{\"issuer\":\"https://canvas\"}", PlatformRegistrationDTO.class);

        assertEquals("https://canvas", dto.getIssuer());
    }

    @Test
    public void testPlatformRegistrationSetters() {
        PlatformRegistrationDTO dto = new PlatformRegistrationDTO();
        List<String> list = List.of("x");
        PlatformConfigurationDTO config = new PlatformConfigurationDTO();
        dto.setIssuer("i");
        dto.setAuthorization_endpoint("a");
        dto.setToken_endpoint("t");
        dto.setToken_endpoint_auth_methods_supported(list);
        dto.setJwks_uri("j");
        dto.setRegistration_endpoint("r");
        dto.setScopes_supported(list);
        dto.setResponse_types_supported(list);
        dto.setSubject_types_supported(list);
        dto.setId_token_signing_alg_values_supported(list);
        dto.setClaims_supported(list);
        dto.setAuthorization_server("as");
        dto.setPlatformConfiguration(config);

        assertEquals("i", dto.getIssuer());
        assertEquals("a", dto.getAuthorization_endpoint());
        assertEquals("t", dto.getToken_endpoint());
        assertSame(list, dto.getToken_endpoint_auth_methods_supported());
        assertEquals("j", dto.getJwks_uri());
        assertEquals("r", dto.getRegistration_endpoint());
        assertSame(list, dto.getScopes_supported());
        assertSame(list, dto.getResponse_types_supported());
        assertSame(list, dto.getSubject_types_supported());
        assertSame(list, dto.getId_token_signing_alg_values_supported());
        assertSame(list, dto.getClaims_supported());
        assertEquals("as", dto.getAuthorization_server());
        assertSame(config, dto.getPlatformConfiguration());
    }

    @Test
    public void testPlatformConfigurationToString() {
        PlatformConfigurationDTO config = new PlatformConfigurationDTO();
        MessagesSupportedDTO message = new MessagesSupportedDTO();
        message.setType("LtiResourceLinkRequest");
        message.setPlacements(List.of("link_selection"));
        config.setProduct_family_code("canvas");
        config.setVersion("1.0");
        config.setVariables(List.of("v"));
        config.setMessages_supported(List.of(message));

        JsonNode json = jsonMapper.readTree(config.toString());

        assertEquals("canvas", json.get("product_family_code").asString());
        assertEquals("1.0", json.get("version").asString());
        assertEquals("v", json.get("variables").get(0).asString());
        assertEquals("LtiResourceLinkRequest", json.get("messages_supported").get(0).get("type").asString());
        assertEquals("link_selection", json.get("messages_supported").get(0).get("placements").get(0).asString());
    }

    @Test
    public void testPlatformConfigurationToStringFallback() {
        PlatformConfigurationDTO config = new PlatformConfigurationDTO();
        config.setVariables(unserializableList());

        assertEquals("PlatformConfigurationDTO could not be serialized", config.toString());
    }

    private ToolConfigurationDTO toolConfiguration() {
        ToolConfigurationDTO config = new ToolConfigurationDTO();
        ToolMessagesSupportedDTO message = new ToolMessagesSupportedDTO();
        message.setType("LtiDeepLinkingRequest");
        message.setTarget_link_uri("https://tool/dl");
        message.setLabel("Deep link");
        message.setIcon_uri("https://tool/icon.png");
        message.setCustom_parameters(Map.of("k", "v"));
        message.setPlacements(List.of("assignment_selection"));
        config.setDomain("tool.example.com");
        config.setSecondary_domains(List.of("alt.example.com"));
        config.setDeployment_id("dep");
        config.setTarget_link_uri("https://tool/launch");
        config.setCustom_parameters(Map.of("a", "b"));
        config.setDescription("desc");
        config.setMessages_supported(List.of(message));
        config.setClaims(List.of("sub"));
        return config;
    }

    @Test
    public void testToolMessagesSupportedAccessors() {
        ToolMessagesSupportedDTO message = toolConfiguration().getMessages_supported().get(0);
        assertEquals("LtiDeepLinkingRequest", message.getType());
        assertEquals("https://tool/dl", message.getTarget_link_uri());
        assertEquals("Deep link", message.getLabel());
        assertEquals("https://tool/icon.png", message.getIcon_uri());
        assertEquals(Map.of("k", "v"), message.getCustom_parameters());
        assertEquals(List.of("assignment_selection"), message.getPlacements());
    }

    @Test
    public void testToolConfigurationAccessors() {
        ToolConfigurationDTO config = toolConfiguration();
        config.setCanvasPrivacyLevel("public");

        assertEquals("tool.example.com", config.getDomain());
        assertEquals(List.of("alt.example.com"), config.getSecondary_domains());
        assertEquals("dep", config.getDeployment_id());
        assertEquals("https://tool/launch", config.getTarget_link_uri());
        assertEquals(Map.of("a", "b"), config.getCustom_parameters());
        assertEquals("desc", config.getDescription());
        assertEquals(1, config.getMessages_supported().size());
        assertEquals(List.of("sub"), config.getClaims());
        assertEquals("public", config.getCanvasPrivacyLevel());
    }

    @Test
    public void testToolConfigurationToString() {
        ToolConfigurationDTO config = toolConfiguration();

        JsonNode json = jsonMapper.readTree(config.toString());

        assertEquals("tool.example.com", json.get("domain").asString());
        assertEquals("alt.example.com", json.get("secondary_domains").get(0).asString());
        assertEquals("dep", json.get("deployment_id").asString());
        assertEquals("https://tool/launch", json.get("target_link_uri").asString());
        assertEquals("b", json.get("custom_parameters").get("a").asString());
        assertEquals("desc", json.get("description").asString());
        assertEquals("sub", json.get("claims").get(0).asString());
        JsonNode message = json.get("messages").get(0);
        assertEquals("LtiDeepLinkingRequest", message.get("type").asString());
        assertEquals("https://tool/dl", message.get("target_link_uri").asString());
        assertEquals("Deep link", message.get("label").asString());
        assertEquals("https://tool/icon.png", message.get("icon_uri").asString());
        assertEquals("v", message.get("custom_parameters").get("k").asString());
        assertEquals("assignment_selection", message.get("placements").get(0).asString());
        assertFalse(json.has("https://canvas.instructure.com/lti/privacy_level"));

        config.setCanvasPrivacyLevel("anonymous");
        json = jsonMapper.readTree(config.toString());
        assertEquals("anonymous", json.get("https://canvas.instructure.com/lti/privacy_level").asString());
    }

    @Test
    public void testToolConfigurationToStringFallback() {
        ToolConfigurationDTO config = new ToolConfigurationDTO();
        config.setDomain("tool.example.com");
        config.setClaims(unserializableList());

        String text = config.toString();

        assertTrue(text.startsWith("ToolConfigurationDTO{domain='tool.example.com'"), text);
        assertTrue(text.endsWith(", canvasPrivacyLevel='null'}"), text);
    }

    private void fillToolRegistration(ToolRegistrationDTO dto) {
        dto.setApplication_type("web");
        dto.setGrant_types(List.of("client_credentials"));
        dto.setResponse_types(List.of("id_token"));
        dto.setRedirect_uris(List.of("https://tool/launch"));
        dto.setInitiate_login_uri("https://tool/login");
        dto.setClient_name("Tool");
        dto.setJwks_uri("https://tool/jwks");
        dto.setLogo_uri("https://tool/logo");
        dto.setToken_endpoint_auth_method("private_key_jwt");
        dto.setContacts(List.of("admin@example.com"));
        dto.setClient_uri("https://tool");
        dto.setTos_uri("https://tool/tos");
        dto.setPolicy_uri("https://tool/policy");
        dto.setToolConfiguration(toolConfiguration());
        dto.setScope("scope-a scope-b");
    }

    @Test
    public void testToolRegistrationAccessors() {
        ToolRegistrationDTO dto = new ToolRegistrationDTO();
        fillToolRegistration(dto);

        assertEquals("web", dto.getApplication_type());
        assertEquals(List.of("client_credentials"), dto.getGrant_types());
        assertEquals(List.of("id_token"), dto.getResponse_types());
        assertEquals(List.of("https://tool/launch"), dto.getRedirect_uris());
        assertEquals("https://tool/login", dto.getInitiate_login_uri());
        assertEquals("Tool", dto.getClient_name());
        assertEquals("https://tool/jwks", dto.getJwks_uri());
        assertEquals("https://tool/logo", dto.getLogo_uri());
        assertEquals("private_key_jwt", dto.getToken_endpoint_auth_method());
        assertEquals(List.of("admin@example.com"), dto.getContacts());
        assertEquals("https://tool", dto.getClient_uri());
        assertEquals("https://tool/tos", dto.getTos_uri());
        assertEquals("https://tool/policy", dto.getPolicy_uri());
        assertEquals("tool.example.com", dto.getToolConfiguration().getDomain());
        assertEquals("scope-a scope-b", dto.getScope());
    }

    @Test
    public void testToolRegistrationToString() {
        ToolRegistrationDTO dto = new ToolRegistrationDTO();
        fillToolRegistration(dto);

        JsonNode json = jsonMapper.readTree(dto.toString());

        assertEquals("web", json.get("application_type").asString());
        assertEquals("client_credentials", json.get("grant_types").get(0).asString());
        assertEquals("https://tool/login", json.get("initiate_login_uri").asString());
        assertEquals("Tool", json.get("client_name").asString());
        assertEquals("admin@example.com", json.get("contacts").get(0).asString());
        assertEquals("scope-a scope-b", json.get("scope").asString());
        assertEquals("tool.example.com",
                json.get("https://purl.imsglobal.org/spec/lti-tool-configuration").get("domain").asString());
    }

    @Test
    public void testToolRegistrationToStringFallback() {
        ToolRegistrationDTO dto = new ToolRegistrationDTO();
        dto.setApplication_type("web");
        dto.setContacts(unserializableList());

        String text = dto.toString();

        assertTrue(text.startsWith("ToolRegistrationDTO{application_type='web'"), text);
        assertTrue(text.endsWith(", toolConfiguration=null, scope='null'}"), text);
    }

    @Test
    public void testToolConfigurationAckToString() {
        ToolConfigurationACKDTO dto = new ToolConfigurationACKDTO();
        fillToolRegistration(dto);
        dto.setClient_id("client-1");

        assertEquals("client-1", dto.getClient_id());
        JsonNode json = jsonMapper.readTree(dto.toString());
        assertEquals("client-1", json.get("client_id").asString());
        assertEquals("web", json.get("application_type").asString());
    }

    @Test
    public void testToolConfigurationAckToStringFallback() {
        ToolConfigurationACKDTO dto = new ToolConfigurationACKDTO();
        dto.setClient_id("client-1");
        dto.setContacts(unserializableList());

        String text = dto.toString();

        assertTrue(text.startsWith("ToolConfigurationACKDTO{client_id='client-1', registration_client_uri='null'} "), text);
        assertTrue(text.contains("ToolRegistrationDTO{application_type='null'"), text);
    }
}
