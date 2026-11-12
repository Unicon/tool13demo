package net.unicon.lti.controller.lti;

import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.PlatformDeploymentRequest;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIJWTService;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class ConfigurationControllerTest {
    private PlatformDeployment platformDeployment = new PlatformDeployment();

    @InjectMocks
    private ConfigurationController configurationController;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private LTIJWTService ltijwtService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDisplayConfigs() {
        Page<PlatformDeployment> platformDeploymentPage = new PageImpl<>(Arrays.asList(platformDeployment));
        ResponseEntity<Page<PlatformDeployment>> platformDeploymentResponseEntity = new ResponseEntity<>(platformDeploymentPage, HttpStatus.OK);
        when(platformDeploymentRepository.findAll(any(PageRequest.class))).thenReturn(platformDeploymentPage);

        ResponseEntity<Page<PlatformDeployment>> found = configurationController.displayConfigs(0, 10);
        Mockito.verify(platformDeploymentRepository).findAll(any(PageRequest.class));
        assertEquals(platformDeploymentResponseEntity.getStatusCode(), found.getStatusCode());
        assertEquals(platformDeploymentResponseEntity.getBody(), found.getBody());
    }

    @Test
    public void testDisplayConfigsNotFound() {
        Page<PlatformDeployment> platformDeploymentPage = new PageImpl<>(new ArrayList<>());
        ResponseEntity<Page<PlatformDeployment>> platformDeploymentResponseEntity = new ResponseEntity<>(platformDeploymentPage, HttpStatus.OK);
        when(platformDeploymentRepository.findAll(any(PageRequest.class))).thenReturn(platformDeploymentPage);

        ResponseEntity<Page<PlatformDeployment>> found = configurationController.displayConfigs(0, 10);
        Mockito.verify(platformDeploymentRepository).findAll(any(PageRequest.class));
        assertEquals(HttpStatus.NO_CONTENT, found.getStatusCode());
        assertNull(found.getBody());
    }

    @Test
    public void testDisplayConfig() {
        ResponseEntity<PlatformDeployment> platformDeploymentResponseEntity = new ResponseEntity<>(platformDeployment, HttpStatus.OK);
        when(platformDeploymentRepository.findById(1L)).thenReturn(java.util.Optional.ofNullable(platformDeployment));

        ResponseEntity<PlatformDeployment> found = configurationController.displayConfig(1L);
        Mockito.verify(platformDeploymentRepository).findById(1L);
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals(platformDeploymentResponseEntity.getBody(), found.getBody());
    }

    @Test
    public void testDisplayConfigNotFound() {
        when(platformDeploymentRepository.findById(1L)).thenReturn(java.util.Optional.ofNullable(null));

        ResponseEntity<PlatformDeployment> found = configurationController.displayConfig(1L);
        Mockito.verify(platformDeploymentRepository).findById(1L);
        assertEquals(HttpStatus.NOT_FOUND, found.getStatusCode());
        Object body = found.getBody();
        assertEquals("platformDeployment with id 1 not found", body);
    }

    private static Map<String, String> assertionBody(String iss, String clientId, String deploymentId) {
        Map<String, String> body = new HashMap<>();
        body.put("iss", iss);
        body.put("client_id", clientId);
        body.put("deployment_id", deploymentId);
        return body;
    }

    private static PlatformDeploymentRequest request(String iss, String clientId, String deploymentId) {
        return new PlatformDeploymentRequest(iss, clientId, "https://lms.com/oidc", "https://lms.com/jwks",
            "https://lms.com/token", "https://lms.com/aud", deploymentId);
    }

    @Test
    public void testCreateDeployment() {
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId")).thenReturn(new ArrayList<>());
        when(platformDeploymentRepository.save(any(PlatformDeployment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<PlatformDeployment> found = configurationController.createDeployment(request("https://lms.com", "clientId", "deploymentId"));

        ArgumentCaptor<PlatformDeployment> saved = ArgumentCaptor.forClass(PlatformDeployment.class);
        Mockito.verify(platformDeploymentRepository).save(saved.capture());
        assertEquals(HttpStatus.CREATED, found.getStatusCode());
        assertSame(saved.getValue(), found.getBody());
        assertEquals(0L, saved.getValue().getKeyId(), "the request can't choose the key id");
        assertEquals("https://lms.com", saved.getValue().getIss());
        assertEquals("clientId", saved.getValue().getClientId());
        assertEquals("deploymentId", saved.getValue().getDeploymentId());
        assertEquals("https://lms.com/oidc", saved.getValue().getOidcEndpoint());
        assertEquals("https://lms.com/jwks", saved.getValue().getJwksEndpoint());
        assertEquals("https://lms.com/token", saved.getValue().getoAuth2TokenUrl());
        assertEquals("https://lms.com/aud", saved.getValue().getoAuth2TokenAud());
    }

    @Test
    public void testCreateDeploymentAlreadyCreated() {
        platformDeployment.setIss("https://lms.com");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId")).thenReturn(Arrays.asList(platformDeployment));

        ResponseEntity<PlatformDeployment> found = configurationController.createDeployment(request("https://lms.com", "clientId", "deploymentId"));
        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId");
        Mockito.verify(platformDeploymentRepository, never()).save(any());
        assertEquals(HttpStatus.CONFLICT, found.getStatusCode());
        Object body = found.getBody();
        assertEquals("Unable to create. This platformDeployment already exists.", body);
    }

    @Test
    public void testUpdateDeployment() {
        platformDeployment.setKeyId(1L);
        platformDeployment.setoAuth2TokenAud("https://lms.com/original-aud");
        when(platformDeploymentRepository.findById(1L)).thenReturn(java.util.Optional.of(platformDeployment));

        ResponseEntity<PlatformDeployment> found = configurationController.updateDeployment(1L, request("https://new.lms.com", "newClient", "newDeployment"));
        Mockito.verify(platformDeploymentRepository).findById(1L);
        Mockito.verify(platformDeploymentRepository).saveAndFlush(platformDeployment);
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertSame(platformDeployment, found.getBody());
        assertEquals(1L, platformDeployment.getKeyId());
        assertEquals("https://new.lms.com", platformDeployment.getIss());
        assertEquals("newClient", platformDeployment.getClientId());
        assertEquals("newDeployment", platformDeployment.getDeploymentId());
        assertEquals("https://lms.com/oidc", platformDeployment.getOidcEndpoint());
        assertEquals("https://lms.com/jwks", platformDeployment.getJwksEndpoint());
        assertEquals("https://lms.com/token", platformDeployment.getoAuth2TokenUrl());
        assertEquals("https://lms.com/original-aud", platformDeployment.getoAuth2TokenAud(), "an update keeps the token audience");
    }

    @Test
    public void testUpdateDeploymentNotFound() {
        when(platformDeploymentRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        ResponseEntity<PlatformDeployment> found = configurationController.updateDeployment(1L, request("https://lms.com", "clientId", "deploymentId"));
        Mockito.verify(platformDeploymentRepository).findById(1L);
        Mockito.verify(platformDeploymentRepository, never()).saveAndFlush(any());
        assertEquals(HttpStatus.NOT_FOUND, found.getStatusCode());
        Object body = found.getBody();
        assertEquals("Unable to update. User with id 1 not found", body);
    }

    @Test
    public void testClientAssertionIsForbiddenOutsideDemoMode() throws Exception {
        ResponseEntity<Map<String, String>> response = configurationController.generateClientAssertionJWT(assertionBody("iss", "client", "deployment"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Disabled: You do not have permission.", response.getBody().get("error"));
        Mockito.verifyNoInteractions(ltijwtService);
    }

    @Test
    public void testClientAssertionRequiresAllIdentifiers() throws Exception {
        ReflectionTestUtils.setField(configurationController, "demoMode", true);

        ResponseEntity<Map<String, String>> response = configurationController.generateClientAssertionJWT(assertionBody("iss", " ", "deployment"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("iss was iss, client_id was  , and deployment_id was deployment. All values must be provided.", response.getBody().get("error"));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(any(), any(), any());
    }

    @Test
    public void testClientAssertionRequiresExactlyOneDeployment() throws Exception {
        ReflectionTestUtils.setField(configurationController, "demoMode", true);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("iss", "client", "deployment")).thenReturn(List.of());

        ResponseEntity<Map<String, String>> response = configurationController.generateClientAssertionJWT(assertionBody("iss", "client", "deployment"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PlatformDeployment size was 0. There should be exactly 1.", response.getBody().get("error"));
        Mockito.verifyNoInteractions(ltijwtService);
    }

    @Test
    public void testClientAssertion() throws Exception {
        ReflectionTestUtils.setField(configurationController, "demoMode", true);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("iss", "client", "deployment")).thenReturn(List.of(platformDeployment));
        when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("client-assertion-jwt");

        ResponseEntity<Map<String, String>> response = configurationController.generateClientAssertionJWT(assertionBody("iss", "client", "deployment"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("client_assertion", "client-assertion-jwt"), response.getBody());
    }
}
