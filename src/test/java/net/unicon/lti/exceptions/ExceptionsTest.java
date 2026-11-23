package net.unicon.lti.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ExceptionsTest {

    @Test
    public void testExceptionsKeepTheirMessage() {
        assertMessage(new BadTokenException("bad token"), "bad token");
        assertMessage(new DataServiceException("data"), "data");
        assertMessage(new RegistrationException("registration"), "registration");
        assertMessage(new ConnectionException("connection"), "connection");
    }

    private void assertMessage(Exception exception, String message) {
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }
}
