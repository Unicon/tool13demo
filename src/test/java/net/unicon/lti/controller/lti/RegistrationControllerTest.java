package net.unicon.lti.controller.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.NoExistingDomainException;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.LtiSystemErrorEnum;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.RestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegistrationControllerTest {
    private static final String D2L_DUPLICATE_REGISTRATION_ERROR = "400 Bad Request: \"{\"error\":\"invalid_registration_data\",\"error_description\":\"Name must be unique across the org\"}\"";
    private static final String TEST_OPENID_CONFIGURATION_URL = "https://platform.com/get-configuration";
    private static final String TEST_ALT_DOMAIN_STRING = "domain1";
    private static final String TEST_REGISTRATION_TOKEN = "test-registration-token";
    private static final String TEST_REGISTRATION_ENDPOINT = "https://platform.com/register-tool";
    private static final String TEST_PLATFORM_ISSUER = "https://platform.com";
    private static final List TEST_SCOPES = Arrays.asList("example-scope1", "example-scope2");

    @InjectMocks
    private RegistrationController registrationController = new RegistrationController();

    @Mock
    private RegistrationService registrationService;

    @Mock
    private RestTemplate restTemplate;

    private MockedStatic<RestUtils> restUtilsMockedStatic;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpSession session;

    private Model model = new ExtendedModelMap();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(req.getSession()).thenReturn(session);
        restUtilsMockedStatic = Mockito.mockStatic(RestUtils.class);
        restUtilsMockedStatic.when(RestUtils::createRestTemplate).thenReturn(restTemplate);
    }

    @AfterEach
    public void close() {
        restUtilsMockedStatic.close();
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

        try {
            when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT))).thenReturn("test-success");
            when(registrationService.generateToolConfiguration(eq(platformRegistration), eq(null))).thenReturn(new ToolRegistrationDTO());

            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).generateToolConfiguration(eq(platformRegistration), eq(null));
            verify(registrationService).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationConfirmation", registrationOutput);
        } catch (ConnectionException | NoExistingDomainException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationWithAltDomain() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setClaims_supported(LtiStrings.LTI_OPTIONAL_CLAIMS);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setIssuer(TEST_PLATFORM_ISSUER);
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try {
            when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT))).thenReturn("test-success");
            when(registrationService.generateToolConfiguration(eq(platformRegistration), eq(TEST_ALT_DOMAIN_STRING))).thenReturn(new ToolRegistrationDTO());

            String registrationOutput = registrationController.registration(TEST_ALT_DOMAIN_STRING, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).generateToolConfiguration(eq(platformRegistration), eq(TEST_ALT_DOMAIN_STRING));
            verify(registrationService).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationConfirmation", registrationOutput);
        } catch (ConnectionException | NoExistingDomainException e) {
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
        when(registrationService.generateToolConfiguration(eq(platformRegistration), eq(null))).thenReturn(new ToolRegistrationDTO());
        } catch (NoExistingDomainException e) {
            fail();
        }
        try {
            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).generateToolConfiguration(eq(platformRegistration), eq(null));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals("registrationRedirect", registrationOutput);
        } catch (ConnectionException | NoExistingDomainException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationNoPlatformConfiguration() {
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try {
            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session, never()).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), any(PlatformRegistrationDTO.class));
            verify(session, never()).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals(LtiSystemErrorEnum.DYNAMIC_REGISTRATION_GENERAL_ERROR.ordinal(), model.getAttribute(TextConstants.LTI_SYSTEM_ERROR));
            assertEquals(TextConstants.REACT_UI_TEMPLATE, registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationNoResponseFromPlatformConfiguration() {
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(null);

        try {
            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session, never()).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), any(PlatformRegistrationDTO.class));
            verify(session, never()).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals(LtiSystemErrorEnum.DYNAMIC_REGISTRATION_GENERAL_ERROR.ordinal(), model.getAttribute(TextConstants.LTI_SYSTEM_ERROR));
            assertEquals(TextConstants.REACT_UI_TEMPLATE, registrationOutput);
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
            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService, never()).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session, never()).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), any(PlatformRegistrationDTO.class));
            verify(session, never()).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals(LtiSystemErrorEnum.DYNAMIC_REGISTRATION_GENERAL_ERROR.ordinal(), model.getAttribute(TextConstants.LTI_SYSTEM_ERROR));
            assertEquals(TextConstants.REACT_UI_TEMPLATE, registrationOutput);
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationPOSTDuplicationError() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setClaims_supported(LtiStrings.LTI_OPTIONAL_CLAIMS);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setIssuer(TEST_PLATFORM_ISSUER);
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try {
            when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT)))
                    .thenThrow(new ConnectionException(D2L_DUPLICATE_REGISTRATION_ERROR));
            when(registrationService.generateToolConfiguration(eq(platformRegistration), eq(null))).thenReturn(new ToolRegistrationDTO());

            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).generateToolConfiguration(eq(platformRegistration), eq(null));
            verify(registrationService).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals(LtiSystemErrorEnum.DYNAMIC_REGISTRATION_DUPLICATE_ERROR.ordinal(), model.getAttribute(TextConstants.LTI_SYSTEM_ERROR));
            assertEquals(TextConstants.REACT_UI_TEMPLATE, registrationOutput);
        } catch (ConnectionException | NoExistingDomainException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationPOSTException() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setClaims_supported(LtiStrings.LTI_OPTIONAL_CLAIMS);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setIssuer(TEST_PLATFORM_ISSUER);
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try {
            when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT)))
                    .thenThrow(new ConnectionException(HttpStatus.INTERNAL_SERVER_ERROR.toString()));
            when(registrationService.generateToolConfiguration(eq(platformRegistration), eq(null))).thenReturn(new ToolRegistrationDTO());

            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).generateToolConfiguration(eq(platformRegistration), eq(null));
            verify(registrationService).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals(LtiSystemErrorEnum.DYNAMIC_REGISTRATION_GENERAL_ERROR.ordinal(), model.getAttribute(TextConstants.LTI_SYSTEM_ERROR));
            assertEquals(TextConstants.REACT_UI_TEMPLATE, registrationOutput);
        } catch (ConnectionException | NoExistingDomainException e) {
            fail();
        }
    }

    @Test
    public void testRegistrationPOSTEncodingException() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        platformRegistration.setClaims_supported(LtiStrings.LTI_OPTIONAL_CLAIMS);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setIssuer(TEST_PLATFORM_ISSUER);
        ResponseEntity<PlatformRegistrationDTO> responseEntity = new ResponseEntity<>(platformRegistration, HttpStatus.OK);
        when(restTemplate.exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class))).thenReturn(responseEntity);

        try (MockedStatic<URLDecoder> urlDecoder = Mockito.mockStatic(URLDecoder.class)) {
            urlDecoder.when(() -> URLDecoder.decode(TEST_PLATFORM_ISSUER, StandardCharsets.UTF_8.name())).thenAnswer(invocation -> {throw new UnsupportedEncodingException();});
            when(registrationService.callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT))).thenReturn("test-response");
            when(registrationService.generateToolConfiguration(eq(platformRegistration), eq(null))).thenReturn(new ToolRegistrationDTO());

            String registrationOutput = registrationController.registration(null, TEST_OPENID_CONFIGURATION_URL, TEST_REGISTRATION_TOKEN, req, model);

            verify(restTemplate).exchange(eq(TEST_OPENID_CONFIGURATION_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(PlatformRegistrationDTO.class));
            verify(registrationService).generateToolConfiguration(eq(platformRegistration), eq(null));
            verify(registrationService).callDynamicRegistration(eq(TEST_REGISTRATION_TOKEN), any(ToolRegistrationDTO.class), eq(TEST_REGISTRATION_ENDPOINT));
            verify(session).setAttribute(eq(LtiStrings.REGISTRATION_TOKEN), eq(TEST_REGISTRATION_TOKEN));
            verify(session).setAttribute(eq(LtiStrings.PLATFORM_CONFIGURATION), eq(platformRegistration));
            verify(session).setAttribute(eq(LtiStrings.TOOL_CONFIGURATION), any(ToolRegistrationDTO.class));
            assertEquals(LtiSystemErrorEnum.DYNAMIC_REGISTRATION_GENERAL_ERROR.ordinal(), model.getAttribute(TextConstants.LTI_SYSTEM_ERROR));
            assertEquals(TextConstants.REACT_UI_TEMPLATE, registrationOutput);
        } catch (ConnectionException | NoExistingDomainException e) {
            fail();
        }
    }
}
