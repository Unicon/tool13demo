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
package net.unicon.lti.utils.lti;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.oauth.OAuthUtils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

@Slf4j
public final class LtiOidcUtils {

    private LtiOidcUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * The state will be returned when the tool makes the final call to us, so it is useful to send information
     * to our own tool, to know about the request.
     */
    public static String generateState(LTIDataService ltiDataService, Map<String, String> authRequestMap, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException {
        LocalDateTime date = LocalDateTime.now(ZoneId.of("Z"));
        Key issPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String state = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer("ltiMiddleware")  //This is our own identifier, to know that we are the issuer.
                .setSubject(loginInitiationDTO.getIss()) // We store here the platform issuer to check that matches with the issuer received later
                .setAudience(clientIdValue)  //We send here the clientId to check it later.
                .setExpiration(Date.from(date.plusHours(1).toInstant(ZoneOffset.UTC))) //a java.util.Date
                .setNotBefore(Date.from(date.toInstant(ZoneOffset.UTC))) //a java.util.Date
                .setIssuedAt(Date.from(date.toInstant(ZoneOffset.UTC))) // for example, now
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

    public static String generateLtiToken(LTI3Request lti3Request, LTIDataService ltiDataService) throws GeneralSecurityException {
        Key issPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String ltiToken = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setClaims(lti3Request.getClaims())
                .signWith(SignatureAlgorithm.RS256, issPrivateKey)
                .compact();
        log.debug("LTI Token: \n {} \n", ltiToken);
        return ltiToken;
    }

}
