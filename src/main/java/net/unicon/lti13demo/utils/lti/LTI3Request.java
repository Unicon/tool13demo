/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.utils.lti;

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
import net.unicon.lti13demo.config.ApplicationConfig;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.LtiContextEntity;
import net.unicon.lti13demo.model.LtiLinkEntity;
import net.unicon.lti13demo.model.LtiMembershipEntity;
import net.unicon.lti13demo.model.LtiResultEntity;
import net.unicon.lti13demo.model.LtiUserEntity;
import net.unicon.lti13demo.model.RSAKeyId;
import net.unicon.lti13demo.service.LTIDataService;
import net.unicon.lti13demo.utils.oauth.OAuthUtils;
import net.unicon.lti13demo.utils.LtiStrings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.util.ListUtils;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LTI3 Request object holds all the details for a valid LTI3 request
 *
 * This is generally the only class that a developer will need to interact with but it will
 * only be available during incoming LTI3 requests (launches, etc.). Once the tool application
 * takes over and is servicing the requests on its own path this will no longer be available.
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
public class LTI3Request {

    static final Logger log = LoggerFactory.getLogger(LTI3Request.class);

    HttpServletRequest httpServletRequest;
    LTIDataService ltiDataService;

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

    String iss;
    String aud;
    Date iat;
    Date exp;
    String sub;
    String kid;

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

    String ltiTargetLinkUrl;


    /**
     * @return the current LTI3Request object if there is one available, null if there isn't one and this is not a valid LTI3 based request
     */
    public static synchronized LTI3Request getInstance() {
        LTI3Request ltiRequest = null;
        try {
            ltiRequest = getInstanceOrDie();
        } catch (Exception e) {
            //Nothing to do here
        }
        return ltiRequest;
    }

    /**
     * @return the current LTI3Request object if there is one available
     * @throws IllegalStateException if the LTI3Request cannot be obtained
     */
    public static LTI3Request getInstanceOrDie() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest req = sra.getRequest();
        if (req == null) {
            throw new IllegalStateException("No HttpServletRequest can be found, cannot get the LTIRequest unless we are currently in a request");
        }
        LTI3Request ltiRequest = (LTI3Request) req.getAttribute(LTI3Request.class.getName());
        if (ltiRequest == null) {
            log.debug("No LTIRequest found, attempting to create one for the current request");
            LTIDataService ltiDataService = null;
            try {
                ltiDataService = ApplicationConfig.getContext().getBean(LTIDataService.class);
            } catch (Exception e) {
                log.warn("Unable to get the LTIDataService, initializing the LTIRequest without it");
            }
            try {
                if (ltiDataService != null) {
                    ltiRequest = new LTI3Request(req, ltiDataService, true);
                } else { //THIS SHOULD NOT HAPPEN
                    throw new IllegalStateException("Error internal, no Dataservice available: " + req);
                }
            } catch (Exception e) {
                log.warn("Failure trying to create the LTIRequest: {}" , e);
            }
        }
        if (ltiRequest == null) {
            throw new IllegalStateException("Invalid LTI request, cannot create LTIRequest from request: " + req);
        }
        return ltiRequest;
    }

    /**
     * @param request an http servlet request
     * @param ltiDataService   the service used for accessing LTI data
     * @param update  if true then update (or insert) the DB records for this request (else skip DB updating)
     * @throws IllegalStateException if this is not an LTI request
     */
    public LTI3Request(HttpServletRequest request, LTIDataService ltiDataService, boolean update) {
        if (request == null) throw new AssertionError("cannot make an LtiRequest without a request");
        if (ltiDataService == null) throw new AssertionError("LTIDataService cannot be null");
        this.ltiDataService = ltiDataService;
        this.httpServletRequest = request;
        // extract the typical LTI data from the request
        String jwt = httpServletRequest.getParameter("id_token");
        JwtParser parser = Jwts.parser();
        parser.setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    PlatformDeployment platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByPlatformKid(claims.getAudience()).get(0);

                    if (platformDeployment.getJwksEndpoint() != null) {
                        try {
                            JWKSet publicKeys = JWKSet.load(new URL(platformDeployment.getJwksEndpoint()));
                            JWK jwk = publicKeys.getKeyByKeyId(header.getKeyId());
                            PublicKey key = ((AsymmetricJWK) jwk).toPublicKey();
                            return key;
                        } catch (JOSEException ex) {
                            log.error("Error getting the iss public key", ex);
                            return null;
                        } catch (ParseException | IOException ex) {
                            log.error("Error getting the iss public key", ex);
                            return null;
                        }
                    } else {
                        return OAuthUtils.loadPublicKey(ltiDataService.getRepos().rsaKeys.findById(new RSAKeyId(platformDeployment.getPlatformKid(), false)).get().getPublicKey());
                    }
                } catch (GeneralSecurityException ex) {
                    log.error("Error generating the tool public key", ex);
                    return null;
                }
            }
        });
        Jws<Claims> jws = parser.parseClaimsJws(jwt);
        Enumeration<String> sessionAtributes = httpServletRequest.getSession().getAttributeNames();
        log.info("----------------------BEFORE---------------------------------------------------------------------------------");
        while (sessionAtributes.hasMoreElements()) {
            String attName = sessionAtributes.nextElement();
            log.info(attName  + " : " + httpServletRequest.getSession().getAttribute(attName));

        }
        log.info("-------------------------------------------------------------------------------------------------------");


        String checkNonce = checkNonce(jws);
        if (!checkNonce.equals("true")) {
            throw new IllegalStateException("Nonce error: " + checkNonce);
        }
        String isLTI3Request = isLTI3Request(jws);
        if (!isLTI3Request.equals("true")) {
            throw new IllegalStateException("Request is not a valid LTI3 request: " + isLTI3Request);
        }
        String processRequestParameters = processRequestParameters(request,jws);
        if (!processRequestParameters.equals("true")){
            throw new IllegalStateException("Request is not a valid LTI3 request: " + processRequestParameters);
        }

        //Load data from DB related with this request and update it if needed with the new values.
        ltiDataService.loadLTIDataFromDB(this);
        if (update) {
            ltiDataService.updateLTIDataInDB(this);
        }
    }

    /**
     * Processes all the parameters in this request into populated internal variables in the LTI Request
     *
     * @param request an http servlet request
     * @return true if this is a complete and correct LTI request (includes key, context, link, user) OR false otherwise
     */

    public String processRequestParameters(HttpServletRequest request, Jws<Claims> jws) {

        if (request != null && this.httpServletRequest != request) {
            this.httpServletRequest = request;
        }
        assert this.httpServletRequest != null;

        //First we get all the possible values, and we set null in the ones empty.
        // Later we will review those values to check if the request is valid or not.

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


        iss = jws.getBody().getIssuer();
        aud = jws.getBody().getAudience();
        iat = jws.getBody().getIssuedAt();
        exp = jws.getBody().getExpiration();
        sub = jws.getBody().getSubject();
        nonce = getStringFromLTIRequest(jws, LtiStrings.LTI_NONCE);

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

        // A sample that shows how we can store some of this in the session
        HttpSession session = this.httpServletRequest.getSession();
        session.setAttribute(LtiStrings.LTI_SESSION_USER_ID, aud);
        session.setAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID, ltiContextId);

        //TODO, surely we need a more elaborated code here based in the huge amount of roles avaliable.
        //In any case, this is for the session... we still have the full list of roles in the ltiRoles list
        String normalizedRoleName = LtiStrings.LTI_ROLE_GENERAL;
        if (isRoleAdministrator()) {
            normalizedRoleName = LtiStrings.LTI_ROLE_ADMIN;
        } else if (isRoleInstructor()) {
            normalizedRoleName = LtiStrings.LTI_ROLE_INSTRUCTOR;
        } else if (isRoleLearner()) {
            normalizedRoleName = LtiStrings.LTI_ROLE_LEARNER;
        }
        session.setAttribute(LtiStrings.LTI_SESSION_USER_ROLE, normalizedRoleName);

        String isComplete = checkCompleteLTIRequest();
        complete = (isComplete.equals("true"));
        String isCorrect = checkCorrectLTIRequest();
        correct = (isCorrect.equals("true"));
        // This is a surely bad way to display the error... can be improved.
        if (complete && correct) {
            return "true";
        } else {
            if (complete) {
                isComplete = "";
            } else if (correct) {
                isCorrect = "";
            }
            return isComplete + isCorrect;
        }
    }

    private String getStringFromLTIRequest(Jws<Claims> jws, String stringToGet) {
        if (jws.getBody().containsKey(stringToGet) && jws.getBody().get(stringToGet)!=null) {
            return jws.getBody().get(stringToGet, String.class);
        } else {
            return null;
        }
    }

    private String getStringFromLTIRequestMap(Map map, String stringToGet) {
        if (map.containsKey(stringToGet) && map.get(stringToGet)!=null) {
            return map.get(stringToGet).toString();
        } else {
            return null;
        }
    }

    private Integer getIntegerFromLTIRequestMap(Map map, String integerToGet) {
        if (map.containsKey(integerToGet)) {
            try {
                return Integer.valueOf(map.get(integerToGet).toString());
            }catch (Exception ex) {
                log.error("No integer when expected in: {0}. Returning null", integerToGet);
                return null;
            }
        } else {
            return null;
        }
    }

    private List<String> getListFromLTIRequestMap(Map map, String listToGet) {
        if (map.containsKey(listToGet)) {
            try {
                return (List)map.get(listToGet);
            }catch (Exception ex) {
                log.error("No list when expected in: {0} Returning null", listToGet);
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }

    private Map<String,Object> getMapFromLTIRequest(Jws<Claims> jws, String mapToGet) {
        if (jws.getBody().containsKey(mapToGet)) {
            try {
                return jws.getBody().get(mapToGet, Map.class);
            }catch (Exception ex) {
                log.error("No map integer when expected in: {0}. Returning null", mapToGet);
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
            }catch (Exception ex) {
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

        String completStr = "";

        if (StringUtils.isEmpty(ltiDeploymentId)) {
            completStr += " Lti Deployment Id is empty.\n ";
        }
        if (ltiResourceLink == null || ltiResourceLink.size() == 0) {
            completStr += " Lti Resource Link is empty.\n ";
        } else {
            if (StringUtils.isEmpty(ltiLinkId)) {
                completStr += " Lti Resource Link ID is empty.\n ";
            }
        }
        if (StringUtils.isEmpty(sub)) {
            completStr += " User (sub) is empty.\n ";
        }
        if (ltiRoles == null || ListUtils.isEmpty(ltiRoles)) {
            completStr += " Lti Roles is empty.\n ";
        }
        if (exp == null ){
            completStr += " Exp is empty or invalid.\n ";
        }
        if (iat == null ){
            completStr += " Iat is empty or invalid.\n ";
        }

        if (completStr.equals("")) {
            return "true";
        } else {
            return completStr;
        }
    }

    /**
     * Checks if this LTI3 request object has correct values
     *
     * @return the string "true" if complete and the error message if not
     */
    //TODO update this to check the really complete conditions...!

    private String checkCorrectLTIRequest() {

        String correctStr = "true";


        //TODO check things as:
        // Roles are correct roles
        //

        return correctStr;
    }

    /**
     * @param jws the JWT token parsed.
     * @return true if this is a valid LTI request
     */
    public String checkNonce(Jws<Claims> jws) {


        Enumeration<String> sessionAtributes = httpServletRequest.getSession().getAttributeNames();
        List<String> ltiNonce = (List)httpServletRequest.getSession().getAttribute("lti_nonce");
        List<String> ltiNonceNew = new ArrayList<>();
        Boolean found = false;
        String nonce = jws.getBody().get(LtiStrings.LTI_NONCE,String.class);
        if (nonce == null || ListUtils.isEmpty(ltiNonce)) {
            return "Nonce = null in the JWT or in the session.";
        } else {

            for (String nonceStored:ltiNonce) {
                String nonceHash = Hashing.sha256()
                        .hashString(nonceStored, StandardCharsets.UTF_8)
                        .toString();
                if (nonce.equals(nonceHash)) {
                    found = true;
                } else {
                    ltiNonceNew.add(nonceStored);
                }
            }
            if (found) {
                httpServletRequest.getSession().setAttribute("lti_nonce",ltiNonceNew);
                return "true";
            }else {
                return "Unknown or already used nounce.";
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
        String ltiVersion = jws.getBody().get(LtiStrings.LTI_VERSION,String.class);
        if (ltiVersion == null) {errorDetail = "LTI Version = null. ";}
        String ltiMessageType = jws.getBody().get(LtiStrings.LTI_MESSAGE_TYPE,String.class);
        if (ltiMessageType == null) {errorDetail += "LTI Message Type = null. ";}
            if (ltiMessageType != null && ltiVersion != null) {
            boolean goodMessageType = LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK.equals(ltiMessageType);
            if (!goodMessageType) {errorDetail = "LTI Message Type is not right: " + ltiMessageType + ". ";}
            boolean goodLTIVersion = LtiStrings.LTI_VERSION_3.equals(ltiVersion);
            if (!goodLTIVersion) {errorDetail += "LTI Version is not right: " + ltiVersion;}
            valid = goodMessageType && goodLTIVersion;
        }
        if (valid) {
            return "true";
        }else {
            return errorDetail;
        }
    }


    public boolean isRoleAdministrator() {
        return (ltiRoles != null && userRoleNumber >= 2);
    }

    public boolean isRoleInstructor() {
        return (ltiRoles != null && userRoleNumber >= 1);
    }

    public boolean isRoleLearner() {
        return (ltiRoles != null && ltiRoles.contains(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER));
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

    // GETTERS



    public LtiContextEntity getContext() {
        return context;
    }

    public LtiLinkEntity getLink() {
        return link;
    }

    public LtiMembershipEntity getMembership() {
        return membership;
    }

    public LtiUserEntity getUser() {
        return user;
    }

    public LtiResultEntity getResult() {
        return result;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public int getLoadingUpdates() {
        return loadingUpdates;
    }

    public void setLoadingUpdates(int loadingUpdates) {
        this.loadingUpdates = loadingUpdates;
    }

    public String getLtiMessageType() {
        return ltiMessageType;
    }

    public String getLtiVersion() {
        return ltiVersion;
    }

    public String getLtiGivenName() {
        return ltiGivenName;
    }

    public String getLtiFamilyName() {
        return ltiFamilyName;
    }

    public String getLtiMiddleName() {
        return ltiMiddleName;
    }

    public String getLtiPicture() {
        return ltiPicture;
    }

    public String getLtiEmail() {
        return ltiEmail;
    }

    public void setLtiEmail(String ltiEmail) {
        this.ltiEmail = ltiEmail;
    }

    public String getLtiName() {
        return ltiName;
    }

    public void setLtiName(String ltiName) {
        this.ltiName = ltiName;
    }

    public List<String> getLtiRoles() {
        return ltiRoles;
    }

    public List<String> getLtiRoleScopeMentor() {
        return ltiRoleScopeMentor;
    }

    public Map<String, Object> getLtiResourceLink() {
        return ltiResourceLink;
    }

    public String getLtiLinkId() {
        return ltiLinkId;
    }

    public void setLtiLinkId(String ltiLinkId) {
        this.ltiLinkId = ltiLinkId;
    }

    public String getLtiLinkTitle() {
        return ltiLinkTitle;
    }

    public String getLtiLinkDescription() {
        return ltiLinkDescription;
    }

    public Map<String, Object> getLtiContext() {
        return ltiContext;
    }

    public String getLtiContextId() {
        return ltiContextId;
    }

    public void setLtiContextId(String ltiContextId) {
        this.ltiContextId = ltiContextId;
    }

    public String getLtiContextTitle() {
        return ltiContextTitle;
    }

    public String getLtiContextLabel() {
        return ltiContextLabel;
    }

    public List<String> getLtiContextType() {
        return ltiContextType;
    }

    public Map<String, Object> getLtiToolPlatform() {
        return ltiToolPlatform;
    }

    public String getLtiToolPlatformName() {
        return ltiToolPlatformName;
    }

    public String getLtiToolPlatformContactEmail() {
        return ltiToolPlatformContactEmail;
    }

    public String getLtiToolPlatformDesc() {
        return ltiToolPlatformDesc;
    }

    public String getLtiToolPlatformUrl() {
        return ltiToolPlatformUrl;
    }

    public String getLtiToolPlatformProduct() {
        return ltiToolPlatformProduct;
    }

    public String getLtiToolPlatformFamilyCode() {
        return ltiToolPlatformFamilyCode;
    }

    public String getLtiToolPlatformVersion() {
        return ltiToolPlatformVersion;
    }

    public Map<String, Object> getLtiEndpoint() {
        return ltiEndpoint;
    }

    public List<String> getLtiEndpointScope() {
        return ltiEndpointScope;
    }

    public String getLtiEndpointLineItems() {
        return ltiEndpointLineItems;
    }

    public Map<String, Object> getLtiNamesRoleService() {
        return ltiNamesRoleService;
    }

    public String getLtiNamesRoleServiceContextMembershipsUrl() {
        return ltiNamesRoleServiceContextMembershipsUrl;
    }

    public List<String> getLtiNamesRoleServiceVersions() {
        return ltiNamesRoleServiceVersions;
    }

    public Map<String, Object> getLtiCaliperEndpointService() {
        return ltiCaliperEndpointService;
    }

    public List<String> getLtiCaliperEndpointServiceScopes() {
        return ltiCaliperEndpointServiceScopes;
    }

    public String getLtiCaliperEndpointServiceUrl() {
        return ltiCaliperEndpointServiceUrl;
    }

    public String getLtiCaliperEndpointServiceSessionId() {
        return ltiCaliperEndpointServiceSessionId;
    }

    public String getIss() {
        return iss;
    }

    public String getAud() {
        return aud;
    }

    public Date getIat() {
        return iat;
    }

    public Date getExp() {
        return exp;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getLti11LegacyUserId() {
        return lti11LegacyUserId;
    }

    public String getNonce() {
        return nonce;
    }

    public String getLocale() {
        return locale;
    }

    public Map<String, Object> getLtiLaunchPresentation() {
        return ltiLaunchPresentation;
    }

    public String getLtiPresTarget() {
        return ltiPresTarget;
    }

    public int getLtiPresWidth() {
        return ltiPresWidth;
    }

    public int getLtiPresHeight() {
        return ltiPresHeight;
    }

    public String getLtiPresReturnUrl() {
        return ltiPresReturnUrl;
    }

    public Locale getLtiPresLocale() {
        return ltiPresLocale;
    }

    public Map<String, Object> getLtiExtension() {
        return ltiExtension;
    }

    public Map<String, Object> getLtiCustom() {
        return ltiCustom;
    }

    public String getLtiTargetLinkUrl() {
        return ltiTargetLinkUrl;
    }

    public String getLtiDeploymentId() {
        return ltiDeploymentId;
    }

    public void setLtiDeploymentId(String ltiDeploymentId) {
        this.ltiDeploymentId = ltiDeploymentId;
    }

    public PlatformDeployment getKey() {
        return key;
    }

    public void setKey(PlatformDeployment key) {
        this.key = key;
    }

    public void setContext(LtiContextEntity context) {
        this.context = context;
    }

    public void setLink(LtiLinkEntity link) {
        this.link = link;
    }

    public void setMembership(LtiMembershipEntity membership) {
        this.membership = membership;
    }

    public void setUser(LtiUserEntity user) {
        this.user = user;
    }

    public void setResult(LtiResultEntity result) {
        this.result = result;
    }

    public int getUserRoleNumber() {
        return userRoleNumber;
    }

    public void setUserRoleNumber(int userRoleNumber) {
        this.userRoleNumber = userRoleNumber;
    }
}
