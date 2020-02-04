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
package net.unicon.lti13demo.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.RSAKeyEntity;
import net.unicon.lti13demo.model.RSAKeyId;
import net.unicon.lti13demo.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Optional;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Component
public class LTIJWTService {

    static final Logger log = LoggerFactory.getLogger(LTIJWTService.class);

    @Autowired
    LTIDataService ltiDataService;

    String error;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     * @param state
     * @return
     */
    //Here we could add other checks like expiration of the state (not implemented)
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
                    RSAKeyId rsaKeyId = new RSAKeyId("OWNKEY", true);
                    Optional<RSAKeyEntity> rsaKeyEntity =  ltiDataService.getRepos().rsaKeys.findById(rsaKeyId);
                    String toolPublicKeyString;
                    if (rsaKeyEntity.isPresent()) {
                        toolPublicKeyString = rsaKeyEntity.get().getPublicKey();
                        toolPublicKey = OAuthUtils.loadPublicKey(toolPublicKeyString);
                    } else {
                        throw new SignatureException("Error validating the state. Error getting the tool public key");
                    }

                } catch (GeneralSecurityException ex){
                    log.error("Error validating the state. Error generating the tool public key",ex);
                    return null;
                }
                return toolPublicKey;
            }
        }).parseClaimsJws(state);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }


    /**
     * We will just check that it is a valid signed JWT from the issuer. The logic later will decide if we
     * want to do what is asking or not. I'm not checking permissions here, that will happen later.
     * We could do other checks here, like comparing some values with the state
     * that just make us sure about the JWT being valid...
     * @param jwt
     * @return
     */
    public Jws<Claims> validateJWT(String jwt, String clientId) {

        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    PlatformDeployment platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByClientId(clientId).get(0);

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
                            log.error("Kid not found in header",ex);
                            return null;
                        }
                    } else { // If not, we get the key stored in our configuration
                        Optional<RSAKeyEntity> rsaKey = ltiDataService.getRepos().rsaKeys.findById(new RSAKeyId(platformDeployment.getPlatformKid(), false));
                        if (rsaKey.isPresent()) {
                           return OAuthUtils.loadPublicKey(rsaKey.get().getPublicKey());
                        } else {
                           log.error("Error retrieving the tool public key");
                           return null;
                        }
                    }
                } catch (GeneralSecurityException ex){
                    log.error("Error generating the tool public key",ex);
                    return null;
                } catch (IndexOutOfBoundsException ex){
                    log.error("Kid not found in header",ex);
                    return null;
                }
            }
        }).parseClaimsJws(jwt);
    }

}
