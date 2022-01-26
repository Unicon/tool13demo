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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.service.lti.RegistrationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * This manages the Registration call
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {
    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    private RestTemplate restTemplate;

    //Calling the membership service and getting a paginated result of users.
    @Override
    public String callDynamicRegistration(String token, ToolRegistrationDTO toolRegistrationDTO, String endpoint) throws ConnectionException {
        String answer;
        try {
            restTemplate = restTemplate == null ? new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())) : restTemplate;
            DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
            defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
            restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

            //We add the token in the request with this.
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.isNotBlank(token)) {
                log.debug("Token was not blank: {}", token);
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            HttpEntity<ToolRegistrationDTO> request = new HttpEntity<>(toolRegistrationDTO, headers);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.

            if (log.isDebugEnabled()) {
                ObjectMapper objectMapper = new ObjectMapper();
                String toolRegistrationDTOString = objectMapper.writeValueAsString(toolRegistrationDTO);
                log.debug(toolRegistrationDTOString);
            }

            log.debug("Endpoint -  " + endpoint);
            ResponseEntity<String> registrationRequest = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
            HttpStatus status = registrationRequest.getStatusCode();
            if (status.is2xxSuccessful()) {
                answer = registrationRequest.getBody();
                log.debug(answer);
            } else {
                log.error(registrationRequest.getBody());
                String exceptionMsg = "Can't get confirmation of the registration";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Problem during the registration");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return answer;
    }


}
