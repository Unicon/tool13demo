/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti.config;

import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.servlet.http.HttpServletRequest;

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