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
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.RegistrationException;
import net.unicon.lti.model.lti.dto.MessagesSupportedDTO;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationACKDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationDTO;
import net.unicon.lti.model.lti.dto.ToolMessagesSupportedDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.RegistrationService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.unicon.lti.utils.TextConstants.CANVAS_ISSUER;

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

    private RestTemplate restTemplate;

    @Value("${application.url}")
    private String localUrl;

    @Value("${domain.url}")
    private String domainUrl;

    @Value("${application.name}")
    private String clientName;

    @Value("${application.description}")
    private String description;

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
        log.debug(openidConfiguration);
        session.setAttribute(LtiStrings.REGISTRATION_TOKEN, registrationToken);

        try {
            // We are going to create the call the openidconfiguration endpoint,
            restTemplate = restTemplate == null ? new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())) : restTemplate;

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
            } else {
                String exceptionMsg = "Can't get the platform configuration";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            session.setAttribute(LtiStrings.PLATFORM_CONFIGURATION, platformRegistrationDTO);
            ToolRegistrationDTO toolRegistrationDTO = generateToolConfiguration(platformConfiguration.getBody());
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
        } catch (HttpServerErrorException | ConnectionException ex) {
            ex.printStackTrace();
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
        ToolConfigurationACKDTO answer = new ToolConfigurationACKDTO();
        try {
            answer = registrationService.callDynamicRegistration(token, toolRegistrationDTO, platformRegistrationDTO.getRegistration_endpoint());
            //If we are here, then we received a succesful registration and we need to create the database entry
            registrationService.saveNewPlatformDeployment(answer, platformRegistrationDTO);
        } catch (ConnectionException ex) {
            ex.printStackTrace();
            model.addAttribute("Error", ex.getMessage());
            return "registrationError";
        } catch (RegistrationException ex) {
            ex.printStackTrace();
            model.addAttribute("Error", ex.getMessage());
            return "registrationError";
        }

        model.addAttribute("registration_confirmation", answer.toString());
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

    /**
     * This generates a JsonNode with all the information that we need to send to the Registration Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     * @param platformRegistrationDTO
     * @return
     */
    private ToolRegistrationDTO generateToolConfiguration(PlatformRegistrationDTO platformConfiguration) {
        ToolRegistrationDTO toolRegistrationDTO = new ToolRegistrationDTO();
        toolRegistrationDTO.setApplication_type("web");
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("implicit");
        grantTypes.add("client_credentials");
        toolRegistrationDTO.setGrant_types(grantTypes);
        toolRegistrationDTO.setResponse_types(Collections.singletonList("id_token"));
        toolRegistrationDTO.setRedirect_uris(Collections.singletonList(localUrl + TextConstants.LTI3_SUFFIX));
        toolRegistrationDTO.setInitiate_login_uri(localUrl + "/oidc/login_initiations");
        toolRegistrationDTO.setClient_name(clientName);
        toolRegistrationDTO.setJwks_uri(localUrl + "/jwks/jwk");
        //OPTIONAL -->setLogo_uri
        toolRegistrationDTO.setToken_endpoint_auth_method("private_key_jwt");
        //OPTIONAL -->setContacts
        //OPTIONAL -->setClient_uri
        //OPTIONAL -->setTos_uri
        //OPTIONAL -->setPolicy_uri
        ToolConfigurationDTO toolConfigurationDTO = new ToolConfigurationDTO();
        toolConfigurationDTO.setDomain(domainUrl);
        //OPTIONAL -->setSecondary_domains --> Collections.singletonList
        //OPTIONAL -->setDeployment_id

        toolConfigurationDTO.setTarget_link_uri(localUrl + TextConstants.LTI3_SUFFIX);

        //OPTIONAL -->setCustom_parameters --> Map
        toolConfigurationDTO.setDescription(description);
        List<ToolMessagesSupportedDTO> messages = new ArrayList<>();

        // Deep Linking
        ToolMessagesSupportedDTO message1 = new ToolMessagesSupportedDTO();
        message1.setType("LtiDeepLinkingRequest");
        message1.setTarget_link_uri(localUrl + TextConstants.LTI3_SUFFIX);
        message1.setLabel(clientName); // required not null for Canvas, optional otherwise
        message1.setIcon_uri(""); // required not null for Canvas, optional otherwise
        if (platformConfiguration.getPlatformConfiguration() != null && platformConfiguration.getPlatformConfiguration().getMessages_supported() != null) {
            MessagesSupportedDTO ltiDeepLinkingPlatformMessagesSupported = platformConfiguration.getPlatformConfiguration().getMessages_supported().stream()
                    .filter(messagesSupported -> messagesSupported.getType().equals("LtiDeepLinkingRequest")).findFirst().orElse(null);
            if (ltiDeepLinkingPlatformMessagesSupported != null && ltiDeepLinkingPlatformMessagesSupported.getPlacements() != null) {
                message1.setPlacements(Arrays.asList(ltiDeepLinkingPlatformMessagesSupported.getPlacements().stream()
                        .filter(placement -> placement.contains("link_selection") || placement.contains("ContentArea")) // link_selection for Canvas, ContentArea for D2L
                        .findFirst()
                        .orElse("")));
            }
        }
//        OPTIONAL: --> message1 --> setCustom_parameters
        messages.add(message1);

        ToolMessagesSupportedDTO message2 = new ToolMessagesSupportedDTO();
        message2.setType("LtiResourceLinkRequest");
        message2.setTarget_link_uri(localUrl + TextConstants.LTI3_SUFFIX);
        message2.setLabel(clientName); // required not null for Canvas, optional otherwise
        message2.setIcon_uri(""); // required not null for Canvas, optional otherwise
        if (platformConfiguration.getPlatformConfiguration() != null && platformConfiguration.getPlatformConfiguration().getMessages_supported() != null) {
            MessagesSupportedDTO ltiResourceLinkPlatformMessagesSupported = platformConfiguration.getPlatformConfiguration().getMessages_supported().stream()
                    .filter(messagesSupported -> messagesSupported.getType().equals("LtiResourceLinkRequest")).findFirst().orElse(null);
            if (ltiResourceLinkPlatformMessagesSupported != null && ltiResourceLinkPlatformMessagesSupported.getPlacements() != null) {
                List<String> placements = ltiResourceLinkPlatformMessagesSupported.getPlacements().stream()
                        .filter(placement -> placement.contains("course_navigation"))
                        .findFirst()
                        .map(Collections::singletonList)
                        .orElse(Collections.singletonList(""));

                // Check if placements contains only a blank string and set it to null if true
                if (placements.size() == 1 && StringUtils.isBlank(placements.get(0))) {
                    placements = null;
                }

                message2.setPlacements(placements);
            }
        }
        messages.add(message2);
        toolConfigurationDTO.setMessages_supported(messages);

        Set<String> platformAndOptionalClaims = new LinkedHashSet<>(platformConfiguration.getClaims_supported());
        platformAndOptionalClaims.addAll(LtiStrings.LTI_OPTIONAL_CLAIMS);
        List<String> toolConfigurationClaims = new ArrayList<>(platformAndOptionalClaims);
        toolConfigurationDTO.setClaims(toolConfigurationClaims);
        if (StringUtils.equals(platformConfiguration.getIssuer(), CANVAS_ISSUER)) { // Canvas does not use claims field for privacy settings as of 3/12/2024
            toolConfigurationDTO.setCanvasPrivacyLevel("public");
        }

        toolRegistrationDTO.setToolConfiguration(toolConfigurationDTO);
        toolRegistrationDTO.setScope(StringUtils.join(platformConfiguration.getScopes_supported(), " "));

        return toolRegistrationDTO;
    }

}