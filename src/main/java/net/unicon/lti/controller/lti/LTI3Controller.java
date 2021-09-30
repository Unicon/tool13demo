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

import com.google.common.io.ByteStreams;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/lti3")
public class LTI3Controller {
    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    LTIDataService ltiDataService;

    @RequestMapping({"", "/"})
    public String lti3(HttpServletRequest req, Model model) {
        //First we will get the state, validate it
        String state = req.getParameter("state");
        //We will use this link to find the content to display.
        String link = req.getParameter("link");
        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            LTI3Request lti3Request = LTI3Request.getInstance(link);
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("clientId").equals(lti3Request.getAud())) {
                model.addAttribute(TextConstants.ERROR, " Bad Client Id");
                return TextConstants.LTI3ERROR;
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("ltiDeploymentId").equals(lti3Request.getLtiDeploymentId())) {
                model.addAttribute(TextConstants.ERROR, " Bad Deployment Id");
                return TextConstants.LTI3ERROR;
            }
            //We add the request to the model so it can be displayed. But, in a real application, we would start
            // processing it here to generate the right answer.
            if (!ltiDataService.getDemoMode()) {
                return "forward:/lti3/target";
            } else {
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
                    return TextConstants.LTI3ERROR;
                }
                return "lti3Result";
            }
        } catch (SignatureException ex) {
            model.addAttribute(TextConstants.ERROR, ex.getMessage());
            return TextConstants.LTI3ERROR;
        }
    }

    @PostMapping(value = "/target", produces = MediaType.TEXT_HTML_VALUE)
    public void getExternalPage(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String link = req.getParameter("link");
        LTI3Request lti3Request = LTI3Request.getInstance(link);
        String target = lti3Request.getLtiTargetLinkUrl();
        System.out.println(target);
        String jwt = req.getParameter("id_token");
        String redirect = UriComponentsBuilder.fromUriString(target).queryParam("id_token", jwt).build().toUriString();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(redirect);
        CloseableHttpResponse response = client.execute(httpPost);
        ByteStreams.copy(response.getEntity().getContent(), res.getOutputStream());
    }

}
