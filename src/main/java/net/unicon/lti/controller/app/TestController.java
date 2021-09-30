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
package net.unicon.lti.controller.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;

@Controller
@RequestMapping(value = TestController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnExpression("${lti13.enableRoleTesting}")
public class TestController {
    static final String REQUEST_ROOT = "api/test";

    @SuppressWarnings("rawtypes")
    @RequestMapping(method = RequestMethod.GET, value = "/general")
    @PreAuthorize("hasAnyRole('GENERAL')")
    public ResponseEntity sampleSecureEndpointAny() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping(method = RequestMethod.GET, value = "/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity sampleSecureEndpointAdmin() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping(method = RequestMethod.GET, value = "/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity sampleSecureEndpointInstructor(@AuthenticationPrincipal Principal principal) {
        String a = "hello";
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping(method = RequestMethod.GET, value = "/student")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity sampleSecureEndpointStudent() {
        return new ResponseEntity<>("Welcome", HttpStatus.OK);
    }
}
