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
package net.unicon.lti.service.app.impl;

import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.model.oauth2.Roles;
import net.unicon.lti.model.oauth2.SecuredInfo;
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.utils.lti.LTI3Request;
import net.unicon.lti.utils.oauth.OAuthUtils;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class APIJWTServiceImpl implements APIJWTService {

    static final Logger log = LoggerFactory.getLogger(APIJWTServiceImpl.class);

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    APIDataService apiDataService;

    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";

    String error;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateToken(String token) {
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                PublicKey toolPublicKey;
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    toolPublicKey = OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());
                } catch (GeneralSecurityException ex) {
                    log.error("Error validating the state. Error generating the tool public key", ex);
                    return null;
                }
                return toolPublicKey;
            }
        }).build().parseSignedClaims(token);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }


    @Override
    public Jwt<Header, Claims> unsecureToken(String token){
        int i = token.lastIndexOf('.');
        String withoutSignature = token.substring(0, i+1);
        return Jwts.parser().build().parseUnsecuredClaims(withoutSignature);
    }

    @Override
    public String buildJwt(boolean oneUse,
                           List<String> roles,
                           Long contextId,
                           String contextKey,
                           Long platformDeploymentId,
                           String userId,
                           String nonce
    ) throws GeneralSecurityException, IOException {

        int length = 3600;
        //We only allow 30 seconds (surely we can low that) for the one time token, because that one must be traded
        // immediately
        if (oneUse){
            length = 300; //TODO, change this test value to 30
        }
        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)
                .setHeaderParam("typ", "JWT")
                .setIssuer("ISSUER")
                .setSubject(userId) // The clientId
                .setAudience(ltiDataService.getLocalUrl())  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("contextId", contextId)  //This is an specific claim to ask for tokens.
                .claim("contextKey", contextKey)  //This is an specific claim to ask for tokens.
                .claim("platformDeploymentId", platformDeploymentId)  //This is an specific claim to ask for tokens.
                .claim("userId", userId)  //This is an specific claim to ask for tokens.
                .claim("roles", roles)
                .claim("oneUse", oneUse)  //This is an specific claim to ask for tokens.
                .claim("nonce", nonce)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String token = builder.compact();
        if (oneUse){
            apiDataService.addOneUseToken(token);
        }
        log.debug("Token Request: \n {} \n", token);
        return token;
    }


    /**
     * This JWT will contain the token request
     */
    @Override
    public String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException {

        return buildJwt(oneUse, lti3Request.getLtiRoles(),
                lti3Request.getContext().getContextId(),
                lti3Request.getContext().getContextKey(),
                lti3Request.getKey().getKeyId(),
                lti3Request.getUser().getUserKey(),
                lti3Request.getNonce());

    }

    @Override
    public String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException {
        int length = 3600;
        Jws<Claims> tokenClaims = validateToken(token);
        if (tokenClaims.getBody().get("oneUse").equals("true")){
            throw new BadTokenException("Trying to refresh an one use token");
        }
        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", tokenClaims.getHeader().getKeyId())
                .setHeaderParam("typ", "JWT")
                .setIssuer(tokenClaims.getBody().getIssuer())
                .setSubject(tokenClaims.getBody().getSubject()) // The clientId
                .setAudience(Iterables.getOnlyElement(tokenClaims.getBody().getAudience()))  //We send here the authToken url.
                .setExpiration(DateUtils.addDays(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("contextId", tokenClaims.getBody().get("contextId"))
                .claim("contextKey", tokenClaims.getBody().get("contextKey"))
                .claim("platformDeploymentId", tokenClaims.getBody().get("platformDeploymentId"))
                .claim("userId", tokenClaims.getBody().get("userId"))
                .claim("roles", tokenClaims.getBody().get("roles"))
                .claim("oneUse", false)
                .claim("nonce", tokenClaims.getBody().get("nonce"))
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String newToken = builder.compact();
        log.debug("Token Request: \n {} \n", newToken);
        return newToken;
    }

    @Override
    public String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam) {
        String rawHeaderValue = StringUtils.trimToNull(request.getHeader(JWT_REQUEST_HEADER_NAME));
        if (rawHeaderValue == null) {
            if (allowQueryParam) {
                return StringUtils.trimToNull(request.getParameter(QUERY_PARAM_NAME));
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

    @Override
    public SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam) {
        String token = extractJwtStringValue(request,allowQueryParam);
        Jws<Claims> claims = validateToken(token);
        if (claims != null) {
          SecuredInfo securedInfo = new SecuredInfo();
          securedInfo.setUserId(claims.getBody().get("userId").toString());
          securedInfo.setPlatformDeploymentId(Long.valueOf((Integer) claims.getBody().get("platformDeploymentId")));
          securedInfo.setContextId(Long.valueOf((Integer) claims.getBody().get("contextId")));
          securedInfo.setContextKey(claims.getBody().get("contextKey").toString());
          securedInfo.setRoles((List<String>) claims.getBody().get("roles"));
          securedInfo.setNonce(claims.getBody().get("nonce").toString());
          return securedInfo;
        } else {
          return null;
        }
    }

    private Timestamp extractTimestamp(Jws<Claims> claims, String id){
        Timestamp extracted;
        try {
            extracted = Timestamp.valueOf(LocalDateTime.parse(claims.getBody().get(id).toString()));
        } catch (Exception ex){
            return null;
        }
        return extracted;
    }

    @Override
    public boolean isAdmin(SecuredInfo securedInfo){ return securedInfo.getRoles().contains(Roles.ADMIN); }

    @Override
    public boolean isInstructor(SecuredInfo securedInfo){
        return (securedInfo.getRoles().contains(Roles.INSTRUCTOR) || securedInfo.getRoles().contains(Roles.MEMBERSHIP_INSTRUCTOR));
    }

    @Override
    public boolean isLearner(SecuredInfo securedInfo){
        return (securedInfo.getRoles().contains(Roles.LEARNER) || securedInfo.getRoles().contains(Roles.MEMBERSHIP_LEARNER));
    }

    @Override
    public boolean isGeneral(SecuredInfo securedInfo){
        return securedInfo.getRoles().contains(Roles.GENERAL);
    }

    @Override
    public boolean isInstructorOrHigher(SecuredInfo securedInfo){
        return (isInstructor(securedInfo) || isAdmin(securedInfo));
    }

    @Override
    public boolean isLearnerOrHigher(SecuredInfo securedInfo){
        return (isLearner(securedInfo) || isInstructorOrHigher(securedInfo));
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase().startsWith(JWT_BEARER_TYPE.toLowerCase());
    }

}
