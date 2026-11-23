package net.unicon.lti.controller.lti;

import com.sun.net.httpserver.HttpServer;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.RegistrationException;
import net.unicon.lti.model.lti.dto.MessagesSupportedDTO;
import net.unicon.lti.model.lti.dto.PlatformConfigurationDTO;
import net.unicon.lti.model.lti.dto.ToolMessagesSupportedDTO;
import net.unicon.lti.utils.TextConstants;
import org.mockito.ArgumentCaptor;
import java.net.InetSocketAddress;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationACKDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.utils.LtiStrings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegistrationControllerTest {

    private static final String TEST_OPENID_CONFIGURATION_URL = "https://platform.com/get-configuration";
    private static final String TEST_REGISTRATION_TOKEN = "test-registration-token";
    private static final String TEST_REGISTRATION_ENDPOINT = "https://platform.com/register-tool";
    private static final String TEST_PLATFORM_ISSUER = "https://platform.com";
    private static final List TEST_SCOPES = Arrays.asList("example-scope1", "example-scope2");

    @InjectMocks
    private RegistrationController registrationController;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // created lazily rather than injected, so Mockito's constructor injection doesn't set it
        ReflectionTestUtils.setField(registrationController, "restTemplate", restTemplate);
        when(req.getSession()).thenReturn(session);
    }

    @Test
    public void testRegistration() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setClaims_supported(LtiStrings.LTI_OPTIONAL_CLAIMS);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setIssuer(TEST_PLATFORM_ISSUER);
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);
        when(platformDeploymentRepository.save(any())).thenReturn(new PlatformDeployment());

        ToolConfigurationACKDTO toolConfigurationACKDTO = new ToolConfigurationACKDTO();
        toolConfigurationACKDTO.setClient_id("test-success");
        ToolConfigurationDTO toolConfigurationDTO = new ToolConfigurationDTO();
        toolConfigurationACKDTO.setToolConfiguration(toolConfigurationDTO);
        ResponseEntity<ToolConfigurationACKDTO> response = new ResponseEntity<>(toolConfigurationACKDTO, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(), eq(ToolConfigurationACKDTO.class))).thenReturn(response);

        try {
            when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT))).thenReturn(toolConfigurationACKDTO);

            String registrationOutput = registrationController.registration(TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationConfirmation", registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationDemo() {
        ReflectionTestUtils.setField(registrationController, "demoMode", true);
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setClaims_supported(LtiStrings.LTI_OPTIONAL_CLAIMS);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);
        try {
            String registrationOutput = registrationController.registration(TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationRedirect", registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationNoPlatformConfiguration() {
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>((PlatformRegistrationDTO) null, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try {
            String registrationOutput = registrationController.registration(TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session, never()).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), any(PlatformRegistrationDTO.class));
            verify(session, never()).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationError", registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationNoResponseFromPlatformConfiguration() {
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(null);

        try {
            String registrationOutput = registrationController.registration(TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session, never()).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), any(PlatformRegistrationDTO.class));
            verify(session, never()).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationError", registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationNot2xxStatusForPlatformConfiguration() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.NOT_FOUND);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try {
            String registrationOutput = registrationController.registration(TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session, never()).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), any(PlatformRegistrationDTO.class));
            verify(session, never()).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationError", registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }

    }

    private PlatformRegistrationDTO platformWith(String issuer, PlatformConfigurationDTO platformConfiguration) {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setIssuer(issuer);
        platformRegistration.setClaims_supported(Arrays.asList("sub", "iss"));
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setPlatformConfiguration(platformConfiguration);
        return platformRegistration;
    }

    private static MessagesSupportedDTO message(String type, List<String> placements) {
        MessagesSupportedDTO message = new MessagesSupportedDTO();
        message.setType(type);
        message.setPlacements(placements);
        return message;
    }

    private static PlatformConfigurationDTO platformConfiguration(MessagesSupportedDTO... messages) {
        PlatformConfigurationDTO platformConfiguration = new PlatformConfigurationDTO();
        platformConfiguration.setMessages_supported(messages == null ? null : Arrays.asList(messages));
        return platformConfiguration;
    }

    // runs the demo registration and returns the tool configuration that was generated for the platform
    private ToolRegistrationDTO generatedToolRegistration(PlatformRegistrationDTO platformRegistration) {
        ReflectionTestUtils.setField(registrationController, "demoMode", true);
        ReflectionTestUtils.setField(registrationController, "localUrl", "https://tool.com");
        ReflectionTestUtils.setField(registrationController, "clientName", "Tool");
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class)))
                .thenReturn(new ResponseEntity<>(platformRegistration, HttpStatus.OK));

        assertEquals("registrationRedirect", registrationController.registration(TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model));

        ArgumentCaptor<ToolRegistrationDTO> captor = ArgumentCaptor.forClass(ToolRegistrationDTO.class);
        verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), captor.capture());
        verify(model).addAttribute(LtiStrings.TOOL_CONFIGURATION, captor.getValue());
        verify(model).addAttribute("own_redirect_post_endpoint", "https://tool.com/registration/");
        return captor.getValue();
    }

    private static ToolMessagesSupportedDTO messageOfType(ToolRegistrationDTO toolRegistration, String type) {
        return toolRegistration.getToolConfiguration().getMessages_supported().stream()
                .filter(m -> m.getType().equals(type)).findFirst().orElseThrow();
    }

    @Test
    public void testToolConfigurationForCanvasPlacements() {
        ToolRegistrationDTO tool = generatedToolRegistration(platformWith(TextConstants.CANVAS_ISSUER, platformConfiguration(
                message("LtiDeepLinkingRequest", Arrays.asList("assignment_selection", "link_selection")),
                message("LtiResourceLinkRequest", Arrays.asList("account_navigation", "course_navigation")))));

        assertEquals(List.of("link_selection"), messageOfType(tool, "LtiDeepLinkingRequest").getPlacements());
        assertEquals(List.of("course_navigation"), messageOfType(tool, "LtiResourceLinkRequest").getPlacements());
        assertEquals("public", tool.getToolConfiguration().getCanvasPrivacyLevel());
        assertEquals("Tool", messageOfType(tool, "LtiDeepLinkingRequest").getLabel());
        assertEquals("https://tool.com/lti3", messageOfType(tool, "LtiResourceLinkRequest").getTarget_link_uri());
        assertEquals("example-scope1 example-scope2", tool.getScope());
        List<String> claims = tool.getToolConfiguration().getClaims();
        assertEquals(List.of("sub", "iss"), claims.subList(0, 2));
        assertEquals(true, claims.containsAll(LtiStrings.LTI_OPTIONAL_CLAIMS));
    }

    @Test
    public void testToolConfigurationForD2lPlacements() {
        ToolRegistrationDTO tool = generatedToolRegistration(platformWith("https://school.brightspace.com", platformConfiguration(
                message("LtiDeepLinkingRequest", List.of("ContentArea")),
                message("LtiResourceLinkRequest", List.of("ContentArea")))));

        assertEquals(List.of("ContentArea"), messageOfType(tool, "LtiDeepLinkingRequest").getPlacements());
        // no course navigation placement offered, so none is requested
        assertNull(messageOfType(tool, "LtiResourceLinkRequest").getPlacements());
        assertNull(tool.getToolConfiguration().getCanvasPrivacyLevel());
    }

    @Test
    public void testToolConfigurationWithoutMatchingPlacements() {
        ToolRegistrationDTO tool = generatedToolRegistration(platformWith(TEST_PLATFORM_ISSUER, platformConfiguration(
                message("LtiDeepLinkingRequest", List.of("editor_button")))));

        assertEquals(List.of(""), messageOfType(tool, "LtiDeepLinkingRequest").getPlacements());
        assertNull(messageOfType(tool, "LtiResourceLinkRequest").getPlacements());
    }

    @Test
    public void testToolConfigurationWhenPlatformOffersNoDeepLinking() {
        ToolRegistrationDTO tool = generatedToolRegistration(platformWith(TEST_PLATFORM_ISSUER, platformConfiguration(
                message("LtiResourceLinkRequest", List.of("course_navigation")))));

        assertNull(messageOfType(tool, "LtiDeepLinkingRequest").getPlacements());
        assertEquals(List.of("course_navigation"), messageOfType(tool, "LtiResourceLinkRequest").getPlacements());
    }

    @Test
    public void testToolConfigurationWhenPlatformListsNoPlacements() {
        ToolRegistrationDTO tool = generatedToolRegistration(platformWith(TEST_PLATFORM_ISSUER, platformConfiguration(
                message("LtiDeepLinkingRequest", null),
                message("LtiResourceLinkRequest", null))));

        assertNull(messageOfType(tool, "LtiDeepLinkingRequest").getPlacements());
        assertNull(messageOfType(tool, "LtiResourceLinkRequest").getPlacements());
    }

    @Test
    public void testToolConfigurationWhenPlatformListsNoMessages() {
        ToolRegistrationDTO tool = generatedToolRegistration(platformWith(TEST_PLATFORM_ISSUER, platformConfiguration((MessagesSupportedDTO[]) null)));

        assertEquals(2, tool.getToolConfiguration().getMessages_supported().size());
        assertNull(messageOfType(tool, "LtiDeepLinkingRequest").getPlacements());
        assertNull(messageOfType(tool, "LtiResourceLinkRequest").getPlacements());
    }

    private PlatformRegistrationDTO givenRegistrationInSession() {
        PlatformRegistrationDTO platformRegistration = platformWith("https%3A%2F%2Fplatform.com", null);
        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        when(session.getAttribute(LtiStrings.REGISTRATION_TOKEN)).thenReturn(TEST_REGISTRATION_TOKEN);
        when(session.getAttribute(LtiStrings.PLATFORM_CONFIGURATION)).thenReturn(platformRegistration);
        when(session.getAttribute(LtiStrings.TOOL_CONFIGURATION)).thenReturn(toolRegistration);
        return platformRegistration;
    }

    @Test
    public void testRegistrationPostInDemoMode() throws Exception {
        ReflectionTestUtils.setField(registrationController, "demoMode", true);
        PlatformRegistrationDTO platformRegistration = givenRegistrationInSession();
        ToolConfigurationACKDTO answer = new ToolConfigurationACKDTO();
        answer.setClient_id("new-client");
        when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT))).thenReturn(answer);

        String output = registrationController.registrationPOST(req, model);

        assertEquals("registrationConfirmationDemo", output);
        verify(registrationService).saveNewPlatformDeployment(answer, platformRegistration);
        verify(model).addAttribute("registration_confirmation", answer.toString());
        verify(model).addAttribute("issuer", "https://platform.com");
    }

    @Test
    public void testRegistrationPostConnectionError() throws Exception {
        givenRegistrationInSession();
        when(registrationService.callDynamicRegistration(any(), any(), any())).thenThrow(new ConnectionException("platform unreachable"));

        String output = registrationController.registrationPOST(req, model);

        assertEquals("registrationError", output);
        verify(model).addAttribute("Error", "platform unreachable");
        verify(registrationService, never()).saveNewPlatformDeployment(any(), any());
    }

    @Test
    public void testRegistrationPostSaveError() throws Exception {
        givenRegistrationInSession();
        when(registrationService.callDynamicRegistration(any(), any(), any())).thenReturn(new ToolConfigurationACKDTO());
        doThrow(new RegistrationException("cannot save")).when(registrationService).saveNewPlatformDeployment(any(), any());

        String output = registrationController.registrationPOST(req, model);

        assertEquals("registrationError", output);
        verify(model).addAttribute("Error", "cannot save");
    }

    @Test
    public void testRegistrationCreatesItsOwnRestTemplate() throws Exception {
        ReflectionTestUtils.setField(registrationController, "restTemplate", null);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/openid-configuration";

            String output = registrationController.registration(url, null, req, model);

            assertEquals("registrationError", output);
            assertNotNull(ReflectionTestUtils.getField(registrationController, "restTemplate"));
            verify(model).addAttribute(eq("Error"), any(String.class));
        } finally {
            server.stop(0);
        }
    }
}
