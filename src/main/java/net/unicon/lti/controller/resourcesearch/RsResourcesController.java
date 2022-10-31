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
package net.unicon.lti.controller.resourcesearch;


import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.membership.CourseUsers;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.model.resourcesearch.CCLTILinkEntity;
import net.unicon.lti.model.resourcesearch.RsResourceEntity;
import net.unicon.lti.model.resourcesearch.Vendor;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.repository.RsCCLTILinkRepository;
import net.unicon.lti.repository.RsResourceRepository;
import net.unicon.lti.repository.RsVendorRepository;
import net.unicon.lti.service.lti.AdvantageMembershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Controller
@Scope("session")
@RequestMapping("/resources")
public class RsResourcesController {

    @Autowired
    RsResourceRepository rsResourceRepository;

    @Autowired
    RsCCLTILinkRepository rsCCLTILinkRepository;

    @Autowired
    RsVendorRepository rsVendorRepository;

    @GetMapping(value = {"/", ""}, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<Page<RsResourceEntity>> getResources(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        Page<RsResourceEntity> rsResourceEntityPage = rsResourceRepository.findAll(PageRequest.of(page, size));
        if (rsResourceEntityPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        return new ResponseEntity<>(rsResourceEntityPage, HttpStatus.OK);
    }

    @PostMapping("/")
    public ResponseEntity<RsResourceEntity> createResource(@RequestBody RsResourceEntity rsResourceEntity) {
        log.info("Creating Resource: {}", rsResourceEntity);

        RsResourceEntity rsResourceEntitySaved = rsResourceRepository.save(rsResourceEntity);

        return new ResponseEntity<>(rsResourceEntitySaved, HttpStatus.CREATED);
    }

    @PostMapping("/{resourceId}/ccltilink/{ccltilinkId}")
    public ResponseEntity<RsResourceEntity> addCCLTILink(@PathVariable(value = "resourceId") long resourceId, @PathVariable(value = "ccltilinkId") long ccltilinkId) {
        RsResourceEntity rsResource = rsResourceRepository.findById(resourceId);
        CCLTILinkEntity ccltiLinkEntity = rsCCLTILinkRepository.findById(ccltilinkId);
        if (rsResource == null || ccltiLinkEntity == null) {
            log.error("rsResource was {}", rsResource);
            log.error("CCLTILink was {}", ccltiLinkEntity);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        rsResource.setLtiLink(ccltiLinkEntity);
        rsResourceRepository.save(rsResource);
        Set<RsResourceEntity> ltiLinkResources = ccltiLinkEntity.getRsResourceEntities();
        ltiLinkResources.add(rsResource);
        ccltiLinkEntity.setRsResourceEntities(ltiLinkResources);
        rsCCLTILinkRepository.save(ccltiLinkEntity);

        return new ResponseEntity<>(rsResource, HttpStatus.OK);
    }

    @GetMapping(value = {"/ccltilink", "/ccltilink/"}, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<Page<CCLTILinkEntity>> getCCLTILinkEntities(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        Page<CCLTILinkEntity> ccltiLinkEntityPage = rsCCLTILinkRepository.findAll(PageRequest.of(page, size));
        if (ccltiLinkEntityPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        return new ResponseEntity<>(ccltiLinkEntityPage, HttpStatus.OK);
    }

    @PostMapping("/ccltilink")
    public ResponseEntity<CCLTILinkEntity> createVendor(@RequestBody CCLTILinkEntity ccltiLinkEntity) {
        log.info("Creating CCLTILink: {}", ccltiLinkEntity);

        CCLTILinkEntity ccltiLinkEntitySaved = rsCCLTILinkRepository.save(ccltiLinkEntity);

        return new ResponseEntity<>(ccltiLinkEntitySaved, HttpStatus.CREATED);
    }

    @PostMapping("/vendor")
    public ResponseEntity<Vendor> createVendor(@RequestBody Vendor vendor) {
        log.info("Creating Vendor: {}", vendor);

        Vendor vendorSaved = rsVendorRepository.save(vendor);

        return new ResponseEntity<>(vendorSaved, HttpStatus.CREATED);
    }
}
