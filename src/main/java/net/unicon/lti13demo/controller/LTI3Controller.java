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
package net.unicon.lti13demo.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import net.unicon.lti13demo.model.LtiLinkEntity;
import net.unicon.lti13demo.repository.LtiContextRepository;
import net.unicon.lti13demo.repository.LtiLinkRepository;
import net.unicon.lti13demo.service.LTIJWTService;
import net.unicon.lti13demo.utils.LtiStrings;
import net.unicon.lti13demo.utils.lti.LTI3Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Enumeration;
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
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    LtiContextRepository ltiContextRepository;

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) {

        //First we will get the state, validate it
        String state = req.getParameter("state");
        //We will use this link to find the content to display.
        String link = req.getParameter("link");
        Enumeration<String> sessionAtributes = req.getSession().getAttributeNames();
        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            LTI3Request lti3Request = LTI3Request.getInstance(link);
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("clientId").equals(lti3Request.getAud())) {
                model.addAttribute("Error", " Bad Client Id");
                return "lti3Error";
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("ltiDeploymentId").equals(lti3Request.getLtiDeploymentId())) {
                model.addAttribute("Error", " Bad Deployment Id");
                return "lti3Error";
            }
            //We add the request to the model so it can be displayed. But, in a real application, we would start
            // processing it here to generate the right answer.
            model.addAttribute("lTI3Request", lti3Request);

            if (StringUtils.isNotBlank(link)){
                List<LtiLinkEntity> linkEntity = ltiLinkRepository.findByLinkKeyAndContext(link, lti3Request.getContext());
                log.debug("Searching for link " + link + " in the context Key " + lti3Request.getContext().getContextKey() + " And id " + lti3Request.getContext().getContextId());
                if (linkEntity.size()>0) {
                    model.addAttribute("htmlContent", linkEntity.get(0).createHtmlFromLink());
                } else {
                    model.addAttribute( "htmlContent", "<b> No element was found for that context and linkKey</b>");
                }
            } else {
                model.addAttribute( "htmlContent", "<b> No element was requested or it doesn't exists </b>");
            }
            if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
                //Let's create the LtilinkEntity's in our database
                //This should be done AFTER the user selects the link in the content selector, and we are doing it before
                //just to keep it simple. The ideal process would be, the user selects a link, sends it to the platform and
                // we create the LtiLinkEntity in our code after that.
                LtiLinkEntity ltiLinkEntity = new LtiLinkEntity("1234", lti3Request.getContext(), "My Test Link", 50f);
                if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity.getLinkKey(), ltiLinkEntity.getContext()).size()==0) {
                    ltiLinkRepository.save(ltiLinkEntity);
                }
                LtiLinkEntity ltiLinkEntity2 = new LtiLinkEntity("4567", lti3Request.getContext(), "Another Link", 0f);
                if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity2.getLinkKey(), ltiLinkEntity2.getContext()).size()==0) {
                    ltiLinkRepository.save(ltiLinkEntity2);
                }
                return "lti3DeepLink";
            }
            return "lti3Result";
        } catch (SignatureException ex){
            model.addAttribute("Error", ex.getMessage());
            return "lti3Error";
        }
    }

}
