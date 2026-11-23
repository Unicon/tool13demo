package net.unicon.lti.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

public class CorsConfigurationSourceImpl implements CorsConfigurationSource {
    private CorsConfiguration corsConfig = new DefaultCorsConfiguration();

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        return corsConfig;
    }

    class DefaultCorsConfiguration extends CorsConfiguration {
        public DefaultCorsConfiguration() {
            // Set the applyPermitDefaultValues defaults:
            // - Allow all origins.
            // - Allow "simple" methods GET, HEAD and POST.
            // - Allow all headers.
            // - Set max age to 1800 seconds (30 minutes).
            applyPermitDefaultValues();

            setAllowedHeaders(null); // Clear allowed headers
            addAllowedHeader("Origin");
            addAllowedHeader("X-Requested-With");
            addAllowedHeader("Content-Type");
            addAllowedHeader("Accept");
            addAllowedHeader("Authorization");

            addAllowedMethod(HttpMethod.PUT);
            addAllowedMethod(HttpMethod.OPTIONS);
            addAllowedMethod(HttpMethod.DELETE);
            addAllowedMethod(HttpMethod.PATCH);
        }
    }

}