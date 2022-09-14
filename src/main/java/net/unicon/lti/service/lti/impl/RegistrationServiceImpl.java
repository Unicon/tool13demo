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
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationDTO;
import net.unicon.lti.model.lti.dto.ToolMessagesSupportedDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.RestUtils;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This manages the Registration call
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {
    @Value("${application.url}")
    private String localUrl;

    @Value("${domain.url}")
    private String domainUrl;

    @Value("${application.name}")
    private String clientName;

    @Value("${application.description}")
    private String description;

    @Value("${application.deep.linking.menu.label}")
    private String deepLinkingMenuLabel;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    //Calling the membership service and getting a paginated result of users.
    @Override
    public String callDynamicRegistration(String token, ToolRegistrationDTO toolRegistrationDTO, String endpoint) throws ConnectionException {
        String answer;
        try {
            RestTemplate restTemplate = RestUtils.createRestTemplate();
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
                log.debug("Tool Registration DTO:");
                ObjectMapper objectMapper = new ObjectMapper();
                String toolRegistrationDTOString = objectMapper.writeValueAsString(toolRegistrationDTO);
                log.debug(toolRegistrationDTOString);
            }

            log.debug("Platform's registration_endpoint: " + endpoint);
            ResponseEntity<String> registrationRequest = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
            HttpStatus status = registrationRequest.getStatusCode();
            if (status.is2xxSuccessful()) {
                answer = registrationRequest.getBody();
                log.info("Registration successfully confirmed! Platform's response to the Tool Registration DTO: {}", answer);
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

    public ToolRegistrationDTO generateToolConfiguration(PlatformRegistrationDTO platformConfiguration) {
        ToolRegistrationDTO toolRegistrationDTO = new ToolRegistrationDTO();

        // Provide Required Constants for the Spec
        toolRegistrationDTO.setApplication_type("web");
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("implicit");
        grantTypes.add("client_credentials");
        toolRegistrationDTO.setGrant_types(grantTypes);
        toolRegistrationDTO.setResponse_types(Collections.singletonList("id_token"));
        toolRegistrationDTO.setToken_endpoint_auth_method("private_key_jwt");

        // Provide Tool URLs/Data for Registration
        toolRegistrationDTO.setRedirect_uris(Collections.singletonList(localUrl + TextConstants.LTI3_SUFFIX));
        toolRegistrationDTO.setInitiate_login_uri(localUrl + "/oidc/login_initiations");
        toolRegistrationDTO.setClient_name(clientName);
        toolRegistrationDTO.setJwks_uri(localUrl + "/jwks/jwk");
        //OPTIONAL -->setLogo_uri
        //OPTIONAL -->setContacts
        //OPTIONAL -->setClient_uri
        //OPTIONAL -->setTos_uri
        //OPTIONAL -->setPolicy_uri
        ToolConfigurationDTO toolConfigurationDTO = new ToolConfigurationDTO();
        toolConfigurationDTO.setDomain(domainUrl);
        //OPTIONAL -->setSecondary_domains --> Collections.singletonList
        //OPTIONAL -->setDeployment_id

        toolConfigurationDTO.setTarget_link_uri(domainUrl);

        //OPTIONAL -->setCustom_parameters --> Map
        toolConfigurationDTO.setDescription(description);
        List<ToolMessagesSupportedDTO> messages = new ArrayList<>();

        // Indicate Deep Linking support
        ToolMessagesSupportedDTO message1 = new ToolMessagesSupportedDTO();
        message1.setType("LtiDeepLinkingRequest");
        message1.setTarget_link_uri(localUrl + TextConstants.LTI3_SUFFIX);
        message1.setLabel(deepLinkingMenuLabel);
//        OPTIONAL: --> message1 --> setIcon_uri
//        OPTIONAL: --> message1 --> setCustom_parameters
        messages.add(message1);

        ToolMessagesSupportedDTO message2 = new ToolMessagesSupportedDTO();
        message2.setType("LtiResourceLinkRequest");
        message2.setTarget_link_uri(localUrl + TextConstants.LTI3_SUFFIX);
        messages.add(message2);
        toolConfigurationDTO.setMessages_supported(messages);

        Set<String> platformAndOptionalClaims = new LinkedHashSet<>(platformConfiguration.getClaims_supported());
        platformAndOptionalClaims.addAll(LtiStrings.LTI_OPTIONAL_CLAIMS);
        List<String> toolConfigurationClaims = new ArrayList<>(platformAndOptionalClaims);
        toolConfigurationDTO.setClaims(toolConfigurationClaims);

        toolRegistrationDTO.setToolConfiguration(toolConfigurationDTO);
        toolRegistrationDTO.setScope(StringUtils.join(platformConfiguration.getScopes_supported(), " "));

        return toolRegistrationDTO;
    }
}
