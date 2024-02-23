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
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.lti.LTI3Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

/**
 * LTI3 Redirect calls will be filtered on this class. We will check if the JWT is valid and then extract all the needed data.
 */
public class LTI3OAuthProviderProcessingFilterAfter extends GenericFilterBean {

    LTIDataService ltiDataService;
    LTIJWTService ltijwtService;

    static final Logger log = LoggerFactory.getLogger(LTI3OAuthProviderProcessingFilterAfter.class);

    /**
     * We need to load the data service to find the iss configurations and extract the keys.
     */
    public LTI3OAuthProviderProcessingFilterAfter(LTIDataService ltiDataService, LTIJWTService ltijwtService) {
        super();
        if (ltiDataService == null) throw new AssertionError();
        this.ltiDataService = ltiDataService;
        if (ltijwtService == null) throw new AssertionError();
        this.ltijwtService = ltijwtService;
    }

    /**
     * We filter all the LTI3 queries received on this endpoint.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException,
            ServletException {

        // We filter all the LTI queries (not the launch) with this filter.
        if (!(servletRequest instanceof HttpServletRequest)) {
            throw new IllegalStateException("LTI request MUST be an HttpServletRequest (cannot only be a ServletRequest)");
        }

        try {

            // This is just for logging.
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            Enumeration<String> sessionAttributes = httpServletRequest.getSession().getAttributeNames();
            log.info("-------------------------------------------------------------------------------------------------------");
            while (sessionAttributes.hasMoreElements()) {
                String attName = sessionAttributes.nextElement();
                log.info(attName + " : " + httpServletRequest.getSession().getAttribute(attName));

            }
            log.info("-------------------------------------------------------------------------------------------------------");

            // First we validate that the state is a good state.

            //First, we make sure that the query has an state, and it is in the database
            String stateHash = httpServletRequest.getParameter("state");
            String expectedStateHash = httpServletRequest.getParameter("expected_state");
            String nonce = httpServletRequest.getParameter("nonce");
            String expectedNonce = httpServletRequest.getParameter("expected_nonce");
            NonceState nonceState = ltiDataService.getRepos().nonceStateRepository.findByStateHash(expectedStateHash);
            if (nonceState == null) {
                throw new IllegalStateException("LTI request doesn't contains the expected state");
            }

            Jws<Claims> stateClaims = null;

            if (httpServletRequest.getParameter("cookies").equals("false")) { //We don't have cookies
                //Check again if the state is valid
                if (!expectedStateHash.equals(stateHash)){
                    throw new IllegalStateException("LTI expected state does not match the retrieved state");
                }
                if (!expectedNonce.equals(nonce)){
                    throw new IllegalStateException("LTI expected nonce does not match the retrieved nonce");
                }
                stateClaims = ltijwtService.validateState(nonceState.getState());
                String nonceForClaims = stateClaims.getBody().get("nonce", String.class);
                if (!expectedNonce.equals(nonceForClaims)){
                    throw new IllegalStateException("LTI expected nonce does not match the nonce in the state");
                }
                //check if nonce is in the database
                if (ltiDataService.getRepos().nonceStateRepository.existsByNonce(nonce)) {
                    NonceState nonceState2 = ltiDataService.getRepos().nonceStateRepository.findByNonce(nonce);
                    if (!nonceState2.getStateHash().equals(stateHash)){
                        throw new IllegalStateException("LTI request doesn't contains the expected state/nonce");
                    }
                }else{
                    throw new IllegalStateException("LTI request doesn't contains a valid nonce");
                }
            } else { //Follow the normal cookie process.
                //Second, as the state is something that we have created, it should be in our list of states.
                List<String> ltiState = (List<String>) httpServletRequest.getSession().getAttribute("lti_state");
                if (!ltiState.contains(stateHash)) {
                    throw new IllegalStateException("LTI request doesn't contains the expected state");
                }
                //Third, we validate the state to be sure that is correct
                stateClaims = ltijwtService.validateState(nonceState.getState());

                // Once we have the state validated we need the key to check the JWT signature from the id_token,
                // and extract all the values in the LTI3Request object.
                // Most of the platforms will provide a JWK repo URL and we will have it stored in configuration,
                // where they store the public keys
                // With that URL and the "kid" in the header of the jwt id_token, we can get the public key too.
                // In our tool we have included a alternative mechanism for those platforms without JWK endpoint
                // The state provides us the way to find that key in our repo. This is not a requirement in LTI, it is just a way to do it that we've implemented, but each one can use the
                // state in a different way.
            }

            if (stateClaims == null) {
                throw new IllegalStateException("LTI state was not processed as expected");
            }

            String jwt = httpServletRequest.getParameter("id_token");
            if (StringUtils.hasText(jwt)) {
                //Now we validate the JWT token
                Jws<Claims> jws = ltijwtService.validateJWT(jwt, stateClaims.getBody().getAudience());
                if (jws != null) {
                    //Here we create and populate the LTI3Request object and we will add it to the httpServletRequest, so the redirect endpoint will have all that information
                    //ready and will be able to use it.
                    LTI3Request lti3Request = new LTI3Request(httpServletRequest, ltiDataService, true, null, null); // IllegalStateException if invalid
                    httpServletRequest.setAttribute("LTI3", true); // indicate this request is an LTI3 one
                    httpServletRequest.setAttribute("lti3_valid", lti3Request.isLoaded() && lti3Request.isComplete()); // is LTI3 request totally valid and complete
                    httpServletRequest.setAttribute("lti3_message_type", lti3Request.getLtiMessageType()); // is LTI3 request totally valid and complete
                    httpServletRequest.setAttribute(LTI3Request.class.getName(), lti3Request); // make the LTI3 data accessible later in the request if needed
                }
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
        } catch (DataServiceException e) {
            log.error("Error in the Data Service", e);
        }
    }

    private void resetAuthenticationAfterRequest() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }


}
