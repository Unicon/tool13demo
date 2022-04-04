package net.unicon.lti.controller.lti;

import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@WebMvcTest(ConfigurationController.class)
public class ConfigurationControllerTest {
    private PlatformDeployment platformDeployment = new PlatformDeployment();

    @InjectMocks
    private ConfigurationController configurationController = new ConfigurationController();

    @MockBean
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Configuration
    static class ContextConfiguration {
    }

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
        assertEquals("platformDeployment with id 1 not found", found.getBody());
    }

    @Test
    public void testCreateDeployment() {
        ResponseEntity<PlatformDeployment> platformDeploymentResponseEntity = new ResponseEntity<>(platformDeployment, HttpStatus.CREATED);
        platformDeployment.setIss("https://lms.com");
        platformDeployment.setDeploymentId("deploymentId");
        platformDeployment.setClientId("clientId");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId")).thenReturn(new ArrayList<>());
        when(platformDeploymentRepository.save(platformDeployment)).thenReturn(platformDeployment);

        ResponseEntity<PlatformDeployment> found = configurationController.createDeployment(platformDeployment);
        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId");
        Mockito.verify(platformDeploymentRepository).save(platformDeployment);
        assertEquals(platformDeploymentResponseEntity.getStatusCode(), found.getStatusCode());
        assertEquals(platformDeploymentResponseEntity.getBody(), found.getBody());
    }

    @Test
    public void testCreateDeploymentAlreadyCreated() {
        platformDeployment.setIss("https://lms.com");
        platformDeployment.setDeploymentId("deploymentId");
        platformDeployment.setClientId("clientId");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId")).thenReturn(Arrays.asList(platformDeployment));

        ResponseEntity<PlatformDeployment> found = configurationController.createDeployment(platformDeployment);
        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId("https://lms.com", "clientId", "deploymentId");
        Mockito.verify(platformDeploymentRepository, never()).save(platformDeployment);
        assertEquals(HttpStatus.CONFLICT, found.getStatusCode());
        assertEquals("Unable to create. This platformDeployment already exists.", found.getBody());
    }

    @Test
    public void testUpdateDeployment() {
        ResponseEntity<PlatformDeployment> platformDeploymentResponseEntity = new ResponseEntity<>(platformDeployment, HttpStatus.OK);
        when(platformDeploymentRepository.findById(1L)).thenReturn(java.util.Optional.ofNullable(platformDeployment));

        ResponseEntity<PlatformDeployment> found = configurationController.updateDeployment(1L, platformDeployment);
        Mockito.verify(platformDeploymentRepository).findById(1L);
        Mockito.verify(platformDeploymentRepository).saveAndFlush(platformDeployment);
        assertEquals(platformDeploymentResponseEntity.getStatusCode(), found.getStatusCode());
        assertEquals(platformDeploymentResponseEntity.getBody(), found.getBody());
    }

    @Test
    public void testUpdateDeploymentNotFound() {
        when(platformDeploymentRepository.findById(1L)).thenReturn(java.util.Optional.ofNullable(null));

        ResponseEntity<PlatformDeployment> found = configurationController.updateDeployment(1L, platformDeployment);
        Mockito.verify(platformDeploymentRepository).findById(1L);
        Mockito.verify(platformDeploymentRepository, never()).saveAndFlush(platformDeployment);
        assertEquals(HttpStatus.NOT_FOUND, found.getStatusCode());
        assertEquals("Unable to update. User with id 1 not found", found.getBody());
    }
}