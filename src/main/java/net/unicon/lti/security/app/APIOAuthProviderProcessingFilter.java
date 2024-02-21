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
package net.unicon.lti.security.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * LTI3 Redirect calls will be filtered on this class. We will check if the JWT is valid and then extract all the needed data.
 */
public class APIOAuthProviderProcessingFilter extends GenericFilterBean {

    APIJWTService apiJwtService;
    APIDataService apiDataService;

    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";
    private final boolean allowQueryParam;

    static final Logger log = LoggerFactory.getLogger(APIOAuthProviderProcessingFilter.class);


    public APIOAuthProviderProcessingFilter(APIJWTService apiJwtService, APIDataService apiDataService) {
        this(apiJwtService, apiDataService, false);
    }

    /**
     * We need to load the data service to find the iss configurations and extract the keys.
     */
    public APIOAuthProviderProcessingFilter(APIJWTService apiJwtService, APIDataService apiDataService, boolean allowQueryParam) {
        super();
        this.allowQueryParam = allowQueryParam;
        if (apiJwtService == null) throw new AssertionError();
        this.apiJwtService = apiJwtService;
        if (apiDataService == null) throw new AssertionError();
        this.apiDataService = apiDataService;
    }

    /**
     * We filter all the API queries received on this endpoint.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException,
            ServletException {

        if (!(servletRequest instanceof HttpServletRequest)) {
            throw new IllegalStateException("API requests MUST be an HttpServletRequest (cannot only be a ServletRequest)");
        }

        try {
            String token = extractJwtStringValue((HttpServletRequest) servletRequest);
            if (token == null) {
                log.info("Missing JWT token");
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                //Second, as the state is something that we have created, it should be in our list of states.

                if (StringUtils.hasText(token)) {
                    Jws<Claims> tokenClaims = apiJwtService.validateToken(token);
                    if (tokenClaims != null) {
                        if (!tokenClaims.getBody().getIssuer().equals("ISSUER")) {
                            throw new IllegalStateException("API token is invalid");
                        }
                        //TODO add here any other checks we want to perform.
                        if ((Boolean) tokenClaims.getBody().get("oneUse")) {
                            boolean exists = apiDataService.findAndDeleteOneUseToken(token);
                            if (!exists) {
                                throw new IllegalStateException("OneUse token does not exists or has been already used");
                            }
                        }
                    }
                }
                filterChain.doFilter(servletRequest, servletResponse);
                this.resetAuthenticationAfterRequest();
            }
        } catch (ExpiredJwtException eje) {
            log.info("Security exception for user {} - {}", eje.getClaims().getSubject(), eje.getMessage());
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.debug("Exception " + eje.getMessage(), eje);
        } catch (SignatureException ex) {
            log.info("Invalid JWT signature: {0}", ex.getMessage());
            log.debug("Exception " + ex.getMessage(), ex);
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void resetAuthenticationAfterRequest() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private String extractJwtStringValue(HttpServletRequest request) {
        String rawHeaderValue = StringUtils.trimAllWhitespace(request.getHeader(JWT_REQUEST_HEADER_NAME));
        if (rawHeaderValue == null) {
            if (allowQueryParam) {
                return StringUtils.trimAllWhitespace(request.getParameter(QUERY_PARAM_NAME));
            }
        }
        if (rawHeaderValue == null) {
          return null;
        }
        // very similar to BearerTokenExtractor.java in Spring spring-security-oauth2
        if (isBearerToken(rawHeaderValue)) {
            return rawHeaderValue.substring(JWT_BEARER_TYPE.length()).trim();
        }
        return null;
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase().startsWith(JWT_BEARER_TYPE.toLowerCase());
    }
}