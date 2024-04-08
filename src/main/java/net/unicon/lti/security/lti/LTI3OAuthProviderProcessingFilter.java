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
package net.unicon.lti.security.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class LTI3OAuthProviderProcessingFilter extends GenericFilterBean {

    LTIDataService ltiDataService;
    LTIJWTService ltijwtService;

    static final Logger log = LoggerFactory.getLogger(LTI3OAuthProviderProcessingFilter.class);

    /**
     * We need to load the data service to find the iss configurations and extract the keys.
     */
    public LTI3OAuthProviderProcessingFilter(LTIDataService ltiDataService, LTIJWTService ltijwtService) {
        super();
        if (ltiDataService == null) throw new AssertionError();
        this.ltiDataService = ltiDataService;
        if (ltijwtService == null) throw new AssertionError();
        this.ltijwtService = ltijwtService;
    }

    /**
     * We filter all the LTI3 queries received on this endpoint.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException,
            ServletException {

        // We filter all the LTI queries (not the launch) with this filter.
        if (!(servletRequest instanceof HttpServletRequest)) {
            throw new IllegalStateException("LTI request MUST be an HttpServletRequest (cannot only be a ServletRequest)");
        }

        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String state = httpServletRequest.getParameter("state");

            NonceState nonceState = ltiDataService.getRepos().nonceStateRepository.findByStateHash(state);
            if (nonceState != null) {
                Jws<Claims> stateClaims = ltijwtService.validateState(nonceState.getState());
                String nonce = stateClaims.getBody().getId();
                if (!nonceState.getNonce().equals(nonce)) {
                    throw new IllegalStateException("LTI request doesn't contain the expected state/nonce");
                }
            }else{
                throw new IllegalStateException("LTI request doesn't contain a valid state");
            }
            filterChain.doFilter(servletRequest, servletResponse);
            this.resetAuthenticationAfterRequest();
        } catch (ExpiredJwtException eje) {
            log.info("Security exception for user {} - {}", eje.getClaims().getSubject(), eje.getMessage());
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.debug("Exception " + eje.getMessage(), eje);
        } catch (SignatureException ex) {
            log.info("Invalid JWT signature: {0}", ex.getMessage());
            log.debug("Exception " + ex.getMessage(), ex);
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            log.info("Error filtering the request", e.getMessage());
            log.debug("Exception " + e.getMessage(), e);
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void resetAuthenticationAfterRequest() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }


}
