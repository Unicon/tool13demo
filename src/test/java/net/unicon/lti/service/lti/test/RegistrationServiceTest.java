package net.unicon.lti.service.lti.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationDTO;
import net.unicon.lti.model.lti.dto.ToolMessagesSupportedDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.service.lti.impl.RegistrationServiceImpl;
import net.unicon.lti.utils.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import java.util.Arrays;
import java.util.List;

import static net.unicon.lti.utils.LtiStrings.LTI_OPTIONAL_CLAIMS;
import static net.unicon.lti.utils.TextConstants.LTI3_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegistrationServiceTest {
    private static final String TEST_PLATFORM_ISSUER = "https://platform.com";
    private static final List TEST_SCOPES = Arrays.asList("example-scope1", "example-scope2");
    private static final String TEST_REGISTRATION_ENDPOINT = "https://platform.com/register-tool";
    private static final String LOCAL_URL = "localUrl";
    private static final String SAMPLE_LOCAL_URL = "https://tool.com";
    private static final String DOMAIN_URL = "domainUrl";
    private static final String SAMPLE_DOMAIN_URL = "https://domain.tool.com";
    private static final String CLIENT_NAME = "clientName";
    private static final String SAMPLE_CLIENT_NAME = "Tool Name";
    private static final String DESCRIPTION = "description";
    private static final String SAMPLE_DESCRIPTION = "LTI 1.3 Tool Description";
    private static final String DEEP_LINKING_MENU_LABEL = "deepLinkingMenuLabel";
    private static final String SAMPLE_DEEP_LINKING_MENU_LABEL = "Tool Content Selector";

    @InjectMocks
    private RegistrationService registrationService = new RegistrationServiceImpl();

    @Mock
    private ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    private RestTemplate restTemplate;

    private MockedStatic<RestUtils> restUtilsMockedStatic;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        restUtilsMockedStatic = Mockito.mockStatic(RestUtils.class);
        restUtilsMockedStatic.when(RestUtils::createRestTemplate).thenReturn(restTemplate);
        ReflectionTestUtils.setField(registrationService, LOCAL_URL, SAMPLE_LOCAL_URL);
        ReflectionTestUtils.setField(registrationService, DOMAIN_URL, SAMPLE_DOMAIN_URL);
        ReflectionTestUtils.setField(registrationService, CLIENT_NAME, SAMPLE_CLIENT_NAME);
        ReflectionTestUtils.setField(registrationService, DESCRIPTION, SAMPLE_DESCRIPTION);
        ReflectionTestUtils.setField(registrationService, DEEP_LINKING_MENU_LABEL, SAMPLE_DEEP_LINKING_MENU_LABEL);
    }

    @AfterEach
    public void close() {
        restUtilsMockedStatic.close();
    }

    @Test
    public void testCallDynamicRegistration() {
        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenReturn(new ResponseEntity<>("test-success", HttpStatus.OK));

        try {
            String answer = registrationService.callDynamicRegistration(null, toolRegistration, TEST_REGISTRATION_ENDPOINT);

            verify(restTemplate).exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST),
                    argThat((HttpEntity entity) ->
                            entity.getHeaders().get(HttpHeaders.AUTHORIZATION) == null && entity.getBody() == toolRegistration
                    ),
                    eq(String.class));
            verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
            assertEquals(answer, "test-success");
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testCallDynamicRegistrationWithToken() {
        String mockToken = "mock-token";
        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenReturn(new ResponseEntity<>("test-success", HttpStatus.OK));

        try {
            String answer = registrationService.callDynamicRegistration(mockToken, toolRegistration, TEST_REGISTRATION_ENDPOINT);

            verify(restTemplate).setUriTemplateHandler(any(UriTemplateHandler.class));
            verify(restTemplate).exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST),
                    argThat((HttpEntity entity) ->
                            StringUtils.equals(entity.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0), "Bearer " + mockToken) && entity.getBody() == toolRegistration
                    ),
                    eq(String.class));
            verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
            assertEquals(answer, "test-success");
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testCallDynamicRegistrationNot2xxSuccessful() throws ConnectionException {
        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenReturn(new ResponseEntity<>("test-failure", HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ConnectionException.class, () -> {
            registrationService.callDynamicRegistration(null, toolRegistration, TEST_REGISTRATION_ENDPOINT);
        });

        verify(restTemplate).exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST),
                argThat((HttpEntity entity) ->
                        entity.getHeaders().get(HttpHeaders.AUTHORIZATION) == null && entity.getBody() == toolRegistration
                ),
                eq(String.class));
        verify(exceptionMessageGenerator).exceptionMessage(any(String.class), any(Exception.class));
    }

    @Test
    public void testGenerateToolConfiguration() {
        PlatformRegistrationDTO platformRegistration = new PlatformRegistrationDTO();
        List<String> customClaims = List.of("custom-claim-1", "custom-claim-2");
        platformRegistration.setClaims_supported(customClaims);
        platformRegistration.setScopes_supported(TEST_SCOPES);
        platformRegistration.setRegistration_endpoint(TEST_REGISTRATION_ENDPOINT);
        platformRegistration.setIssuer(TEST_PLATFORM_ISSUER);

        ToolRegistrationDTO toolRegistration = registrationService.generateToolConfiguration(platformRegistration);

        // Validate required constants set
        assertEquals("web", toolRegistration.getApplication_type());
        assertTrue(toolRegistration.getGrant_types().containsAll(List.of("implicit", "client_credentials")));
        assertEquals("id_token", toolRegistration.getResponse_types().get(0));
        assertEquals("private_key_jwt", toolRegistration.getToken_endpoint_auth_method());

        // Validate tool urls/data set
        assertEquals(SAMPLE_LOCAL_URL + LTI3_SUFFIX, toolRegistration.getRedirect_uris().get(0));
        assertEquals(SAMPLE_LOCAL_URL + "/oidc/login_initiations", toolRegistration.getInitiate_login_uri());
        assertEquals(SAMPLE_CLIENT_NAME, toolRegistration.getClient_name());
        assertEquals(SAMPLE_LOCAL_URL + "/jwks/jwk", toolRegistration.getJwks_uri());
        ToolConfigurationDTO toolConfiguration = toolRegistration.getToolConfiguration();
        assertEquals(SAMPLE_DOMAIN_URL, toolConfiguration.getDomain());
        assertEquals(SAMPLE_DOMAIN_URL, toolConfiguration.getTarget_link_uri());
        assertEquals(SAMPLE_DESCRIPTION, toolConfiguration.getDescription());

        // Validate tool supports Deep Linking
        List<ToolMessagesSupportedDTO> toolMessagesSupportedList = toolConfiguration.getMessages_supported();
        ToolMessagesSupportedDTO deepLinkingMessageSupported = toolMessagesSupportedList.get(0);
        assertEquals("LtiDeepLinkingRequest", deepLinkingMessageSupported.getType());
        assertEquals(SAMPLE_LOCAL_URL + LTI3_SUFFIX, deepLinkingMessageSupported.getTarget_link_uri());
        assertEquals(SAMPLE_DEEP_LINKING_MENU_LABEL, deepLinkingMessageSupported.getLabel());

        // Validate tool supports LTI Core SSO Standard Launch (aka LtiResourceLinkRequest)
        ToolMessagesSupportedDTO resourceLinkMessageSupported = toolMessagesSupportedList.get(1);
        assertEquals("LtiResourceLinkRequest", resourceLinkMessageSupported.getType());
        assertEquals(SAMPLE_LOCAL_URL + LTI3_SUFFIX, resourceLinkMessageSupported.getTarget_link_uri());

        // Validate tool accepts the claims and scopes from the platform
        assertTrue(toolConfiguration.getClaims().containsAll(LTI_OPTIONAL_CLAIMS));
        assertTrue(toolConfiguration.getClaims().containsAll(customClaims));
        assertEquals("example-scope1 example-scope2", toolRegistration.getScope());
    }
}
