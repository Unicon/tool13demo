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
package net.unicon.lti.controller.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.lti.dto.DeepLinkJWTDTO;
import net.unicon.lti.model.lti.dto.DeepLinkRequest;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.LtiLinkRepository;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.DeepLinkService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.security.GeneralSecurityException;


/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Controller
@Scope("session")
@RequestMapping("/deeplink")
public class DeepLinkController {

    static final Logger log = LoggerFactory.getLogger(DeepLinkController.class);

    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    APIJWTService apiJWTService;

    @Autowired
    DeepLinkService deepLinkService;

    @Autowired
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    NonceStateRepository nonceStateRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    LtiContextRepository ltiContextRepository;


    @RequestMapping({"", "/toJwt"})
    @ResponseBody
    public DeepLinkJWTDTO deepLinksToJwt(@RequestBody DeepLinkRequest deeplinksRequested) throws ConnectionException, GeneralSecurityException, IOException {
        //Validate state and token again...

        NonceState nonceState = nonceStateRepository.findByNonce(deeplinksRequested.getNonce());
        Jws< Claims> stateClaims = ltijwtService.validateState(nonceState.getState());
        if (!nonceState.getStateHash().equals(deeplinksRequested.getState_hash())){
            //TODO improve this error
            return new DeepLinkJWTDTO(null,null);
        }
        Jws< Claims> idToken = ltijwtService.validateJWT(deeplinksRequested.getId_token(), stateClaims.getBody().get("clientId", String.class));
        log.info(String.join(",", deeplinksRequested.getSelectedIds()));
        DeepLinkJWTDTO JSONdeeplink = deepLinkService.generateDeepLinkJWT(deeplinksRequested.getSelectedIds(), idToken);

        return JSONdeeplink;
    }

}
