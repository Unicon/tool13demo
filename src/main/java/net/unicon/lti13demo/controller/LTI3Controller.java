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
import net.unicon.lti13demo.service.LTIJWTService;
import net.unicon.lti13demo.utils.LtiStrings;
import net.unicon.lti13demo.utils.lti.LTI3Request;
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

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) {

        String state = req.getParameter("state");
        Enumeration<String> sessionAtributes = req.getSession().getAttributeNames();
        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            LTI3Request lti3Request = LTI3Request.getInstance();
            //Checking that the deploymentId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("deploymentId").equals(lti3Request.getLtiDeploymentId())) {
                model.addAttribute("Error", " Bad Deployment Id");
                return "lti3Error";
            }
            model.addAttribute("lTI3Request", lti3Request);
            if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
                return "lti3DeepLink";
            }
            return "lti3Result";
        } catch (SignatureException ex){
            model.addAttribute("Error", ex.getMessage());
            return "lti3Error";
        }
    }



}
