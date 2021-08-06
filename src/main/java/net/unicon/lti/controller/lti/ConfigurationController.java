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

import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * This controller is protected by basic authentication
 * Allows to read and change the configuration
 */
@Controller
@Scope("session")
@RequestMapping("/config")
public class ConfigurationController {

    static final Logger log = LoggerFactory.getLogger(ConfigurationController.class);


    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;


    /**
     * To show the configurations.
     */
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<PlatformDeployment>> displayConfigs(HttpServletRequest req) {

        List<PlatformDeployment> platformDeploymentListEntityList = platformDeploymentRepository.findAll();
        if (platformDeploymentListEntityList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        return new ResponseEntity<>(platformDeploymentListEntityList, HttpStatus.OK);
    }

    /**
     * To show the configurations.
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<PlatformDeployment> displayConfig(@PathVariable("id") long id, HttpServletRequest req) {

        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(id);

        if (!platformDeployment.isPresent()) {
            log.error("platformDeployment with id {} not found.", id);
            return new ResponseEntity("platformDeployment with id " + id
                    + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(platformDeployment.get(), HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> createDeployment(@RequestBody PlatformDeployment platformDeployment, UriComponentsBuilder ucBuilder) {
        log.info("Creating Deployment : {}", platformDeployment);

        if (!platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(platformDeployment.getIss(), platformDeployment.getClientId(), platformDeployment.getDeploymentId()).isEmpty()) {
            log.error("Unable to create. A platformDeployment like that already exist");
            return new ResponseEntity("Unable to create. A platformDeployment with same key already exist.", HttpStatus.CONFLICT);
        }
        PlatformDeployment platformDeploymentSaved = platformDeploymentRepository.save(platformDeployment);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/config/{id}").buildAndExpand(platformDeploymentSaved.getKeyId()).toUri());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
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

        platformDeploymentRepository.saveAndFlush(platformDeploymentToChange);
        return new ResponseEntity<>(platformDeploymentToChange, HttpStatus.OK);
    }
}
