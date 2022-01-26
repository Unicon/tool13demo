package net.unicon.lti.service.lti.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
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
    private RegistrationService registrationService = new RegistrationServiceImpl();

    @Mock
    private ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
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

}
