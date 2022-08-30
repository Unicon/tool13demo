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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * This LTI controller should be protected by OAuth 1.0a (on the /oauth path)
 * This will handle LTI 1 and 2 (many of the paths ONLY make sense for LTI2 though)
 * Sample Key "key" and secret "secret"
 */
@SuppressWarnings("ALL")
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/registration")
@ConditionalOnExpression("${lti13.enableDynamicRegistration}")
public class RegistrationController {
    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    RegistrationService registrationService;

    @Value("${application.url}")
    private String localUrl;

    @Value("${lti13.demoMode}")
    private boolean demoMode;


    /**
     * This will receive the request to start the dynamic registration process and prepare the answer.
     * We receive some parameters (issuer, authorization_endpoint, registration_endpoint,
     * jwks_uri, token_endpoint, token_endpoint_auth_methods_supported,
     * token_endpoint_auth_signing_alg_values_supported,
     * scopes_supported, response_types_supported, subject_types_supported,
     * id_token_signing_alg_values_supported, claims_supported, authorization_server (optional) and
     * https://purl.imsglobal.org/spec/lti-platform-configuration --> product_family_code,version, messages_supported --> (type,placements (optional)), variables(optional))
     * @param req
     * @param model
     * @return
     */
    @RequestMapping(value = {"", "/"}, method = RequestMethod.GET)
    public String registration(@RequestParam("openid_configuration") String openidConfiguration, @RequestParam(name = LtiStrings.REGISTRATION_TOKEN, required = false) String registrationToken, HttpServletRequest req, Model model) {
        // We need to call the configuration endpoint recevied in the registration inititaion message and
        // call it to get all the information about the platform
        HttpSession session = req.getSession();
        log.debug("OpenID Configuration: {}", openidConfiguration);
        session.setAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);

        try {
            // We are going to create the call the openidconfiguration endpoint,
            RestTemplate restTemplate = RestUtils.createRestTemplate();

            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.

            HttpHeaders headers = new HttpHeaders();
            DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
            defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
            restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);
            ResponseEntity<PlatformRegistrationDTO> platformConfiguration = restTemplate.
                    exchange(openidConfiguration, HttpMethod.GET, new HttpEntity<>(headers), PlatformRegistrationDTO.class);
            PlatformRegistrationDTO platformRegistrationDTO = null;
            if (platformConfiguration != null && platformConfiguration.getStatusCode().is2xxSuccessful() && platformConfiguration.getBody() != null) {
                    platformRegistrationDTO = platformConfiguration.getBody();
                if (log.isDebugEnabled()) {
                    log.debug("Platform Registration DTO:");
                    ObjectMapper objectMapper = new ObjectMapper();
                    String platformRegistrationDTOString = objectMapper.writeValueAsString(platformRegistrationDTO);
                    log.debug(platformRegistrationDTOString);
                }
            } else {
                String exceptionMsg = "Can't get the platform configuration";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            session.setAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDTO);
            ToolRegistrationDTO toolRegistrationDTO = registrationService.generateToolConfiguration(platformConfiguration.getBody());
            session.setAttribute(LtiStrings.TOOL_CONFIGURATION, toolRegistrationDTO);

            // Once all is added to the session, and we have the data ready for the html template, we redirect
            if (!demoMode) {
                 return registrationPOST(req, model, registrationToken, platformRegistrationDTO, toolRegistrationDTO);
            } else {
                model.addAttribute("openid_configuration", openidConfiguration);
                model.addAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);
                model.addAttribute("own_redirect_post_endpoint", localUrl + "/registration/");
                model.addAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDTO);
                model.addAttribute(LtiStrings.TOOL_CONFIGURATION, toolRegistrationDTO);

                return "registrationRedirect";
            }
        } catch (HttpServerErrorException | ConnectionException | JsonProcessingException ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
            model.addAttribute("Error", ex.getMessage());
            return "registrationError";
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String registrationPOST(HttpServletRequest req, Model model) {
        HttpSession session = req.getSession();
        String token = (String) session.getAttribute(LtiStrings.REGISTRATION_TOKEN);
        PlatformRegistrationDTO platformRegistrationDTO = (PlatformRegistrationDTO) session.getAttribute(LtiStrings.PLATFORM_CONFIGURATION);
        ToolRegistrationDTO toolRegistrationDTO = (ToolRegistrationDTO) session.getAttribute(LtiStrings.TOOL_CONFIGURATION);

        return registrationPOST(req, model, token, platformRegistrationDTO, toolRegistrationDTO);
    }

    private String registrationPOST(HttpServletRequest req, Model model, String token, PlatformRegistrationDTO platformRegistrationDTO, ToolRegistrationDTO toolRegistrationDTO) {
        String answer = "Error during the registration";
        try {
            answer = registrationService.callDynamicRegistration(token, toolRegistrationDTO, platformRegistrationDTO.getRegistration_endpoint());
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
        model.addAttribute("registration_confirmation", answer);
        try {
            model.addAttribute("issuer", java.net.URLDecoder.decode(platformRegistrationDTO.getIssuer(), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            log.error("Error decoding the issuer as URL", e);
        }
        if (!demoMode) {
            return "registrationConfirmation";
        } else {
            return "registrationConfirmationDemo";
        }
    }

}
