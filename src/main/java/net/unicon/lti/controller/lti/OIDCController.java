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

import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.unicon.lti.utils.LtiStrings.OIDC_CLIENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_FORM_POST;
import static net.unicon.lti.utils.LtiStrings.OIDC_ID_TOKEN;
import static net.unicon.lti.utils.LtiStrings.OIDC_NONE;
import static net.unicon.lti.utils.LtiStrings.OIDC_OPEN_ID;
import static net.unicon.lti.utils.TextConstants.LTI_NONCE_COOKIE_NAME;
import static net.unicon.lti.utils.TextConstants.LTI_STATE_COOKIE_NAME;

/**
 * This LTI controller should be protected by OAuth 1.0a (on the /oauth path)
 * This will handle LTI 1 and 2 (many of the paths ONLY make sense for LTI2 though)
 * Sample Key "key" and secret "secret"
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/oidc")
public class OIDCController {
    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    LTIDataService ltiDataService;

    /**
     * This will receive the request to start the OIDC process.
     * We receive some parameters (iss, login_hint, target_link_uri, lti_message_hint, and optionally, the deployment_id and the client_id)
     */
    @RequestMapping("/login_initiations")
    public String loginInitiations(HttpServletRequest req, HttpServletResponse res, Model model) {

        // We need to receive the parameters and search for the deployment of the tool that matches with what we receive.
        LoginInitiationDTO loginInitiationDTO = new LoginInitiationDTO(req);
        List<PlatformDeployment> platformDeploymentList;
        // Getting the client_id (that is optional) and can come in the form or in the URL.
        String clientIdValue = loginInitiationDTO.getClientId();
        // Getting the deployment_id (that is optional) and can come in the form or in the URL.
        String deploymentIdValue = loginInitiationDTO.getDeploymentId();

        // We search for the platformDeployment.
        // We will try all the options here (from more detailed to less), and we will deal with the error if there are more than one result.
        if (clientIdValue != null && deploymentIdValue != null) {
            platformDeploymentList = platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(loginInitiationDTO.getIss(), clientIdValue, deploymentIdValue);
        } else if (clientIdValue != null) {
            platformDeploymentList = platformDeploymentRepository.findByIssAndClientId(loginInitiationDTO.getIss(), clientIdValue);
        } else if (deploymentIdValue != null) {
            platformDeploymentList = platformDeploymentRepository.findByIssAndDeploymentId(loginInitiationDTO.getIss(), deploymentIdValue);
        } else {
            platformDeploymentList = platformDeploymentRepository.findByIss(loginInitiationDTO.getIss());
        }
        // We deal with some possible errors
        if (platformDeploymentList.isEmpty()) {  //If we don't have configuration
            model.addAttribute(TextConstants.ERROR, "Not found any existing tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);
            return TextConstants.LTI3ERROR;
        }
        PlatformDeployment platformDeployment = platformDeploymentList.get(0);
        if (platformDeploymentList.size() == 1 && (clientIdValue == null || deploymentIdValue == null)) {
            // If there is only one result, we know what the clientId and deploymentId will be
            if (clientIdValue == null) {
                clientIdValue = platformDeployment.getClientId();
            }
            if (deploymentIdValue == null) {
                deploymentIdValue = platformDeployment.getDeploymentId();
            }
        }

        try {
            // We are going to create the OIDC request,
            Map<String, String> parameters = generateAuthRequestPayload(loginInitiationDTO, clientIdValue, deploymentIdValue, platformDeployment.getOidcEndpoint());
            // We add that information so the thymeleaf template can display it (and prepare the links)
            //model.addAllAttributes(parameters);
            // These 3 are to display what we received from the platform.
            if (ltiDataService.getDemoMode()) {
                model.addAllAttributes(parameters);
                model.addAttribute("initiation_dto", loginInitiationDTO);
                model.addAttribute("client_id_received", clientIdValue);
                model.addAttribute("deployment_id_received", deploymentIdValue);
            }

            // This can be implemented in different ways. In this case, we are storing the state and nonce in
            // cookies, so we can compare later if they are valid.
            res.addCookie(generateLtiOidcCookie(LTI_STATE_COOKIE_NAME, parameters.get("state")));
            res.addCookie(generateLtiOidcCookie(LTI_NONCE_COOKIE_NAME, parameters.get("nonce")));

            // Once all is added to the session, and we have the data ready for the html template, we redirect
            if (!ltiDataService.getDemoMode()) {
                return "redirect:" + parameters.get("oicdEndpointComplete");
            } else {
                return "oicdRedirect";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute(TextConstants.ERROR, ExceptionUtils.getStackTrace(ex));
            return TextConstants.LTI3ERROR;
        }
    }

    /**
     * This generates a map with all the information that we need to send to the OIDC Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     */
    private Map<String, String> generateAuthRequestPayload(LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue, String oidcEndpoint) throws GeneralSecurityException, IOException {
        Map<String, String> authRequestMap = new HashMap<>();
        if (clientIdValue != null) {
            authRequestMap.put(OIDC_CLIENT_ID, clientIdValue); //As it came from the Platform (if it came or was found in config)
        }
        authRequestMap.put("login_hint", loginInitiationDTO.getLoginHint()); //As it came from the Platform
        authRequestMap.put("lti_message_hint", loginInitiationDTO.getLtiMessageHint()); //As it came from the Platform
        String nonce = UUID.randomUUID().toString(); // We generate a nonce to allow this auth request to be used only one time.
        String nonceHash = Hashing.sha256()
                .hashString(nonce, StandardCharsets.UTF_8)
                .toString();
        authRequestMap.put("nonce", nonce);  //The nonce
        authRequestMap.put("nonce_hash", nonceHash);  //The hash value of the nonce
        authRequestMap.put("prompt", OIDC_NONE);  //Always this value, as specified in the standard.
        authRequestMap.put("redirect_uri", ltiDataService.getLocalUrl() + TextConstants.LTI3_SUFFIX);  // One of the valid redirect uris.
        authRequestMap.put("response_mode", OIDC_FORM_POST); //Always this value, as specified in the standard.
        authRequestMap.put("response_type", OIDC_ID_TOKEN); //Always this value, as specified in the standard.
        authRequestMap.put("scope", OIDC_OPEN_ID);  //Always this value, as specified in the standard.
        // The state is something that we can create and add anything we want on it.
        // On this case, we have decided to create a JWT token with some information that we will use as additional security. But it is not mandatory.
        String state = LtiOidcUtils.generateState(ltiDataService, authRequestMap, loginInitiationDTO, clientIdValue, deploymentIdValue);
        authRequestMap.put("state", state); //The state we use later to retrieve some useful information about the OICD request.
        authRequestMap.put("oicdEndpoint", oidcEndpoint);  //We need this in the Thymeleaf template in case we decide to use the POST method. It is the endpoint where the LMS receives the OICD requests
        authRequestMap.put("oicdEndpointComplete", generateCompleteUrl(authRequestMap));  //This generates the URL to use in case we decide to use the GET method
        return authRequestMap;
    }

    /**
     * This generates the GET URL with all the query string parameters.
     */
    private String generateCompleteUrl(Map<String, String> model) throws UnsupportedEncodingException {
        StringBuilder getUrl = new StringBuilder();

        getUrl.append(model.get("oicdEndpoint"));
        if (model.get(OIDC_CLIENT_ID) != null) {
            getUrl = addParameter(getUrl, "client_id", model.get(OIDC_CLIENT_ID), true);
            getUrl = addParameter(getUrl, "login_hint", model.get("login_hint"), false);
        } else {
            getUrl = addParameter(getUrl, "login_hint", model.get("login_hint"), true);
        }
        getUrl = addParameter(getUrl, "lti_message_hint", model.get("lti_message_hint"), false);
        getUrl = addParameter(getUrl, "nonce", model.get("nonce_hash"), false);
        getUrl = addParameter(getUrl, "prompt", model.get("prompt"), false);
        getUrl = addParameter(getUrl, "redirect_uri", model.get("redirect_uri"), false);
        getUrl = addParameter(getUrl, "response_mode", model.get("response_mode"), false);
        getUrl = addParameter(getUrl, "response_type", model.get("response_type"), false);
        getUrl = addParameter(getUrl, "scope", model.get("scope"), false);
        getUrl = addParameter(getUrl, "state", model.get("state"), false);
        return getUrl.toString();
    }

    private StringBuilder addParameter(StringBuilder url, String parameter, String value, boolean first) throws UnsupportedEncodingException {
        if (value != null) {
            if (first) {
                url.append("?").append(parameter).append("=");
            } else {
                url.append("&").append(parameter).append("=");
            }
            url.append(URLEncoder.encode(value, String.valueOf(StandardCharsets.UTF_8)));
        }
        return url;
    }

    private Cookie generateLtiOidcCookie(String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        log.debug(name + " cookie in OIDC Controller: {}", cookie.getValue());
        return cookie;
    }

}
