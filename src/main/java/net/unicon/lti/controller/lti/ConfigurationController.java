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
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

/**
 * This controller is protected by basic authentication
 * Allows to read and change the configuration
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/config")
public class ConfigurationController {
    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @GetMapping(value = "/", produces = "application/json;")
    @ResponseBody
    public ResponseEntity<Page<PlatformDeployment>> displayConfigs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<PlatformDeployment> platformDeploymentListEntityList = platformDeploymentRepository.findAll(PageRequest.of(page, size));
        if (platformDeploymentListEntityList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        return new ResponseEntity<>(platformDeploymentListEntityList, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json;")
    @ResponseBody
    public ResponseEntity<PlatformDeployment> displayConfig(@PathVariable("id") long id) {

        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(id);

        if (!platformDeployment.isPresent()) {
            log.error("platformDeployment with id {} not found.", id);
            return new ResponseEntity("platformDeployment with id " + id
                    + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(platformDeployment.get(), HttpStatus.OK);
        }
    }

    @PostMapping("/")
    public ResponseEntity<PlatformDeployment> createDeployment(@RequestBody PlatformDeployment platformDeployment) {
        log.info("Creating Deployment : {}", platformDeployment);

        if (!platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(platformDeployment.getIss(), platformDeployment.getClientId(), platformDeployment.getDeploymentId()).isEmpty()) {
            log.error("Unable to create. A platformDeployment like that already exist");
            return new ResponseEntity("Unable to create. This platformDeployment already exists.", HttpStatus.CONFLICT);
        }
        PlatformDeployment platformDeploymentSaved = platformDeploymentRepository.save(platformDeployment);

        return new ResponseEntity<>(platformDeploymentSaved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatformDeployment> updateDeployment(@PathVariable("id") long id, @RequestBody PlatformDeployment platformDeployment) {
        log.info("Updating User with id {}", id);

        Optional<PlatformDeployment> platformDeploymentSearchResult = platformDeploymentRepository.findById(id);

        if (!platformDeploymentSearchResult.isPresent()) {
            log.error("Unable to update. PlatformDeployment with id {} not found.", id);
            return new ResponseEntity("Unable to update. User with id " + id + TextConstants.NOT_FOUND_SUFFIX,
                    HttpStatus.NOT_FOUND);
        }

        PlatformDeployment platformDeploymentToChange = platformDeploymentSearchResult.get();
        platformDeploymentToChange.setoAuth2TokenUrl(platformDeployment.getoAuth2TokenUrl());
        platformDeploymentToChange.setClientId(platformDeployment.getClientId());
        platformDeploymentToChange.setDeploymentId(platformDeployment.getDeploymentId());
        platformDeploymentToChange.setIss(platformDeployment.getIss());
        platformDeploymentToChange.setOidcEndpoint(platformDeployment.getOidcEndpoint());
        platformDeploymentToChange.setJwksEndpoint(platformDeployment.getJwksEndpoint());
        platformDeploymentToChange.setLumenAdminId(platformDeployment.getLumenAdminId());

        platformDeploymentRepository.saveAndFlush(platformDeploymentToChange);
        return new ResponseEntity<>(platformDeploymentToChange, HttpStatus.OK);
    }
}
