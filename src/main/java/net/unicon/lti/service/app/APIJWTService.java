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
package net.unicon.lti.service.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.utils.oauth.OAuthUtils;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Component
public class APIJWTService {

    static final Logger log = LoggerFactory.getLogger(APIJWTService.class);

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
        }).parseClaimsJws(token);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }




    /**
     * This JWT will contain the token request
     */
    public String buildJwt(boolean oneUse) throws GeneralSecurityException, IOException {

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
                .setSubject("define subject") // The clientId
                .setAudience(ltiDataService.getLocalUrl())  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("something", UUID.randomUUID().toString())  //This is an specific claim to ask for tokens.
                .claim("oneUse", oneUse)  //This is an specific claim to ask for tokens.
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String token = builder.compact();
        if (oneUse){
            apiDataService.addOneUseToken(token);
        }
        log.debug("Token Request: \n {} \n", token);
        return token;
    }

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
                .setAudience(tokenClaims.getBody().getAudience())  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("something",tokenClaims.getBody().get("something"))  //This is an specific claim to ask for tokens.
                .claim("oneUse", false)  //This is an specific claim to ask for tokens.
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String newToken = builder.compact();
        log.debug("Token Request: \n {} \n", newToken);
        return newToken;
    }

    public String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam) {
        String rawHeaderValue = StringUtils.trimAllWhitespace(request.getHeader(JWT_REQUEST_HEADER_NAME));
        if (rawHeaderValue == null) {
            if (allowQueryParam) {
                String param = StringUtils.trimAllWhitespace(request.getParameter(QUERY_PARAM_NAME));
                return param;
            }
        }
        if (rawHeaderValue == null) {
            return null;
        }
        // very similar to BearerTokenExtractor.java in Spring spring-security-oauth2
        if (isBearerToken(rawHeaderValue)) {
            String jwtValue = rawHeaderValue.substring(JWT_BEARER_TYPE.length()).trim();
            return jwtValue;
        }
        return null;
    }

    public boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase().startsWith(JWT_BEARER_TYPE.toLowerCase());
    }

}
