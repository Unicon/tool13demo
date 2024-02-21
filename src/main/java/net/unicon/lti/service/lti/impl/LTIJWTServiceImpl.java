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
package net.unicon.lti.service.lti.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class LTIJWTServiceImpl implements LTIJWTService {

    static final Logger log = LoggerFactory.getLogger(LTIJWTServiceImpl.class);

    @Autowired
    LTIDataService ltiDataService;

    String error;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateState(String state) {
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
        }).build().parseSignedClaims(state);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }

    /**
     * This will check that the nonce state token has been signed by us .
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateNonceState(String nonceStateToken) {
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
        }).build().parseSignedClaims(nonceStateToken);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }


    /**
     * We will just check that it is a valid signed JWT from the issuer. The logic later will decide if we
     * want to do what is asking or not. I'm not checking permissions here, that will happen later.
     * We could do other checks here, like comparing some values with the state
     * that just make us sure about the JWT being valid...
     */
    @Override
    public Jws<Claims> validateJWT(String jwt, String clientId) {

        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                PlatformDeployment platformDeployment;
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientId(claims.getIssuer(), clientId).get(0);
                } catch (IndexOutOfBoundsException ex) {
                    log.error("Kid not found in header", ex);
                    return null;
                }
                // If the platform has a JWK Set endpoint... we try that.
                if (StringUtils.isNoneEmpty(platformDeployment.getJwksEndpoint())) {
                    try {
                        JWKSet publicKeys = JWKSet.load(new URL(platformDeployment.getJwksEndpoint()));
                        JWK jwk = publicKeys.getKeyByKeyId(header.getKeyId());
                        return ((AsymmetricJWK) jwk).toPublicKey();
                    } catch (JOSEException | ParseException | IOException ex) {
                        log.error("Error getting the iss public key", ex);
                        return null;
                    } catch (NullPointerException ex) {
                        log.error("Kid not found in header", ex);
                        return null;
                    }
                } else { // If not, we get the key stored in our configuration
                    log.error("The platform configuration must contain a valid JWKS");
                    return null;
                }

            }
        }).build().parseSignedClaims(jwt);
    }

    /**
     * This JWT will contain the token request
     */
    @Override
    public String generateStateOrClientAssertionJWT(PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException {

        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String aud;
        //D2L needs a different aud, maybe others too
        if (!StringUtils.isEmpty(platformDeployment.getoAuth2TokenAud())) {
            aud = platformDeployment.getoAuth2TokenAud();
        } else {
            aud = platformDeployment.getoAuth2TokenUrl();
        }
        String state = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)
                .setHeaderParam("typ", "JWT")
                .setIssuer(platformDeployment.getClientId())  // D2L needs the issuer to be the clientId
                .setSubject(platformDeployment.getClientId()) // The clientId
                .setAudience(aud)  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("jti", UUID.randomUUID().toString())  //This is an specific claim to ask for tokens.
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it with our own private key. The platform has the public one.
                .compact();
        log.debug("Client Assertion JWT/State: \n {} \n", state);
        return state;
    }

    /**
     * This JWT will contain the hash for the nonce state check
     */
    @Override
    public String generateStateNonceTokenJWT(String hash) throws GeneralSecurityException, IOException {

        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        String ourOwnTool = "Our own tool";
        String state = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)
                .setHeaderParam("typ", "JWT")
                .setIssuer(ourOwnTool)
                .setSubject(ourOwnTool) // The clientId
                .setAudience(ourOwnTool)  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("expected_hash", hash)  //This is an specific claim to ask for tokens.
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it with our own private key. The platform has the public one.
                .compact();
        log.debug("JWT State Nonce Hash: \n {} \n", state);
        return state;
    }

}
