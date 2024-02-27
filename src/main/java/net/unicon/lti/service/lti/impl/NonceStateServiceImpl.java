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
package net.unicon.lti.service.lti.impl;

import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.service.lti.NonceStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class NonceStateServiceImpl implements NonceStateService {

    static final Logger log = LoggerFactory.getLogger(NonceStateServiceImpl.class);

    @Autowired
    NonceStateRepository nonceStateRepository;

    //TODO, add more methods here and remove code from the controllers or services that call the repo directly.

    @Override
    public void deleteOldNonces() {
        Date thresholdDate = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
        nonceStateRepository.deleteByCreatedAtBefore(thresholdDate);
    }

    @Override
    public NonceState getNonce(String nonce) {
            return nonceStateRepository.findByNonce(nonce);
    }

    @Override
    public void deleteNonce(String nonce) {
        nonceStateRepository.deleteByNonce(nonce);
    }
}
