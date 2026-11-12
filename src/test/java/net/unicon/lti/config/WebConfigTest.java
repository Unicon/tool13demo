package net.unicon.lti.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebConfigTest {

    @Test
    public void testRegistersStaticResourceHandlers() {
        MockServletContext servletContext = new MockServletContext();
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(servletContext), servletContext);

        new WebConfig().addResourceHandlers(registry);

        for (String pattern : new String[]{"/webjars/**", "/img/**", "/files/**", "/css/**", "/js/**", "/app/**"}) {
            assertTrue(registry.hasMappingForPattern(pattern), pattern);
        }
        assertFalse(registry.hasMappingForPattern("/templates/**"));
    }
}
