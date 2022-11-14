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
import net.unicon.lti.model.resourcesearch.RsSubjectEntity;
import net.unicon.lti.model.resourcesearch.Vendor;
import net.unicon.lti.repository.RsCCLTILinkRepository;
import net.unicon.lti.repository.RsResourceRepository;
import net.unicon.lti.repository.RsSubjectRepository;
import net.unicon.lti.repository.RsVendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@Scope("session")
@RequestMapping("/ims/rs/v1p0/subjects")
public class RsSubjectsController {

    @Autowired
    private RsSubjectRepository rsSubjectRepository;

    @GetMapping(value = {"/", ""}, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<Page<RsSubjectEntity>> getResources(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        Page<RsSubjectEntity> rsSubjectEntityPage = rsSubjectRepository.findAll(PageRequest.of(page, size));
        if (rsSubjectEntityPage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        return new ResponseEntity<>(rsSubjectEntityPage, HttpStatus.OK);
    }


    @PostMapping("/")
    public ResponseEntity<RsSubjectEntity> createSubject(@RequestBody RsSubjectEntity rsSubjectEntity) {
        log.info("Creating Subject: {}", rsSubjectEntity);

        RsSubjectEntity rsSubjectEntitySaved = rsSubjectRepository.save(rsSubjectEntity);

        return new ResponseEntity<>(rsSubjectEntitySaved, HttpStatus.CREATED);
    }

}
