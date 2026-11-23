package net.unicon.lti.config;

import net.unicon.lti.utils.SameSiteCookieValve;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomcatConfigurationTest {

    private StandardContext customizeContext(String sessionDomain) {
        TomcatConfiguration configuration = new TomcatConfiguration();
        configuration.sessionDomain = sessionDomain;
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        configuration.cookieProcessorCustomizer().customize(factory);

        assertEquals(1, factory.getContextValves().size());
        assertInstanceOf(SameSiteCookieValve.class, factory.getContextValves().iterator().next());
        assertEquals(1, factory.getContextCustomizers().size());
        TomcatContextCustomizer contextCustomizer = factory.getContextCustomizers().iterator().next();
        StandardContext context = new StandardContext();
        contextCustomizer.customize(context);
        return context;
    }

    @Test
    public void testInstallsPartitionedSameSiteNoneCookies() {
        StandardContext context = customizeContext("https://tool.example.com:8443/app");

        Rfc6265CookieProcessor processor = assertInstanceOf(Rfc6265CookieProcessor.class, context.getCookieProcessor());
        assertEquals(SameSiteCookies.NONE, processor.getSameSiteCookies());
        assertTrue(processor.getPartitioned());
        assertTrue(context.getUsePartitioned());
        assertEquals("tool.example.com", context.getSessionCookieDomain());
    }

    @Test
    public void testInvalidUrlLeavesTheSessionDomainUnset() {
        StandardContext context = customizeContext("http://bad host");

        assertNull(context.getSessionCookieDomain());
        assertInstanceOf(Rfc6265CookieProcessor.class, context.getCookieProcessor());
    }
}
