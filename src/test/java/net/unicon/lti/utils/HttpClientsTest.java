package net.unicon.lti.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpClientsTest {

    @Test
    public void testRestTemplatesBufferTheirRequests() {
        RestTemplate first = HttpClients.restTemplate();
        RestTemplate second = HttpClients.restTemplate();

        assertInstanceOf(BufferingClientHttpRequestFactory.class, first.getRequestFactory());
        assertInstanceOf(BufferingClientHttpRequestFactory.class, HttpClients.requestFactory());
        // each caller gets its own RestTemplate, so one can add converters without affecting others
        assertNotSame(first, second);
    }

    @Test
    public void testTimeouts() {
        assertEquals(Duration.ofSeconds(10), HttpClients.CONNECT_TIMEOUT);
        assertEquals(Duration.ofSeconds(30), HttpClients.READ_TIMEOUT);
    }

    @Test
    public void testCannotBeInstantiated() throws Exception {
        Constructor<HttpClients> constructor = HttpClients.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

}
