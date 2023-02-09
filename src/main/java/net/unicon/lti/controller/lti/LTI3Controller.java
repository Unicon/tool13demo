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

import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiLinkEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.LtiLinkRepository;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.List;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Controller
@Scope("session")
@RequestMapping("/lti3")
public class LTI3Controller {

    static final Logger log = LoggerFactory.getLogger(LTI3Controller.class);

    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    APIJWTService apiJWTService;

    @Autowired
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    LtiContextRepository ltiContextRepository;

    @RequestMapping({"", "/"})
    public String lti3(HttpServletRequest req, Model model) throws DataServiceException, ConnectionException {

        //We need to pass to the model the LTI3 request, the state and the nonce and link, and a JWT with the hash of all of them.
        String state = req.getParameter("state");
        String id_token = req.getParameter("id_token");
        String link = req.getParameter("link");

        NonceState nonceState = ltiDataService.getRepos().nonceStateRepository.findByStateHash(state);
        if (nonceState != null) {
            Jws<Claims> stateClaims = ltijwtService.validateState(nonceState.getState());
            String nonce = stateClaims.getBody().getId();
            String tohash = id_token + state + nonce + link;
            String expected_hash = Hashing.sha256()
                    .hashString(tohash, StandardCharsets.UTF_8)
                    .toString();
            model.addAttribute("expected_state", state);
            model.addAttribute("expected_nonce", nonce);
            try {
                String token = ltijwtService.generateStateNonceTokenJWT(expected_hash);
                model.addAttribute("token", token);
            } catch (GeneralSecurityException | IOException e) {
                log.error("Error generating JWT", e);
                model.addAttribute(TextConstants.ERROR, "Error generating JWT");
                return TextConstants.LTI3ERROR;
            }
            model.addAttribute("id_token", id_token);
            model.addAttribute("link", link);
            model.addAttribute("ltiStorageTarget", nonceState.getLtiStorageTarget());


            //We need to get the PlatformDeployment based on the iss, clientId and ltiDeploymentId
            String iss = stateClaims.getBody().get("original_iss", String.class);
            String clientId = stateClaims.getBody().get("clientId", String.class);
            String ltiDeploymentId = stateClaims.getBody().get("ltiDeploymentId", String.class);

            List<PlatformDeployment> platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(iss, clientId, ltiDeploymentId);
            if (!platformDeployment.isEmpty()) {
                model.addAttribute("oidc_authorization_uri", platformDeployment.get(0).getOidcEndpoint());
            } else {
                log.error("Error getting PlatformDeployment");
                model.addAttribute(TextConstants.ERROR, "Error getting oidc_authorization_uri from the PlatformDeployment");
                return TextConstants.LTI3ERROR;
            }
        }else{
            model.addAttribute(TextConstants.ERROR, "State was not expected");
            return TextConstants.LTI3ERROR;
        }
        return "nonceStateCheck";
    }

    @RequestMapping({"", "/after"})
    public String lti3checked(HttpServletRequest req, Model model) throws DataServiceException, ConnectionException {
        //We validate the hash.
        String state = req.getParameter("state");
        String nonce = req.getParameter("nonce");
        String token = req.getParameter("token");
        String id_token = req.getParameter("id_token");
        String expected_state = req.getParameter("expected_state");
        String expected_nonce = req.getParameter("expected_nonce");
        String link = req.getParameter("link");

        String tohash = id_token + expected_state + expected_nonce + link;
        String expected_hash = Hashing.sha256()
                .hashString(tohash, StandardCharsets.UTF_8)
                .toString();

        Jws<Claims> claims_token = ltijwtService.validateNonceState(token);

        if (!claims_token.getBody().get("expected_hash").equals(expected_hash)){
            log.error("Hashes don't match");
            model.addAttribute(TextConstants.ERROR, "Token hashes don't match");
            return TextConstants.LTI3ERROR;
        }

        if (!state.equals(expected_hash)){
            log.error("Hashes don't match");
            model.addAttribute(TextConstants.ERROR, "Token hashes don't match");
            return TextConstants.LTI3ERROR;
        }

        //We will use this link to find the content to display.
        NonceState nonceState = ltiDataService.getRepos().nonceStateRepository.findByStateHash(state);

        try {
            Jws<Claims> claims = ltijwtService.validateState(nonceState.getState());
            LTI3Request lti3Request = LTI3Request.getInstance(link);
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the state (if sent in OIDC initiation request) matches the one coming with the ltiRequest.
            String clientIdFromState = claims.getBody().get("clientId") != null ? claims.getBody().get("clientId").toString() : null;
            if (clientIdFromState != null && !clientIdFromState.equals(lti3Request.getAud())) {
                model.addAttribute(TextConstants.ERROR, "Invalid Client Id");
                return TextConstants.LTI3ERROR;
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the state (if sent in the OIDC initiation request) matches the one coming with the ltiRequest.
            String deploymentIdFromState = claims.getBody().get("ltiDeploymentId") != null ? claims.getBody().get("ltiDeploymentId").toString() : null;
            if (deploymentIdFromState != null && !deploymentIdFromState.equals(lti3Request.getLtiDeploymentId())) {
                model.addAttribute(TextConstants.ERROR, "Invalid Deployment Id");
                return TextConstants.LTI3ERROR;
            }
            //We add the request to the model so it can be displayed. But, in a real application, we would start
            // processing it here to generate the right answer.
            if (ltiDataService.getDemoMode()) {
                model.addAttribute("lTI3Request", lti3Request);
                if (link == null) {
                    link = lti3Request.getLtiTargetLinkUrl().substring(lti3Request.getLtiTargetLinkUrl().lastIndexOf("?link=") + 6);
                }
                if (StringUtils.isNotBlank(link)) {
                    List<LtiLinkEntity> linkEntity = ltiLinkRepository.findByLinkKeyAndContext(link, lti3Request.getContext());
                    log.debug("Searching for link " + link + " in the context Key " + lti3Request.getContext().getContextKey() + " And id " + lti3Request.getContext().getContextId());
                    if (linkEntity.size() > 0) {
                        model.addAttribute(TextConstants.HTML_CONTENT, linkEntity.get(0).createHtmlFromLink());
                    } else {
                        model.addAttribute(TextConstants.HTML_CONTENT, "<b> No element was found for that context and linkKey</b>");
                    }
                } else {
                    model.addAttribute(TextConstants.HTML_CONTENT, "<b> No element was requested or it doesn't exists </b>");
                }
                if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
                    //Let's create the LtiLinkEntity's in our database
                    //This should be done AFTER the user selects the link in the content selector, and we are doing it before
                    //just to keep it simple. The ideal process would be, the user selects a link, sends it to the platform and
                    // we create the LtiLinkEntity in our code after that.
                    LtiLinkEntity ltiLinkEntity = new LtiLinkEntity("1234", lti3Request.getContext(), "My Test Link");
                    if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity.getLinkKey(), ltiLinkEntity.getContext()).size() == 0) {
                        ltiLinkRepository.save(ltiLinkEntity);
                    }
                    LtiLinkEntity ltiLinkEntity2 = new LtiLinkEntity("4567", lti3Request.getContext(), "Another Link");
                    if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity2.getLinkKey(), ltiLinkEntity2.getContext()).size() == 0) {
                        ltiLinkRepository.save(ltiLinkEntity2);
                    }
                    return "lti3DeepLink";
                }
                return "lti3Result";
            } else {
                String oneTimeToken = apiJWTService.buildJwt(
                        true,
                        lti3Request);
                return "redirect:/app/app.html?token=" + oneTimeToken;
            }
        } catch (SignatureException ex) {
            model.addAttribute(TextConstants.ERROR, ex.getMessage());
            return TextConstants.LTI3ERROR;
        } catch (GeneralSecurityException e) {
            model.addAttribute(TextConstants.ERROR, e.getMessage());
            return TextConstants.LTI3ERROR;
        } catch (IOException e) {
            model.addAttribute(TextConstants.ERROR, e.getMessage());
            return TextConstants.LTI3ERROR;
        }
    }

}
