package net.unicon.lti.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CorsConfigurationSourceImplTest {

    @Test
    public void testDefaultConfiguration() {
        CorsConfigurationSourceImpl source = new CorsConfigurationSourceImpl();

        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest());

        assertSame(config, source.getCorsConfiguration(new MockHttpServletRequest()));
        assertEquals(List.of("*"), config.getAllowedOrigins());
        assertEquals(List.of("Origin", "X-Requested-With", "Content-Type", "Accept", "Authorization"), config.getAllowedHeaders());
        assertTrue(config.getAllowedMethods().containsAll(List.of("GET", "HEAD", "POST", "PUT", "OPTIONS", "DELETE", "PATCH")));
        assertEquals(7, config.getAllowedMethods().size());
        assertEquals(1800L, config.getMaxAge());
    }
}
