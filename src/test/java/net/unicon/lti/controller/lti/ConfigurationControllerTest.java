package net.unicon.lti.controller.lti;

import net.unicon.lti.model.AlternativeDomain;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AlternativeDomainRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@WebMvcTest(ConfigurationController.class)
public class ConfigurationControllerTest {
    private PlatformDeployment platformDeployment = new PlatformDeployment();
    private AlternativeDomain alternativeDomain = new AlternativeDomain("domain1", "The alternative name", null, null, null, null);

    @InjectMocks
    private ConfigurationController configurationController = new ConfigurationController();

    @MockBean
    private PlatformDeploymentRepository platformDeploymentRepository;

    @MockBean
    private AlternativeDomainRepository alternativeDomainRepository;

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
    public void testDisplayAltDomains() {
        Page<AlternativeDomain> alternativeDomainsPage = new PageImpl<>(Arrays.asList(alternativeDomain));
        ResponseEntity<Page<AlternativeDomain>> alternativeDomainsResponseEntity = new ResponseEntity<>(alternativeDomainsPage, HttpStatus.OK);
        when(alternativeDomainRepository.findAll(any(PageRequest.class))).thenReturn(alternativeDomainsPage);

        ResponseEntity<Page<AlternativeDomain>> found = configurationController.displayAltDomains(0, 10);
        Mockito.verify(alternativeDomainRepository).findAll(any(PageRequest.class));
        assertEquals(alternativeDomainsResponseEntity.getStatusCode(), found.getStatusCode());
        assertEquals(alternativeDomainsResponseEntity.getBody(), found.getBody());
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
    public void testDisplayAltDomainsNotFound() {
        Page<AlternativeDomain> alternativeDomainsPage = new PageImpl<>(Arrays.asList());
        ResponseEntity<Page<AlternativeDomain>> alternativeDomainsResponseEntity = new ResponseEntity<>(alternativeDomainsPage, HttpStatus.OK);
        when(alternativeDomainRepository.findAll(any(PageRequest.class))).thenReturn(alternativeDomainsPage);

        ResponseEntity<Page<AlternativeDomain>> found = configurationController.displayAltDomains(0, 10);
        Mockito.verify(alternativeDomainRepository).findAll(any(PageRequest.class));
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
    public void testDisplayAltDomain() {
        ResponseEntity<AlternativeDomain> alternativeDomainResponseEntity = new ResponseEntity<>(alternativeDomain, HttpStatus.OK);
        when(alternativeDomainRepository.findById("domain1")).thenReturn(java.util.Optional.ofNullable(alternativeDomain));

        ResponseEntity<AlternativeDomain> found = configurationController.displayAltDomain("domain1");
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals(alternativeDomainResponseEntity.getBody(), found.getBody());
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
    public void testDisplayAltDomainNotFound() {
        when(alternativeDomainRepository.findById("domain1")).thenReturn(java.util.Optional.ofNullable(null));

        ResponseEntity<AlternativeDomain> found = configurationController.displayAltDomain("domain1");
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        assertEquals(HttpStatus.NOT_FOUND, found.getStatusCode());
        assertEquals("alt domain domain1 not found", found.getBody());
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
    public void testCreateAltDomain() {
        ResponseEntity<AlternativeDomain> alternativeDomainResponseEntity = new ResponseEntity<>(alternativeDomain, HttpStatus.CREATED);
        alternativeDomain.setDomainUrl("https://domain.domain1.tool.com");
        alternativeDomain.setLocalUrl("https://home.domain1.tool.com");
        alternativeDomain.setDescription("Description text");
        alternativeDomain.setMenuLabel("Menu Label");

        when(alternativeDomainRepository.findById("domain1")).thenReturn(Optional.ofNullable(null));
        when(alternativeDomainRepository.findByName("The alternative name")).thenReturn(new ArrayList<>());
        when(alternativeDomainRepository.save(alternativeDomain)).thenReturn(alternativeDomain);

        ResponseEntity<AlternativeDomain> found = configurationController.createAltDomain(alternativeDomain);
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        Mockito.verify(alternativeDomainRepository).findByName("The alternative name");
        Mockito.verify(alternativeDomainRepository).save(alternativeDomain);
        assertEquals(alternativeDomainResponseEntity.getStatusCode(), found.getStatusCode());
        assertEquals(alternativeDomainResponseEntity.getBody(), found.getBody());
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
    public void testCreateAltDomainAlreadyCreated() {
        alternativeDomain.setDomainUrl("https://domain.domain1.tool.com");
        alternativeDomain.setLocalUrl("https://home.domain1.tool.com");
        alternativeDomain.setDescription("Description text");
        alternativeDomain.setMenuLabel("Menu Label");
        when(alternativeDomainRepository.findById("domain1")).thenReturn(Optional.of(alternativeDomain));
        when(alternativeDomainRepository.findByName("The alternative name")).thenReturn(Arrays.asList(alternativeDomain));

        ResponseEntity<AlternativeDomain> found = configurationController.createAltDomain(alternativeDomain);
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        Mockito.verify(alternativeDomainRepository, never()).findByName("The alternative name");
        Mockito.verify(alternativeDomainRepository, never()).save(alternativeDomain);
        assertEquals(HttpStatus.CONFLICT, found.getStatusCode());
        assertEquals("Unable to create. A domain called " + alternativeDomain.getAltDomain() + " already exist", found.getBody());
    }

    @Test
    public void testCreateAltDomainSameNameCreated() {
        alternativeDomain.setDomainUrl("https://domain.domain1.tool.com");
        alternativeDomain.setLocalUrl("https://home.domain1.tool.com");
        alternativeDomain.setDescription("Description text");
        alternativeDomain.setMenuLabel("Menu Label");
        AlternativeDomain alternativeDomain2 = new AlternativeDomain("domain2", "The alternative name", null, null, null, null);
        when(alternativeDomainRepository.findById("domain2")).thenReturn(Optional.ofNullable(null));
        when(alternativeDomainRepository.findByName("The alternative name")).thenReturn(Arrays.asList(alternativeDomain));

        ResponseEntity<AlternativeDomain> found = configurationController.createAltDomain(alternativeDomain2);
        Mockito.verify(alternativeDomainRepository).findById("domain2");
        Mockito.verify(alternativeDomainRepository).findByName("The alternative name");
        Mockito.verify(alternativeDomainRepository, never()).save(alternativeDomain2);
        assertEquals(HttpStatus.CONFLICT, found.getStatusCode());
        assertEquals("Unable to create. The name " + alternativeDomain.getName() + " needs to be unique and it is used in " + alternativeDomain.getAltDomain(), found.getBody());
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
    public void testUpdateAltDomain() {
        ResponseEntity<AlternativeDomain> alternativeDomainResponseEntity = new ResponseEntity<>(alternativeDomain, HttpStatus.OK);
        when(alternativeDomainRepository.findById("domain1")).thenReturn(java.util.Optional.ofNullable(alternativeDomain));
        when(alternativeDomainRepository.findByName("The alternative name")).thenReturn(Arrays.asList(alternativeDomain));
        ResponseEntity<AlternativeDomain> found = configurationController.updateAltDomain("domain1", alternativeDomain);
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        Mockito.verify(alternativeDomainRepository).saveAndFlush(alternativeDomain);
        assertEquals(alternativeDomainResponseEntity.getStatusCode(), found.getStatusCode());
        assertEquals(alternativeDomainResponseEntity.getBody(), found.getBody());
    }

    @Test
    public void testUpdateAltDomainOtherWithSameName() {
        AlternativeDomain alternativeDomain2 = new AlternativeDomain("domain2", "The alternative name", null, null, null, null);
        when(alternativeDomainRepository.findById("domain1")).thenReturn(java.util.Optional.ofNullable(alternativeDomain));
        when(alternativeDomainRepository.findByName("The alternative name")).thenReturn(Arrays.asList(alternativeDomain2));
        ResponseEntity<AlternativeDomain> found = configurationController.updateAltDomain("domain1", alternativeDomain);
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        Mockito.verify(alternativeDomainRepository, never()).saveAndFlush(alternativeDomain);
        assertEquals(HttpStatus.CONFLICT, found.getStatusCode());
        assertEquals("Unable to edit. The name " + alternativeDomain.getName() + " needs to be unique and it is used in " + alternativeDomain2.getAltDomain(), found.getBody());
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

    @Test
    public void testUpdateAltDomainNotFound() {
        when(alternativeDomainRepository.findById("domain1")).thenReturn(java.util.Optional.ofNullable(null));

        ResponseEntity<AlternativeDomain> found = configurationController.updateAltDomain("domain1", alternativeDomain);
        Mockito.verify(alternativeDomainRepository).findById("domain1");
        Mockito.verify(alternativeDomainRepository, never()).saveAndFlush(alternativeDomain);
        assertEquals(HttpStatus.NOT_FOUND, found.getStatusCode());
        assertEquals("Unable to create. A domain called " + alternativeDomain.getAltDomain() + " does not exist", found.getBody());
    }
}
