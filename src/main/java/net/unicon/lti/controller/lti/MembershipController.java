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


import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.membership.CourseUsers;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.AdvantageMembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Optional;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/membership")
@ConditionalOnExpression("${lti13.enableMembership}")
public class MembershipController {
    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    AdvantageMembershipService advantageMembershipService;

    @SuppressWarnings("SameReturnValue")
    @RequestMapping({"", "/"})
    public String membershipGet(HttpServletRequest req, Principal principal, Model model) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if (session.getAttribute("deployment_key") != null) {
            model.addAttribute("noSessionValues", false);
            Long deployment = (Long) session.getAttribute("deployment_key");
            String contextId = (String) session.getAttribute("context_id");
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the membership service to get the users on the context
                // 1. Get the token
                LTIAdvantageToken LTIAdvantageToken = advantageMembershipService.getToken(platformDeployment.get());

                // 2. Call the service
                CourseUsers courseUsers = advantageMembershipService.callMembershipService(LTIAdvantageToken, context);

                // 3. update the model
                model.addAttribute("results", courseUsers.getCourseUserList());
            }
        } else {
            model.addAttribute("noSessionValues", true);
        }
        return "ltiAdvMembershipMain";
    }


}
