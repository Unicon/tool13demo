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
import net.unicon.lti.model.resourcesearch.CCLTILinkEntity;
import net.unicon.lti.model.resourcesearch.RsResourceEntity;
import net.unicon.lti.model.resourcesearch.Vendor;
import net.unicon.lti.repository.RsCCLTILinkRepository;
import net.unicon.lti.repository.RsResourceRepository;
import net.unicon.lti.repository.RsVendorRepository;
import net.unicon.lti.utils.resourcesearch.ResourceSearchCriteria;
import net.unicon.lti.utils.resourcesearch.RsResourceSpecificationsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Controller
@Scope("session")
@RequestMapping("/ims/rs/v1p0/resources")
public class RsResourcesController {

    @Autowired
    RsResourceRepository rsResourceRepository;

    @Autowired
    RsCCLTILinkRepository rsCCLTILinkRepository;

    @Autowired
    RsVendorRepository rsVendorRepository;

    @GetMapping(value = {"/", ""}, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<Page<RsResourceEntity>> getResources(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "filter", required = false) String filterParam
            ) throws UnsupportedEncodingException {
        Page<RsResourceEntity> rsResourceEntityPage = null;
        if (StringUtils.isNotEmpty(filterParam)) {
            filterParam = URLDecoder.decode(filterParam, StandardCharsets.UTF_8.name());
        }
        log.debug("filterParam is {}", filterParam);
        RsResourceSpecificationsBuilder builder = new RsResourceSpecificationsBuilder();
        Pattern pattern = Pattern.compile("([a-zA-Z]+)(!=|:|<|>)(.*),");
        Matcher matcher = pattern.matcher(filterParam + ",");
        while (matcher.find()) {
            log.debug("Pattern Matcher Group 1 is {}", matcher.group(1));
            log.debug("Pattern Matcher Group 2 is {}", matcher.group(2));
            log.debug("Pattern Matcher Group 3 is {}", matcher.group(3));
            builder.getParams().add(new ResourceSearchCriteria(matcher.group(1), matcher.group(2), matcher.group(3)));
        }

        Specification<RsResourceEntity> spec = builder.build();
        rsResourceEntityPage = rsResourceRepository.findAll(spec, PageRequest.of(page, size));

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

    @GetMapping(value = {"/ccltilink", "/ccltilink/"}, produces = "application/json")
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
    public ResponseEntity<CCLTILinkEntity> createCCLTILink(@RequestBody CCLTILinkEntity ccltiLinkEntity) {
        log.info("Creating CCLTILink: {}", ccltiLinkEntity);

        CCLTILinkEntity ccltiLinkEntitySaved = rsCCLTILinkRepository.save(ccltiLinkEntity);

        return new ResponseEntity<>(ccltiLinkEntitySaved, HttpStatus.CREATED);
    }

    @PostMapping("/ccltilink/{ccltilinkId}/vendor/{vendorId}")
    public ResponseEntity<CCLTILinkEntity> addVendorToCCLTILink(@PathVariable(value = "ccltilinkId") long ccltilinkId, @PathVariable(value = "vendorId") long vendorId) {
        CCLTILinkEntity ccltiLinkEntity = rsCCLTILinkRepository.findById(ccltilinkId);
        Vendor vendor = rsVendorRepository.findById(vendorId);
        if (vendor == null || ccltiLinkEntity == null) {
            log.error("CCLTILink was {}", ccltiLinkEntity);
            log.error("vendor was {}", vendor);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ccltiLinkEntity.setVendor(vendor);
        rsCCLTILinkRepository.save(ccltiLinkEntity);
        Set<CCLTILinkEntity> ccltiLinkEntities = vendor.getCcltiLinkEntitySet();
        ccltiLinkEntities.add(ccltiLinkEntity);
        vendor.setCcltiLinkEntitySet(ccltiLinkEntities);
        rsVendorRepository.save(vendor);

        return new ResponseEntity<>(ccltiLinkEntity, HttpStatus.OK);
    }

    @PostMapping("/vendor")
    public ResponseEntity<Vendor> createVendor(@RequestBody Vendor vendor) {
        log.info("Creating Vendor: {}", vendor);

        Vendor vendorSaved = rsVendorRepository.save(vendor);

        return new ResponseEntity<>(vendorSaved, HttpStatus.CREATED);
    }
}
