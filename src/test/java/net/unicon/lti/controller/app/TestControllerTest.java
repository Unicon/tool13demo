package net.unicon.lti.controller.app;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class TestControllerTest {

    private final TestController testController = new TestController();

    private static void assertWelcome(ResponseEntity<?> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Welcome", response.getBody());
    }

    @Test
    public void testEndpointsWelcomeTheCaller() {
        assertWelcome(testController.sampleSecureEndpointAny());
        assertWelcome(testController.sampleSecureEndpointAdmin());
        assertWelcome(testController.sampleSecureEndpointInstructor(mock(Principal.class)));
        assertWelcome(testController.sampleSecureEndpointStudent());
    }
}
