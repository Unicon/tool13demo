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

import io.jsonwebtoken.SignatureException;
import net.unicon.lti13demo.service.LTIJWTService;
import net.unicon.lti13demo.utils.lti.LTI3Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Controller
@RequestMapping("/lti3")
public class LTI3Controller {

    @Autowired
    LTIJWTService ltijwtService;

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) {

        String state = req.getParameter("state");
        try {
            ltijwtService.validateState(state);
            LTI3Request lti3Request = LTI3Request.getInstance();
            model.addAttribute("lTI3Request", lti3Request);
            return "lti3Result";
        } catch (SignatureException ex){
            model.addAttribute("Error", ex.getMessage());
            return "lti3Error";
        }
    }



}
