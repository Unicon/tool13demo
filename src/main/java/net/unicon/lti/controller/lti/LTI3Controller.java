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
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.LtiLinkEntity;
import net.unicon.lti.repository.LtiLinkRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@Scope("session")
public class LTI3Controller {
    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    LTIDataService ltiDataService;

    LTI3Request lti3Request;

    private CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

    @PostMapping(value={"/lti3","/lti3/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String lti3(HttpServletRequest req, HttpServletResponse res, Model model)  {
        //First we will get the state, validate it
        String state = req.getParameter("state");
        //We will use this link to find the content to display.
        String link = req.getParameter("link");

        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            lti3Request = LTI3Request.getInstance(link); // validates nonce & id_token
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the state (if sent in OIDC initiation request) matches the one coming with the ltiRequest.
            String clientIdFromState = claims.getBody().get("clientId") != null ? claims.getBody().get("clientId").toString() : null;
            if (clientIdFromState != null && !clientIdFromState.equals(lti3Request.getAud())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client_id");
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the state (if sent in the OIDC initiation request) matches the one coming with the ltiRequest.
            String deploymentIdFromState = claims.getBody().get("ltiDeploymentId") != null ? claims.getBody().get("ltiDeploymentId").toString() : null;
            if (deploymentIdFromState != null && !deploymentIdFromState.equals(lti3Request.getLtiDeploymentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid deployment_id");
            }

            if (!ltiDataService.getDemoMode()) {
                String target = lti3Request.getLtiTargetLinkUrl();
                log.debug("Target Link URL: {}", target);
                String ltiData = LtiOidcUtils.generateLtiToken(lti3Request, ltiDataService);

                model.addAttribute("target", target);
                model.addAttribute("id_token", ltiData);
            } else {
                model.addAttribute("target", ltiDataService.getLocalUrl() + "/demo?link=" + link);
            }

            // When the LTI message type is deep linking we must to display the React UI to select courses from harmony. 
            if (LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(lti3Request.getLtiMessageType())) {
                if (ltiDataService.getDeepLinkingEnabled()) {
                    // Send the relevant LTI attributes to the frontend
                    model.addAttribute("deploymentId", deploymentIdFromState);
                    model.addAttribute("clientId", clientIdFromState);
                    model.addAttribute("iss", lti3Request.getIss());
                    model.addAttribute("context", lti3Request.getLtiContextId());
                    // This redirects to the REACT UI which is a secondary set of templates.
                    return TextConstants.REACT_UI_TEMPLATE;
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deep Linking Disabled");
                }
            }

            return "lti3Redirect";

        } catch (SignatureException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        } catch (GeneralSecurityException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error");
        }
    }

    @RequestMapping("/demo")
    public String demo(HttpServletRequest req, Model model) {
        String link = req.getParameter("link");
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
        if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING) && ltiDataService.getDeepLinkingEnabled()) {
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
        } else if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deep Linking Disabled");
        }
        return "lti3Result";
    }

}
