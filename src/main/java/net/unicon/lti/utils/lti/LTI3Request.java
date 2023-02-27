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

import com.google.common.hash.Hashing;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.config.ApplicationConfig;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiLinkEntity;
import net.unicon.lti.model.LtiMembershipEntity;
import net.unicon.lti.model.LtiResultEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.impl.LTIDataServiceImpl;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.util.ListUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static net.unicon.lti.utils.TextConstants.LTI_NONCE_COOKIE_NAME;

/**
 * LTI3 Request object holds all the details for a valid LTI3 request
 *
 *
 * Obtain this class using the static instance methods like so (recommended):
 * LTI3Request lti3Request = LTI3Request.getInstanceOrDie();
 *
 * Or by retrieving it from the HttpServletRequest attributes like so (best to not do this):
 * LTI3Request lti3Request = (LTI3Request) req.getAttribute(LTI3Request.class.getName());
 *
 * Devs may also need to use the LTIDataService service (injected) to access data when there is no
 * LTI request active.
 *
 * The main LTI data will also be placed into the Session and the Principal under the
 * LTI_USER_ID, LTI_CONTEXT_ID, and LTI_ROLE_ID constant keys.
 *
 */
@SuppressWarnings("ConstantConditions")
@Data
@Slf4j
public class LTI3Request implements ApplicationContextAware {
    HttpServletRequest httpServletRequest;
    LTIDataService ltiDataService;
    ApplicationContext applicationContext;

    Jws<Claims> jws;

    // these are populated by the loadLTIDataFromDB operation
    PlatformDeployment key;
    LtiContextEntity context;
    LtiLinkEntity link;
    LtiMembershipEntity membership;
    LtiUserEntity user;
    LtiResultEntity result;
    boolean loaded = false;
    boolean complete = false;
    boolean correct = false;
    boolean updated = false;
    int loadingUpdates = 0;

    // these are populated on construct

    Claims claims;

    String iss;
    String aud;
    Date iat;
    Date exp;
    String sub;
    String kid;
    String azp;

    String ltiMessageType;
    String ltiVersion;
    String ltiDeploymentId;

    String ltiGivenName;
    String ltiFamilyName;
    String ltiMiddleName;
    String ltiPicture;
    String ltiEmail;
    String ltiName;

    List<String> ltiRoles;
    List<String> ltiRoleScopeMentor;
    int userRoleNumber;
    Map<String, Object> ltiResourceLink;
    String ltiLinkId;
    String ltiLinkTitle;
    String ltiLinkDescription;

    Map<String, Object> ltiContext;
    String ltiContextId;
    String ltiContextTitle;
    String ltiContextLabel;
    List<String> ltiContextType;

    Map<String, Object> ltiToolPlatform;
    String ltiToolPlatformGuid;
    String ltiToolPlatformName;
    String ltiToolPlatformContactEmail;
    String ltiToolPlatformDesc;
    String ltiToolPlatformUrl;
    String ltiToolPlatformProduct;
    String ltiToolPlatformFamilyCode;
    String ltiToolPlatformVersion;

    Map<String, Object> ltiEndpoint;
    List<String> ltiEndpointScope;
    String ltiEndpointLineItems;

    Map<String, Object> ltiNamesRoleService;
    String ltiNamesRoleServiceContextMembershipsUrl;
    List<String> ltiNamesRoleServiceVersions;

    Map<String, Object> ltiCaliperEndpointService;
    List<String> ltiCaliperEndpointServiceScopes;
    String ltiCaliperEndpointServiceUrl;
    String ltiCaliperEndpointServiceSessionId;

    String lti11LegacyUserId;

    String nonce;
    String locale;

    Map<String, Object> ltiLaunchPresentation;
    String ltiPresTarget;
    Integer ltiPresWidth;
    Integer ltiPresHeight;
    String ltiPresReturnUrl;
    Locale ltiPresLocale;

    Map<String, Object> ltiExtension;
    Map<String, Object> ltiCustom;

    Map<String, Object> deepLinkingSettings;
    String deepLinkReturnUrl;
    List<String> deepLinkAcceptTypes;
    String deepLinkAcceptMediaTypes;
    List<String> deepLinkAcceptPresentationDocumentTargets;
    String deepLinkAcceptMultiple;
    String deepLinkAutoCreate;
    String deepLinkTitle;
    String deepLinkText;
    String deepLinkData;

    String ltiTargetLinkUrl;

    Map<String, Object> ltiLis;

    //DEEP LINKING RESPONSE (FOR DEMO PURPOSES_
    // We will return some hardcoded JWT's to test the deep Linking LTI Advanced Service standard, but the way this should work
    // is with the tool allowing the user to select the contents to link and generating the JWT with the selection

    Map<String, List<String>> deepLinkJwts;

    private boolean forceNonceCheck = true;

    /**
     * @return the current LTI3Request object if there is one available, null if there isn't one and this is not a valid LTI3 based request
     */
    public static synchronized LTI3Request getInstance(String linkId) {
        LTI3Request ltiRequest = null;
        try {
            ltiRequest = getInstanceOrDie(linkId);
        } catch (Exception e) {
            log.debug("The method getInstanceOrDie... died", e);
        }
        return ltiRequest;
    }

    /**
     * @return the current LTI3Request object if there is one available
     * @throws IllegalStateException if the LTI3Request cannot be obtained
     */
    public static LTI3Request getInstanceOrDie(String linkId) {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            throw new IllegalStateException("No ServletRequestAttributes can be found, cannot get the LTIRequest unless we are currently in a request");
        }
        HttpServletRequest req = sra.getRequest();
        LTI3Request ltiRequest = (LTI3Request) req.getAttribute(LTI3Request.class.getName());
        if (ltiRequest == null) {
            log.debug("No LTIRequest found, attempting to create one for the current request");
            LTIDataService ltiDataService = null;
            try {
                ltiDataService = ApplicationConfig.getContext().getBean(LTIDataServiceImpl.class);
            } catch (Exception e) {
                log.warn("Unable to get the LTIDataService, initializing the LTIRequest without it");
            }
            try {
                if (ltiDataService != null) {
                    ltiRequest = new LTI3Request(req, ltiDataService, true, linkId, null, null);
                } else { //THIS SHOULD NOT HAPPEN
                    throw new IllegalStateException("Error internal, no Dataservice available: " + req);
                }
            } catch (Exception e) {
                log.warn("Failure trying to create the LTIRequest: ", e);
            }
        }
        if (ltiRequest == null) {
            throw new IllegalStateException("Invalid LTI request, cannot create LTIRequest from request: " + req);
        }
        return ltiRequest;
    }

    protected Jws<Claims> validateAndRetrieveJWTClaims(LTIDataService ltiDataService, String jwt) {
        JwtParser parser = Jwts.parser();
        parser.setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {

                // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                PlatformDeployment platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientId(claims.getIssuer(), claims.getAudience()).get(0);

                if (StringUtils.isNoneEmpty(platformDeployment.getJwksEndpoint())) {
                    try {
                        JWKSet publicKeys = JWKSet.load(new URL(platformDeployment.getJwksEndpoint()));
                        JWK jwk = publicKeys.getKeyByKeyId(header.getKeyId());
                        if (jwk == null) {
                            log.debug("LMS JWK does not match. Trying middleware JWK.");
                            return OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());
                        }
                        return ((AsymmetricJWK) jwk).toPublicKey();
                    } catch (JOSEException | ParseException | IOException | GeneralSecurityException ex) {
                        log.error("Error getting the iss public key", ex);
                        return null;
                    }
                } else {
                    log.error("The platform configuration must contain a Jwks endpoint");
                    return null;
                }

            }
        });
        return parser.parseClaimsJws(jwt);
    }

    /**
     * @param request an http servlet request
     * @param ltiDataService   the service used for accessing LTI data
     * @param update  if true then update (or insert) the DB records for this request (else skip DB updating)
     * @param linkId
     * @param jwsClaims
     * @param ltiStorageTarget
     * @throws IllegalStateException if this is not an LTI request
     */
    public LTI3Request(HttpServletRequest request, LTIDataService ltiDataService, boolean update, String linkId, Jws<Claims> jwsClaims, String ltiStorageTarget) throws DataServiceException {
        if (request == null) throw new AssertionError("cannot make an LtiRequest without a request");
        if (ltiDataService == null) throw new AssertionError("LTIDataService cannot be null");
        this.ltiDataService = ltiDataService;
        this.httpServletRequest = request;
        // extract the typical LTI data from the request
        String jwt = httpServletRequest.getParameter("id_token");
        this.jws = jwsClaims != null ? jwsClaims : validateAndRetrieveJWTClaims(ltiDataService, jwt);

        //This is just for logging.
        Enumeration<String> sessionAttributes = httpServletRequest.getSession().getAttributeNames();
        while (log.isDebugEnabled() && sessionAttributes.hasMoreElements()) {
            log.info("----------------------BEFORE---------------------------------------------------------------------------------");
            String attName = sessionAttributes.nextElement();
            log.debug(attName + " : " + httpServletRequest.getSession().getAttribute(attName));
            log.info("-------------------------------------------------------------------------------------------------------");
        }

        validateIdToken(jws, ltiStorageTarget);

        //Here we will populate the LTI3Request object
        processRequestParameters(request, jws);

        // We update the database in case we have new values. (New users, new resources...etc)
        // Load data from DB related with this request and update it if needed with the new values.
        if (update) {
            updateLTIDataInDB(jws, linkId);
        }
    }

    /**
     * @param ltiDataService   the service used for accessing LTI data
     * @param update  if true then update (or insert) the DB records for this request (else skip DB updating)
     * @param jwt id_token string
     */
    public LTI3Request(LTIDataService ltiDataService, boolean update, String linkId, String jwt, String ltiStorageTarget) throws DataServiceException {
        if (ltiDataService == null) throw new AssertionError("LTIDataService cannot be null");
        this.ltiDataService = ltiDataService;
        this.jws = validateAndRetrieveJWTClaims(ltiDataService, jwt);

        validateIdToken(jws, ltiStorageTarget);

        //Here we will populate the LTI3Request object
        processRequestParameters(jws);

        // We update the database in case we have new values. (New users, new resources...etc)
        // Load data from DB related with this request and update it if needed with the new values.
        if (update) {
            updateLTIDataInDB(jws, linkId);
        }
    }

    public LTI3Request(LTIDataService ltiDataService, boolean update, String linkId, String jwt, boolean forceNonceCheck) throws DataServiceException {
        if (ltiDataService == null) throw new AssertionError("LTIDataService cannot be null");
        this.forceNonceCheck = forceNonceCheck;
        this.ltiDataService = ltiDataService;
        this.jws = validateAndRetrieveJWTClaims(ltiDataService, jwt);

        validateIdToken(jws, null);

        //Here we will populate the LTI3Request object
        processRequestParameters(jws);

        // We update the database in case we have new values. (New users, new resources...etc)
        // Load data from DB related with this request and update it if needed with the new values.
        if (update) {
            updateLTIDataInDB(jws, linkId);
        }
    }

    private void validateIdToken(Jws<Claims> jws, String ltiStorageTarget) {
        // Validate deployment
        String iss = jws.getBody().getIssuer();
        String clientId = jws.getBody().getAudience();
        String deploymentId = String.valueOf(jws.getBody().get(LtiStrings.LTI_DEPLOYMENT_ID));
        List<PlatformDeployment> platformDeploymentList = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(iss, clientId, deploymentId);
        if (platformDeploymentList.size() != 1) {
            throw new IllegalStateException("PlatformDeployment does not exist or is duplicated for issuer: " + iss + ", clientId: " + clientId + ", and deploymentId: " + deploymentId);
        }

        // We check that the LTI request is a valid LTI Request and has the right type.
        String isLTI3Request = isLTI3Request(jws);
        if (!(isLTI3Request.equals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK) || isLTI3Request.equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING))) {
            throw new IllegalStateException("Request is not a valid LTI3 request: " + isLTI3Request);
        }

        log.debug("ltiStorageTarget in LTI3Request");
        log.debug(ltiStorageTarget);

        // If forceNonceCheck is true and there is no ltiStorageTarget value then we check the nonce here for the cookie flow
        if (forceNonceCheck && StringUtils.isBlank(ltiStorageTarget)) {
            //Now we are going to check the if the nonce is valid.
            String checkNonce = checkNonce(jws);
            if (!checkNonce.equals("true")) {
                throw new IllegalStateException("Nonce error: " + checkNonce);
            }
        }
    }

    private void updateLTIDataInDB(Jws<Claims> jws, String linkId) throws DataServiceException {
        PlatformDeployment platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(this.iss, this.aud, ltiDeploymentId).get(0);
        ltiDataService.loadLTIDataFromDB(this, linkId);
        if (isLTI3Request(jws).equals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)) {
            ltiDataService.upsertLTIDataInDB(this, platformDeployment, linkId);
        } else {
            ltiDataService.upsertLTIDataInDB(this, platformDeployment, null);
        }
    }

    /**
     * Processes all the parameters in this request into populated internal variables in the LTI Request
     *
     * @param request an http servlet request
     * @return true if this is a complete and correct LTI request (includes key, context, link, user) OR false otherwise
     */
    public void processRequestParameters(HttpServletRequest request, Jws<Claims> jws) throws DataServiceException {
        if (request != null && this.httpServletRequest != request) {
            this.httpServletRequest = request;
        }
        assert this.httpServletRequest != null;

        //First we get all the possible values, and we set null in the ones empty.
        // Later we will review those values to check if the request is valid or not.

        processRequestParameters(jws);

        // A sample that shows how we can store some of this in the session
        HttpSession session = this.httpServletRequest.getSession();
        session.setAttribute(LtiStrings.LTI_SESSION_USER_ID, sub);
        session.setAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID, ltiContextId);
        session.setAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID, ltiContextId);
        try {
            session.setAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY, ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(iss, aud, ltiDeploymentId).get(0).getKeyId());
        } catch (Exception e) {
            log.error("No deployment found");
        }

        // Surely we need a more elaborated code here based in the huge amount of roles available.
        // In any case, this is for the session... we still have the full list of roles in the ltiRoles list

        session.setAttribute(LtiStrings.LTI_SESSION_USER_ROLE, getNormalizedRoleName());
    }

    private void validateLTIClaims() {
        String isComplete;
        String isCorrect;
        if (ltiMessageType.equals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)) {
            isComplete = checkCompleteLTIRequest();
            complete = isComplete.equals("true");
            isCorrect = checkCorrectLTIRequest();
            correct = isCorrect.equals("true");
        } else {  //DEEP Linking
            isComplete = checkCompleteDeepLinkingRequest();
            complete = isComplete.equals("true");
            isCorrect = checkCorrectDeepLinkingRequest();
            correct = isCorrect.equals("true");
            // NOTE: This is just to hardcode some demo information.
            if (ltiDataService.getDemoMode()) {
                try {
                    PlatformDeployment platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(iss, aud, ltiDeploymentId).get(0);
                    deepLinkJwts = DeepLinkUtils.generateDeepLinkDemoJWT(ltiDataService, platformDeployment, this, ltiDataService.getLocalUrl());
                    LtiContextEntity ltiContext = ltiDataService.getRepos().contexts.findByContextKeyAndPlatformDeployment(ltiContextId, platformDeployment);
                    ltiContext.setLineitemsSynced(false);
                    ltiDataService.getRepos().contexts.save(ltiContext);
                } catch (GeneralSecurityException | IOException | NullPointerException ex) {
                    log.error("Error creating the DeepLinking Response", ex);
                }
            }

        }

        if (!(complete && correct)) {
            throw new IllegalStateException("Request is not complete and correct: \n" + isComplete + isCorrect);
        }
    }

    public void processRequestParameters(Jws<Claims> jws) {
        claims = jws.getBody();

        //LTI3 CORE

        iss = jws.getBody().getIssuer();
        aud = jws.getBody().getAudience();
        iat = jws.getBody().getIssuedAt();
        exp = jws.getBody().getExpiration();
        sub = jws.getBody().getSubject();
        nonce = getStringFromLTIRequest(jws, LtiStrings.LTI_NONCE);
        azp = getStringFromLTIRequest(jws, LtiStrings.LTI_AZP);

        ltiMessageType = getStringFromLTIRequest(jws, LtiStrings.LTI_MESSAGE_TYPE);
        ltiVersion = getStringFromLTIRequest(jws, LtiStrings.LTI_VERSION);
        ltiDeploymentId = getStringFromLTIRequest(jws, LtiStrings.LTI_DEPLOYMENT_ID);

        ltiGivenName = getStringFromLTIRequest(jws, LtiStrings.LTI_GIVEN_NAME);
        ltiFamilyName = getStringFromLTIRequest(jws, LtiStrings.LTI_FAMILY_NAME);
        ltiMiddleName = getStringFromLTIRequest(jws, LtiStrings.LTI_MIDDLE_NAME);
        ltiPicture = getStringFromLTIRequest(jws, LtiStrings.LTI_PICTURE);

        ltiEmail = getStringFromLTIRequest(jws, LtiStrings.LTI_EMAIL);
        ltiName = getStringFromLTIRequest(jws, LtiStrings.LTI_NAME);

        ltiRoles = getListFromLTIRequest(jws, LtiStrings.LTI_ROLES);
        userRoleNumber = makeUserRoleNum(ltiRoles);
        ltiRoleScopeMentor = getListFromLTIRequest(jws, LtiStrings.LTI_ROLE_SCOPE_MENTOR);

        ltiResourceLink = getMapFromLTIRequest(jws, LtiStrings.LTI_LINK);
        ltiLinkId = getStringFromLTIRequestMap(ltiResourceLink, LtiStrings.LTI_LINK_ID);
        ltiLinkDescription = getStringFromLTIRequestMap(ltiResourceLink, LtiStrings.LTI_LINK_DESC);
        ltiLinkTitle = getStringFromLTIRequestMap(ltiResourceLink, LtiStrings.LTI_LINK_TITLE);

        ltiContext = getMapFromLTIRequest(jws, LtiStrings.LTI_CONTEXT);
        ltiContextId = getStringFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_ID);
        ltiContextLabel = getStringFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_LABEL);
        ltiContextTitle = getStringFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_TITLE);
        ltiContextType = getListFromLTIRequestMap(ltiContext, LtiStrings.LTI_CONTEXT_TYPE);


        ltiToolPlatform = getMapFromLTIRequest(jws, LtiStrings.LTI_PLATFORM);
        ltiToolPlatformGuid = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_GUID);
        ltiToolPlatformName = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_NAME);
        ltiToolPlatformContactEmail = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_CONTACT_EMAIL);
        ltiToolPlatformDesc = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_DESC);
        ltiToolPlatformUrl = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_URL);
        ltiToolPlatformProduct = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_PRODUCT);
        ltiToolPlatformFamilyCode = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_PRODUCT_FAMILY_CODE);
        ltiToolPlatformVersion = getStringFromLTIRequestMap(ltiToolPlatform, LtiStrings.LTI_PLATFORM_VERSION);


        ltiEndpoint = getMapFromLTIRequest(jws, LtiStrings.LTI_ENDPOINT);
        ltiEndpointScope = getListFromLTIRequestMap(ltiEndpoint, LtiStrings.LTI_ENDPOINT_SCOPE);
        ltiEndpointLineItems = getStringFromLTIRequestMap(ltiEndpoint, LtiStrings.LTI_ENDPOINT_LINEITEMS);


        ltiNamesRoleService = getMapFromLTIRequest(jws, LtiStrings.LTI_NAMES_ROLE_SERVICE);
        ltiNamesRoleServiceContextMembershipsUrl = getStringFromLTIRequestMap(ltiNamesRoleService, LtiStrings.LTI_NAMES_ROLE_SERVICE_CONTEXT);
        ltiNamesRoleServiceVersions = getListFromLTIRequestMap(ltiNamesRoleService, LtiStrings.LTI_NAMES_ROLE_SERVICE_VERSIONS);

        ltiCaliperEndpointService = getMapFromLTIRequest(jws, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE);
        ltiCaliperEndpointServiceScopes = getListFromLTIRequestMap(ltiCaliperEndpointService, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE_SCOPES);
        ltiCaliperEndpointServiceUrl = getStringFromLTIRequestMap(ltiCaliperEndpointService, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE_URL);
        ltiCaliperEndpointServiceSessionId = getStringFromLTIRequestMap(ltiCaliperEndpointService, LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE_SESSION_ID);

        ltiLis = getMapFromLTIRequest(jws, LtiStrings.LTI_LIS);


        lti11LegacyUserId = getStringFromLTIRequest(jws, LtiStrings.LTI_11_LEGACY_USER_ID);

        locale = getStringFromLTIRequest(jws, LtiStrings.LTI_PRES_LOCALE);
        if (locale == null) {
            ltiPresLocale = Locale.getDefault();
        } else {
            ltiPresLocale = Locale.forLanguageTag(locale);
        }

        ltiLaunchPresentation = getMapFromLTIRequest(jws, LtiStrings.LTI_LAUNCH_PRESENTATION);
        ltiPresHeight = getIntegerFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_HEIGHT);
        ltiPresWidth = getIntegerFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_WIDTH);
        ltiPresReturnUrl = getStringFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_RETURN_URL);
        ltiPresTarget = getStringFromLTIRequestMap(ltiLaunchPresentation, LtiStrings.LTI_PRES_TARGET);

        ltiCustom = getMapFromLTIRequest(jws, LtiStrings.LTI_CUSTOM);
        ltiExtension = getMapFromLTIRequest(jws, LtiStrings.LTI_EXTENSION);

        ltiTargetLinkUrl = getStringFromLTIRequest(jws, LtiStrings.LTI_TARGET_LINK_URI);

        //LTI3 DEEP LINKING

        deepLinkingSettings = getMapFromLTIRequest(jws, LtiStrings.DEEP_LINKING_SETTINGS);
        deepLinkReturnUrl = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_RETURN_URL);
        deepLinkAcceptTypes = getListFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_ACCEPT_TYPES);
        deepLinkAcceptMediaTypes = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_ACCEPT_MEDIA_TYPES);
        deepLinkAcceptPresentationDocumentTargets = getListFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_DOCUMENT_TARGETS);
        deepLinkAcceptMultiple = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_ACCEPT_MULTIPLE);
        deepLinkAutoCreate = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_AUTO_CREATE);
        deepLinkTitle = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_TITLE);
        deepLinkText = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_TEXT);
        deepLinkData = getStringFromLTIRequestMap(deepLinkingSettings, LtiStrings.DEEP_LINK_DATA);

        // And now we will check that all the mandatory fields are there and are correct
        validateLTIClaims();
    }

    private String getNormalizedRoleName() {
        String normalizedRoleName = LtiStrings.LTI_ROLE_GENERAL;
        if (isRoleAdministrator()) {
            normalizedRoleName = LtiStrings.LTI_ROLE_ADMIN;
        } else if (isRoleInstructor()) {
            normalizedRoleName = LtiStrings.LTI_ROLE_INSTRUCTOR;
        } else if (isRoleLearner()) {
            normalizedRoleName = LtiStrings.LTI_ROLE_LEARNER;
        }
        return normalizedRoleName;
    }

    private String getStringFromLTIRequest(Jws<Claims> jws, String stringToGet) {
        if (jws.getBody().containsKey(stringToGet) && jws.getBody().get(stringToGet) != null) {
            return jws.getBody().get(stringToGet, String.class);
        } else {
            return null;
        }
    }

    private String getStringFromLTIRequestMap(Map<String, Object> map, String stringToGet) {
        if (map.containsKey(stringToGet) && map.get(stringToGet) != null) {
            return map.get(stringToGet).toString();
        } else {
            return null;
        }
    }

    private Integer getIntegerFromLTIRequestMap(Map<String, Object> map, String integerToGet) {
        if (map.containsKey(integerToGet)) {
            try {
                return Integer.valueOf(map.get(integerToGet).toString());
            } catch (Exception ex) {
                log.error("No integer when expected in: {}. Returning null", integerToGet);
                return null;
            }
        } else {
            return null;
        }
    }

    private List<String> getListFromLTIRequestMap(Map<String, Object> map, String listToGet) {
        if (map.containsKey(listToGet)) {
            try {
                return (List<String>) map.get(listToGet);
            } catch (Exception ex) {
                log.error("No list when expected in: {} Returning null", listToGet);
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> getMapFromLTIRequest(Jws<Claims> jws, String mapToGet) {
        if (jws.getBody().containsKey(mapToGet)) {
            try {
                return jws.getBody().get(mapToGet, Map.class);
            } catch (Exception ex) {
                log.info("No map integer when expected in: {}. Returning null", mapToGet);
                return new HashMap<>();
            }
        } else {
            return new HashMap<>();
        }
    }

    private List<String> getListFromLTIRequest(Jws<Claims> jws, String listToGet) {
        if (jws.getBody().containsKey(listToGet)) {
            try {
                return jws.getBody().get(listToGet, List.class);
            } catch (Exception ex) {
                log.error("No map integer when expected in: " + listToGet + ". Returning null");
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * Checks if this LTI request object has a complete set of required LTI data,
     * also sets the #complete variable appropriately
     *
     * @param objects if true then check for complete objects, else just check for complete request params
     * @return true if complete
     */
    public boolean checkCompleteLTIRequest(boolean objects) {
        return objects && key != null && context != null && link != null && user != null;
    }


    /**
     * Checks if this LTI3 request object has a complete set of required LTI3 data,
     * NOTE: this code is not the one I would create for production, it is more a didactic one
     * to understand what is being checked.
     *
     * @return true if complete
     */

    public String checkCompleteLTIRequest() {

        String completeStr = "";

        if (StringUtils.isEmpty(ltiDeploymentId)) {
            completeStr += " Lti Deployment Id is empty.\n ";
        }
        if (ltiResourceLink == null || ltiResourceLink.size() == 0) {
            completeStr += " Lti Resource Link is empty.\n ";
        } else {
            if (StringUtils.isEmpty(ltiLinkId)) {
                completeStr += " Lti Resource Link ID is empty.\n ";
            }
        }
        if (StringUtils.isEmpty(sub)) {
            completeStr += " User (sub) is empty.\n ";
        }
        if (ltiRoles == null || ListUtils.isEmpty(ltiRoles)) {
            completeStr += " Lti Roles is empty.\n ";
        }
        if (exp == null) {
            completeStr += " Exp is empty or invalid.\n ";
        }
        if (iat == null) {
            completeStr += " Iat is empty or invalid.\n ";
        }

        if (completeStr.equals("")) {
            return "true";
        } else {
            return completeStr;
        }
    }

    /**
     * Checks if this Deep Linking request object has a complete set of required LTI3 data,
     * NOTE: this code is not the one I would create for production, it is more a didactic one
     * to understand what is being checked.
     *
     * @return true if complete
     */

    public String checkCompleteDeepLinkingRequest() {

        String completeStr = "";

        if (StringUtils.isEmpty(ltiDeploymentId)) {
            completeStr += " Lti Deployment Id is empty.\n ";
        }
        if (StringUtils.isEmpty(sub)) {
            completeStr += " User (sub) is empty.\n ";
        }
        if (exp == null) {
            completeStr += " Exp is empty or invalid.\n ";
        }
        if (iat == null) {
            completeStr += " Iat is empty or invalid.\n ";
        }
        if (deepLinkingSettings == null || deepLinkingSettings.isEmpty()) {
            completeStr += " DeepLinkingSettings is empty or invalid.\n ";
        }
        if (StringUtils.isEmpty(deepLinkReturnUrl)) {
            completeStr += " deepLinkReturnUrl is empty.\n ";
        }
        if (deepLinkAcceptTypes == null || deepLinkAcceptTypes.isEmpty()) {
            completeStr += " deepLink AcceptTypes is empty.\n ";
        }
        if (deepLinkAcceptPresentationDocumentTargets == null || deepLinkAcceptPresentationDocumentTargets.isEmpty()) {
            completeStr += " deepLink AcceptPresentationDocumentTargets is empty.\n ";
        }

        if (completeStr.equals("")) {
            return "true";
        } else {
            return completeStr;
        }
    }


    /**
     * Checks if this LTI3 request object has correct values
     *
     * @return the string "true" if complete and the error message if not
     */
    //TODO update this to check the really complete conditions...!
    private String checkCorrectLTIRequest() {


        //TODO check things as:
        // Roles are correct roles
        //

        return "true";
    }

    /**
     * Checks if this Deep Linking request object has correct values
     *
     * @return the string "true" if complete and the error message if not
     */
    //TODO update this to check the really complete conditions...!
    private String checkCorrectDeepLinkingRequest() {


        //TODO check anything needed to check if the request is valid:
        //
        //

        return "true";
    }

    /**
     * @param jws the JWT token parsed.
     * @return true if this is a valid LTI request
     */
    public String checkNonce(Jws<Claims> jws) {
        if (httpServletRequest == null) {
            ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (sra == null) {
                throw new IllegalStateException("No ServletRequestAttributes can be found, cannot validate the request.");
            }
            httpServletRequest = sra.getRequest();
        }

        // We get the nonce from the cookie and compare to the nonce from the id_token JWT.
        Optional<Cookie> ltiNonceCookie = Arrays.stream(httpServletRequest.getCookies())
                .filter(e -> LTI_NONCE_COOKIE_NAME.equals(e.getName())).findAny();
        String jwtNonce = jws.getBody().get(LtiStrings.LTI_NONCE, String.class);
        if (jwtNonce == null || ltiNonceCookie.isEmpty()) {
            return "Nonce = null in the JWT or in the session.";
        } else {
            // Really, we send the hash of the nonce to the platform.
            String nonceStored = ltiNonceCookie.get().getValue();
            String nonceHash = Hashing.sha256().hashString(nonceStored, StandardCharsets.UTF_8).toString();
            if (StringUtils.equals(jwtNonce, nonceHash)) {
                return "true";
            } else {
                return "Unknown or already used nonce.";
            }

        }
    }

    /**
     * @param jws the JWT token parsed.
     * @return true if this is a valid LTI request
     */
    public static String isLTI3Request(Jws<Claims> jws) {

        String errorDetail = "";
        boolean valid = false;
        String ltiVersion = jws.getBody().get(LtiStrings.LTI_VERSION, String.class);
        if (ltiVersion == null) {
            errorDetail = "LTI Version = null. ";
        }
        String ltiMessageType = jws.getBody().get(LtiStrings.LTI_MESSAGE_TYPE, String.class);
        if (ltiMessageType == null) {
            errorDetail += "LTI Message Type = null. ";
        }
        if (ltiMessageType != null && ltiVersion != null) {
            boolean goodMessageType = LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(ltiMessageType) || LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(ltiMessageType);
            if (!goodMessageType) {
                errorDetail = "LTI Message Type is not right: " + ltiMessageType + ". ";
            }
            boolean goodLTIVersion = LtiStrings.LTI_VERSION_3.equals(ltiVersion);
            if (!goodLTIVersion) {
                errorDetail += "LTI Version is not right: " + ltiVersion;
            }
            valid = goodMessageType && goodLTIVersion;
        }
        if (valid && LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(ltiMessageType)) {
            return LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK;
        } else if (valid && LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(ltiMessageType)) {
            return LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING;
        } else {
            return errorDetail;
        }
    }


    public boolean isRoleAdministrator() {
        return ltiRoles != null && userRoleNumber >= 2;
    }

    public boolean isRoleInstructor() {
        return ltiRoles != null && userRoleNumber >= 1;
    }

    public boolean isRoleLearner() {
        return ltiRoles != null && ltiRoles.contains(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER);
    }


    /**
     * @param rawUserRoles the raw roles string (this could also only be part of the string assuming it is the highest one)
     * @return the number that represents the role (higher is more access)
     */
    public int makeUserRoleNum(List<String> rawUserRoles) {
        int roleNum = 0;
        if (rawUserRoles != null) {
            if (rawUserRoles.contains(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN)) {
                roleNum = 2;
            } else if (rawUserRoles.contains(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR)) {
                roleNum = 1;
            }
        }
        return roleNum;
    }

    public static LTI3Request makeLTI3Request(LTIDataService ltiDataService, boolean update, String linkId, String jwt, String ltiStorageTarget) throws DataServiceException {
        return new LTI3Request(ltiDataService, update, linkId, jwt, ltiStorageTarget);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
