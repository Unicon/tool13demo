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


import net.unicon.lti13demo.exceptions.ConnectionException;
import net.unicon.lti13demo.model.LtiContextEntity;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.membership.CourseUsers;
import net.unicon.lti13demo.model.oauth2.Token;
import net.unicon.lti13demo.repository.LtiContextRepository;
import net.unicon.lti13demo.repository.PlatformDeploymentRepository;
import net.unicon.lti13demo.service.AdvantageMembershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Controller
@Scope("session")
@RequestMapping("/membership")
public class MembershipController {

    static final Logger log = LoggerFactory.getLogger(MembershipController.class);

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    AdvantageMembershipService advantageMembershipService;


    @RequestMapping({"", "/{deployment}"})
    public String membershipMain(@PathVariable("deployment") Long deployment, HttpServletRequest req, Principal principal, Model model) {

        //Get the contexts available for this user
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
        if (platformDeployment.isPresent()){
            List<LtiContextEntity> contexts = ltiContextRepository.findByPlatformDeployment(platformDeployment.get());
            model.addAttribute("contexts",contexts);
        }
        return "ltiAdvMembershipMain";
    }

    @RequestMapping({"", "/{deployment}/context/{id}"})
    public String membershipGet(@PathVariable("deployment") Long deployment, @PathVariable("id") String key, HttpServletRequest req, Principal principal, Model model) throws ConnectionException {

        //Get the contexts available for this user
        //Try to use session.
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
        if (platformDeployment.isPresent()) {
            List<LtiContextEntity> contexts = ltiContextRepository.findByPlatformDeployment(platformDeployment.get());
            model.addAttribute("contexts",contexts);
            model.addAttribute("deploymentKey",deployment);

            //Get the context in the query
            LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(key, platformDeployment.get());
            model.addAttribute("contextToQuery", context);


            //Call the membership service to get the users on the context
            // 1. Get the token
            Token token = advantageMembershipService.getToken(platformDeployment.get());

            // 2. Call the service
            CourseUsers courseUsers = advantageMembershipService.callMembershipService(token, context);

            // 3. update the model
            model.addAttribute("results", courseUsers.getCourseUserList());
        }
        return "ltiAdvMembershipMain";
    }


}
