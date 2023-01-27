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

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.unicon.lti.model.GcCourseEntity;
import net.unicon.lti.model.GcLinkEntity;
import net.unicon.lti.model.GcUserEntity;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.unicon.lti.utils.LtiStrings.DEEP_LINKING_SETTINGS;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTEXT;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTEXT_ID;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTEXT_LABEL;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTEXT_TITLE;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTEXT_TYPE;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTEXT_TYPE_COURSE_OFFERING;
import static net.unicon.lti.utils.LtiStrings.LTI_DEPLOYMENT_ID;
import static net.unicon.lti.utils.LtiStrings.LTI_EMAIL;
import static net.unicon.lti.utils.LtiStrings.LTI_FAMILY_NAME;
import static net.unicon.lti.utils.LtiStrings.LTI_GIVEN_NAME;
import static net.unicon.lti.utils.LtiStrings.LTI_LINK_ID;
import static net.unicon.lti.utils.LtiStrings.LTI_LINK_TITLE;
import static net.unicon.lti.utils.LtiStrings.LTI_MESSAGE_TYPE;
import static net.unicon.lti.utils.LtiStrings.LTI_NAME;
import static net.unicon.lti.utils.LtiStrings.LTI_NONCE;
import static net.unicon.lti.utils.LtiStrings.LTI_ROLES;
import static net.unicon.lti.utils.LtiStrings.LTI_TARGET_LINK_URI;
import static net.unicon.lti.utils.LtiStrings.LTI_VERSION;
import static net.unicon.lti.utils.LtiStrings.LTI_VERSION_3;
import static net.unicon.lti.utils.TextConstants.LTI3_SUFFIX;

public class LtiOidcUtils {

    static final Logger log = LoggerFactory.getLogger(LtiOidcUtils.class);

    private LtiOidcUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * The state will be returned when the tool makes the final call to us, so it is useful to send information
     * to our own tool, to know about the request.
     */
    public static String generateState(LTIDataService ltiDataService, Map<String, String> authRequestMap, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException {
        Date date = new Date();
        Key issPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String state = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                .setSubject(loginInitiationDTO.getIss()) // We store here the platform issuer to check that matches with the issuer received later
                .setAudience(clientIdValue)  //We send here the clientId to check it later.
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

    public static String generateLtiMessageHint(LTIDataService ltiDataService, String linkUuid, String link) throws GeneralSecurityException {
        Date date = new Date();
        Key issPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String state = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                .setSubject(ltiDataService.getLocalUrl()) // We store here the platform issuer to check that matches with the issuer received later
                .setAudience("self-client-id")  //We send here the clientId to check it later.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("linkUuid", linkUuid)
                .claim("link", link)
                .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                .compact();
        log.debug("lti_message_hint: \n {} \n", state);
        return state;
    }

    public static String generateLtiIdToken(LTIDataService ltiDataService, String nonce, GcUserEntity gcUserEntity, GcCourseEntity gcCourseEntity, GcLinkEntity gcLinkEntity, boolean deepLinking) throws GeneralSecurityException {
        Date date = new Date();
        Key issPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());

        JSONObject context = new JSONObject();
        context.put(LTI_CONTEXT_ID, gcCourseEntity.getGcCourseId());
        context.put(LTI_CONTEXT_LABEL, gcCourseEntity.getSection());
        context.put(LTI_CONTEXT_TITLE, gcCourseEntity.getName());
        context.put(LTI_CONTEXT_TYPE, Collections.singletonList(LTI_CONTEXT_TYPE_COURSE_OFFERING));

        String baseTargetUri = ltiDataService.getLocalUrl() + LTI3_SUFFIX;

        JwtBuilder jwtBuilder = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer(ltiDataService.getLocalUrl())  //This is our own identifier, to know that we are the issuer.
                .setSubject(gcUserEntity.getGcUserId())
                .setAudience("self-client-id")  //We send here the clientId to check it later.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date)
                .claim(LTI_VERSION, LTI_VERSION_3)
                .claim(LTI_CONTEXT, context.toMap())
                .claim(LTI_DEPLOYMENT_ID, "self-deployment-id")
                .claim(LTI_NONCE, nonce)
                .claim(LTI_ROLES, gcUserEntity.getLtiRoles())
                .claim(LTI_EMAIL, gcUserEntity.getEmail())
                .claim(LTI_NAME, gcUserEntity.getFullName())
                .claim(LTI_GIVEN_NAME, gcUserEntity.getGivenName())
                .claim(LTI_FAMILY_NAME, gcUserEntity.getFamilyName());
        if (!deepLinking) {
            jwtBuilder.claim(LTI_MESSAGE_TYPE, "LtiResourceLinkRequest");
            jwtBuilder.claim(LTI_TARGET_LINK_URI, baseTargetUri + "?link=" + gcLinkEntity.getId());

            JSONObject resourceLink = new JSONObject();
            resourceLink.put(LTI_LINK_ID, gcLinkEntity.getUuid());
            resourceLink.put(LTI_LINK_TITLE, gcLinkEntity.getTitle());
            jwtBuilder.claim("https://purl.imsglobal.org/spec/lti/claim/resource_link", resourceLink.toMap());
        } else {
            Map<String, Object> deepLinkingSettings = new HashMap<>();
            deepLinkingSettings.put("deep_link_return_url", ltiDataService.getLocalUrl() + "/app/gccoursework/" + gcCourseEntity.getGcCourseId());
            List<String> deepLinkingResponseAcceptTypes = new ArrayList<>();
            deepLinkingResponseAcceptTypes.add("ltiResourceLink");
            deepLinkingSettings.put("accept_types", deepLinkingResponseAcceptTypes);
            List<String> deepLinkingAcceptPresentationDocumentTargets = new ArrayList<>();
            deepLinkingAcceptPresentationDocumentTargets.add("iframe");
            deepLinkingAcceptPresentationDocumentTargets.add("window");
            deepLinkingSettings.put("accept_presentation_document_targets", deepLinkingAcceptPresentationDocumentTargets);

            jwtBuilder.claim(LTI_MESSAGE_TYPE, "LtiDeepLinkingRequest");
            jwtBuilder.claim(LTI_TARGET_LINK_URI, baseTargetUri);
            jwtBuilder.claim(DEEP_LINKING_SETTINGS, deepLinkingSettings);
        }

        String ltiToken = jwtBuilder.signWith(SignatureAlgorithm.RS256, issPrivateKey)
                .compact();
        log.debug("Internal LTI id_token: \n {} \n", ltiToken);
        return ltiToken;
    }
}
