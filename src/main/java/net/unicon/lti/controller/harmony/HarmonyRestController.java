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

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import net.unicon.lti.service.harmony.HarmonyService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.lti.LTI3Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/harmony", produces = MediaType.APPLICATION_JSON_VALUE)
@Scope("session")
@Slf4j
public class HarmonyRestController {

    @Autowired
    HarmonyService harmonyService;

    @Autowired
    LTIDataService ltiDataService;

    LTI3Request lti3Request;

    @RequestMapping(value = "/courses")
    public ResponseEntity<HarmonyPageResponse> listHarmonyCourses(@RequestHeader(value="lti-id-token") String ltiIdToken, @RequestParam(required = false) Integer page, @RequestParam(required = false, value = "root_outcome_guid") String rootOutcomeGuid) {
        //To keep this endpoint secured, we will validate the id_token
        try {
            // validates JWT signature, ensures existing platformDeployment, validates 1.3 format of JWT, validates nonce
            lti3Request = new LTI3Request(ltiDataService, true, null, ltiIdToken);
            log.debug("The id_token is valid, fetching courses....");
            // We convert the JSON response to a Java object, and send the JSON value again to the frontend.
            return ResponseEntity.ok(harmonyService.fetchHarmonyCourses(page, rootOutcomeGuid));
        } catch (Exception e) {
            log.debug(e.getMessage());
            log.debug("No permissions to fetch courses from Harmony");
            return ResponseEntity.status(403).build();
        }
    }

}
