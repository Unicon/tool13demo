package net.unicon.lti.service.lti;

import org.springframework.test.util.ReflectionTestUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.RegistrationException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.lti.dto.ToolConfigurationACKDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.service.lti.impl.RegistrationServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegistrationServiceTest {

    private static final String TEST_REGISTRATION_ENDPOINT = "https://platform.com/register-tool";

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Mock
    private ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // created lazily rather than injected, so Mockito's constructor injection doesn't set it
        ReflectionTestUtils.setField(registrationService, "restTemplate", restTemplate);
    }

    @Test
    public void testCallDynamicRegistration() {
        ToolConfigurationACKDTO toolConfigurationACKDTO = new ToolConfigurationACKDTO();
        toolConfigurationACKDTO.setClient_id("test-success");
        ResponseEntity<ToolConfigurationACKDTO> response = new ResponseEntity<>(toolConfigurationACKDTO, HttpStatus.OK);
        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(), eq(ToolConfigurationACKDTO.class))).thenReturn(response);

        try {
            ToolConfigurationACKDTO answer = registrationService.callDynamicRegistration(null, toolRegistration, TEST_REGISTRATION_ENDPOINT);

            verify(restTemplate).exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST),
                    argThat((HttpEntity entity) ->
                            entity.getHeaders().get(HttpHeaders.AUTHORIZATION) == null && entity.getBody() == toolRegistration
                    ),
                    eq(ToolConfigurationACKDTO.class));
            verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
            assertEquals(answer.getClient_id(), "test-success");
        } catch (ConnectionException e) {
            fail();
        }
    }

    @Test
    public void testCallDynamicRegistrationWithToken() {
        String mockToken = "mock-token";
        ToolConfigurationACKDTO toolConfigurationACKDTO = new ToolConfigurationACKDTO();
        toolConfigurationACKDTO.setClient_id("test-success");
        ResponseEntity<ToolConfigurationACKDTO> response = new ResponseEntity<>(toolConfigurationACKDTO, HttpStatus.OK);

        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(), eq(ToolConfigurationACKDTO.class))).thenReturn(response);

        try {
            ToolConfigurationACKDTO answer = registrationService.callDynamicRegistration(mockToken, toolRegistration, TEST_REGISTRATION_ENDPOINT);

            verify(restTemplate).setUriTemplateHandler(any(UriTemplateHandler.class));
            verify(restTemplate).exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST),
                    argThat((HttpEntity entity) ->
                            StringUtils.equals(entity.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0), "Bearer " + mockToken) && entity.getBody() == toolRegistration
                    ),
                    eq(ToolConfigurationACKDTO.class));
            verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
            assertEquals(answer.getClient_id(), "test-success");
        } catch (ConnectionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testCallDynamicRegistrationNot2xxSuccessful() throws ConnectionException {
        ToolRegistrationDTO toolRegistration = new ToolRegistrationDTO();
        ToolConfigurationACKDTO toolConfigurationACKDTO = new ToolConfigurationACKDTO();
        toolConfigurationACKDTO.setClient_id("test-error");
        ResponseEntity<ToolConfigurationACKDTO> response = new ResponseEntity<>(toolConfigurationACKDTO, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(), eq(ToolConfigurationACKDTO.class))).thenReturn(response);

        assertThrows(ConnectionException.class, () -> {
            registrationService.callDynamicRegistration(null, toolRegistration, TEST_REGISTRATION_ENDPOINT);
        });

        verify(restTemplate).exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST),
                argThat((HttpEntity entity) ->
                        entity.getHeaders().get(HttpHeaders.AUTHORIZATION) == null && entity.getBody() == toolRegistration
                ),
                eq(ToolConfigurationACKDTO.class));
        verify(exceptionMessageGenerator).exceptionMessage(any(String.class), any(Exception.class));
    }


    @Test
    public void testCallDynamicRegistrationLogsTheRequestWhenDebugging() throws ConnectionException {
        Logger logger = (Logger) LoggerFactory.getLogger(RegistrationServiceImpl.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        try {
            ToolConfigurationACKDTO ack = new ToolConfigurationACKDTO();
            ack.setClient_id("debug-client");
            when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(), eq(ToolConfigurationACKDTO.class)))
                    .thenReturn(new ResponseEntity<>(ack, HttpStatus.CREATED));

            ToolConfigurationACKDTO answer = registrationService.callDynamicRegistration("token", new ToolRegistrationDTO(), TEST_REGISTRATION_ENDPOINT);

            assertEquals("debug-client", answer.getClient_id());
        } finally {
            logger.setLevel(previous);
        }
    }

    @Test
    public void testCallDynamicRegistrationWithoutDebugLogging() throws ConnectionException {
        Logger logger = (Logger) LoggerFactory.getLogger(RegistrationServiceImpl.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            ToolConfigurationACKDTO ack = new ToolConfigurationACKDTO();
            ack.setClient_id("quiet-client");
            when(restTemplate.exchange(eq(TEST_REGISTRATION_ENDPOINT), eq(HttpMethod.POST), any(), eq(ToolConfigurationACKDTO.class)))
                    .thenReturn(new ResponseEntity<>(ack, HttpStatus.OK));

            assertEquals("quiet-client", registrationService.callDynamicRegistration(null, new ToolRegistrationDTO(), TEST_REGISTRATION_ENDPOINT).getClient_id());
        } finally {
            logger.setLevel(previous);
        }
    }

    @Test
    public void testCallDynamicRegistrationCreatesItsOwnRestTemplate() {
        ReflectionTestUtils.setField(registrationService, "restTemplate", null);
        when(exceptionMessageGenerator.exceptionMessage(any(String.class), any(Exception.class))).thenReturn("generated message");

        // a relative URL fails before any connection is attempted
        ConnectionException e = assertThrows(ConnectionException.class,
                () -> registrationService.callDynamicRegistration(null, new ToolRegistrationDTO(), "not-an-absolute-url"));

        assertEquals("generated message", e.getMessage());
        assertNotNull(ReflectionTestUtils.getField(registrationService, "restTemplate"));
        verify(exceptionMessageGenerator).exceptionMessage(eq("Problem during the registration"), any(IllegalArgumentException.class));
    }

    private static ToolConfigurationACKDTO ackFor(String clientId, String deploymentId) {
        ToolConfigurationACKDTO answer = new ToolConfigurationACKDTO();
        answer.setClient_id(clientId);
        ToolConfigurationDTO toolConfiguration = new ToolConfigurationDTO();
        toolConfiguration.setDeployment_id(deploymentId);
        answer.setToolConfiguration(toolConfiguration);
        return answer;
    }

    private static PlatformRegistrationDTO platformFor(String issuer) {
        PlatformRegistrationDTO platform = new PlatformRegistrationDTO();
        platform.setIssuer(issuer);
        platform.setJwks_uri(issuer + "/jwks");
        platform.setToken_endpoint(issuer + "/token");
        platform.setAuthorization_endpoint(issuer + "/authorize");
        return platform;
    }

    private PlatformDeployment savedDeployment() {
        ArgumentCaptor<PlatformDeployment> captor = ArgumentCaptor.forClass(PlatformDeployment.class);
        verify(platformDeploymentRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    public void testSaveNewPlatformDeployment() throws RegistrationException {
        registrationService.saveNewPlatformDeployment(ackFor("client-1", "deployment-1"), platformFor("https://canvas.instructure.com"));

        PlatformDeployment saved = savedDeployment();
        assertEquals("client-1", saved.getClientId());
        assertEquals("deployment-1", saved.getDeploymentId());
        assertEquals("https://canvas.instructure.com", saved.getIss());
        assertEquals("https://canvas.instructure.com/jwks", saved.getJwksEndpoint());
        assertEquals("https://canvas.instructure.com/token", saved.getoAuth2TokenUrl());
        assertEquals("https://canvas.instructure.com/authorize", saved.getOidcEndpoint());
        assertNull(saved.getoAuth2TokenAud());
    }

    @Test
    public void testSaveNewPlatformDeploymentFixesTheBlackboardIssuer() throws RegistrationException {
        registrationService.saveNewPlatformDeployment(ackFor("client-1", "deployment-1"), platformFor("https://developer.blackboard.com"));

        assertEquals("https://blackboard.com", savedDeployment().getIss());
    }

    @Test
    public void testSaveNewPlatformDeploymentSetsTheBrightspaceAudience() throws RegistrationException {
        registrationService.saveNewPlatformDeployment(ackFor("client-1", "deployment-1"), platformFor("https://school.brightspace.com"));

        PlatformDeployment saved = savedDeployment();
        assertEquals("https://school.brightspace.com", saved.getIss());
        assertEquals("https://school.brightspace.com/token", saved.getoAuth2TokenAud());
    }

    @Test
    public void testSaveNewPlatformDeploymentFailure() {
        when(platformDeploymentRepository.save(any())).thenThrow(new RuntimeException("db down"));
        when(exceptionMessageGenerator.exceptionMessage(any(String.class), any(Exception.class))).thenReturn("generated message");

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> registrationService.saveNewPlatformDeployment(ackFor("client-1", "deployment-1"), platformFor("https://platform.com")));

        assertEquals("generated message", e.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(eq("Problem during the registration. Not able to save the new configration"), any(RuntimeException.class));
    }

}
