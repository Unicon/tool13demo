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
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    public void lti3(HttpServletRequest req, HttpServletResponse res)  {
        //First we will get the state, validate it
        String state = req.getParameter("state");
        //We will use this link to find the content to display.
        String link = req.getParameter("link");

        try{
            Jws<Claims> claims = ltijwtService.validateState(state);
            lti3Request = LTI3Request.getInstance(link); // validates nonce & id_token
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("clientId").equals(lti3Request.getAud())) {
//                return "invalid client_id";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client_id");
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("ltiDeploymentId").equals(lti3Request.getLtiDeploymentId())) {
//                return "invalid deployment-id";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid deployment_id");
            }

            if (!ltiDataService.getDemoMode()) {
                String target = lti3Request.getLtiTargetLinkUrl();
                log.debug(target);
                String ltiData = LtiOidcUtils.generateLtiToken(lti3Request, ltiDataService);
                HttpEntity entity = MultipartEntityBuilder.create().addTextBody("id_token", ltiData).build();
                String redirect = UriComponentsBuilder.fromUriString(target).build().toUriString();
                HttpPost httpPost = new HttpPost(redirect);
                httpPost.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ltiDataService.getLocalUrl());
                httpPost.setEntity(entity);
//                URI redirect = UriComponentsBuilder.fromUriString(target).queryParam("id_token", ltiData).build().toUri();
//                HttpPost httpPost = new HttpPost(redirect);
                CloseableHttpResponse response = client.execute(httpPost);
//                return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT).location(UriComponentsBuilder.fromUriString(target).queryParam("id_token", ltiData).build().toUri()).build();

                if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
                    log.error("Unsuccessful Post to {}", redirect.toString());
                    log.error(String.valueOf(response.getStatusLine().getStatusCode()));
                    log.error(response.getStatusLine().getReasonPhrase());
                    log.error(new String(response.getEntity().getContent().readAllBytes()));
//                    return "Unsuccessful post to application: " + response.getStatusLine().toString();
                    ByteStreams.copy(new ByteArrayInputStream(("Unsuccessful post to application: " + response.getStatusLine().toString()).getBytes()), res.getOutputStream());
                } else {
                    res.setContentType(MediaType.TEXT_HTML_VALUE);
//                    return "post to application returned Http Status OK";
                    ByteStreams.copy(response.getEntity().getContent(), res.getOutputStream());
                }
            }
            else {
//                return "demo mode enabled";
//                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(ltiDataService.getLocalUrl() + "/demo?link=" + link)).build();
                res.sendRedirect("/demo?link=" + link);
            }
        } catch (SignatureException ex) {
//            return "invalid signature";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        } catch (IOException ex) {
//            return "io exception";
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad Request");
        } catch (GeneralSecurityException ex) {
//            return "general security exception";
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error");
        }
    }

    @RequestMapping("/demo")
    public String demo(HttpServletRequest req, Model model) {
        System.out.println("inside demo endpoint");
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
