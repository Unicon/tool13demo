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
package net.unicon.lti13demo.utils.lti;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.dto.LoginInitiationDTO;
import net.unicon.lti13demo.service.LTIDataService;
import net.unicon.lti13demo.utils.TextConstants;
import net.unicon.lti13demo.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Date;
import java.util.Map;

public class LtiOidcUtils {

    static final Logger log = LoggerFactory.getLogger(LtiOidcUtils.class);

    private LtiOidcUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * The state will be returned when the tool makes the final call to us, so it is useful to send information
     * to our own tool, to know about the request.
     */
    public static String generateState(LTIDataService ltiDataService, PlatformDeployment platformDeployment, Map<String, String> authRequestMap, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException, IOException {

        Date date = new Date();
        Key issPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String state = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                .setSubject(platformDeployment.getIss()) // We store here the platform issuer to check that matches with the issuer received later
                .setAudience(platformDeployment.getClientId())  //We send here the clientId to check it later.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .setId(authRequestMap.get("nonce")) //just a nonce... we don't use it by the moment, but it could be good if we store information about the requests in DB.
                .claim("original_iss", loginInitiationDTO.getIss())  //All this claims are the information received in the OIDC initiation and some other useful things.
                .claim("loginHint", loginInitiationDTO.getLoginHint())
                .claim("ltiMessageHint", loginInitiationDTO.getLtiMessageHint())
                .claim("targetLinkUri", loginInitiationDTO.getTargetLinkUri())
                .claim("clientId", clientIdValue)
                .claim("ltiDeploymentId", deploymentIdValue)
                .claim("controller", "/oidc/login_initiations")
                .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                .compact();
        log.debug("State: \n {} \n", state);
        return state;
    }

}
