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

package net.unicon.lti.controller.harmony;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.harmony.HarmonyCourse;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.harmony.HarmonyService;
import net.unicon.lti.utils.LtiStrings;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "/harmony", produces = MediaType.APPLICATION_JSON_VALUE)
@Scope("session")
@Slf4j
public class HarmonyRestController {

    @Autowired
    HarmonyService harmonyService;

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @RequestMapping(value = "/courses")
    public ResponseEntity<HarmonyPageResponse> fetchHarmonyCourses(HttpServletRequest req, Principal principal) {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY) != null) {
            Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY);
            log.debug("The request contains the deploymentId {}", deployment);
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                log.debug("The deploymentId is present in the repository, fetching courses....");
                // We convert the JSON response to a Java object, and send the JSON value again to the frontend.
                return ResponseEntity.ok(harmonyService.fetchHarmonyCourses());
            }
        }
        log.debug("No permissions to fetch courses from Harmony");
        return ResponseEntity.status(403).build();
    }

}
