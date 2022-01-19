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
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This LTI controller should be protected by OAuth 1.0a (on the /oauth path)
 * This will handle LTI 1 and 2 (many of the paths ONLY make sense for LTI2 though)
 * Sample Key "key" and secret "secret"
 */
@Controller
@Scope("session")
@RequestMapping("/oidc")
public class OIDCController {

    static final Logger log = LoggerFactory.getLogger(OIDCController.class);

    //Constants defined in the LTI standard
    private static final String NONE = "none";
    private static final String FORM_POST = "form_post";
    private static final String ID_TOKEN = "id_token";
    private static final String OPEN_ID = "openid";
    private static final String CLIENT_ID = "client_id";
    private static final String DEPLOYMENT_ID = "lti_deployment_id";

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
        List<PlatformDeployment> platformDeploymentListEntityList;
        // Getting the client_id (that is optional) and can come in the form or in the URL.
        String clientIdValue;
        // If we already have it in the loginInitiationDTO
        if (loginInitiationDTO.getClientId() != null) {
            clientIdValue = loginInitiationDTO.getClientId();
        } else {  // We try to get it from the URL query parameters.
            clientIdValue = req.getParameter(CLIENT_ID);
        }
        // Getting the deployment_id (that is optional) and can come in the form or in the URL.
        String deploymentIdValue;
        // If we already have it in the loginInitiationDTO
        if (loginInitiationDTO.getDeploymentId() != null) {
            deploymentIdValue = loginInitiationDTO.getDeploymentId();
        } else {  // We try to get it from the URL query getDeploymentId.
            deploymentIdValue = req.getParameter(DEPLOYMENT_ID);
        }

        // We search for the platformDeployment.
        // We will try all the options here (from more detailed to less), and we will deal with the error if there are more than one result.
        if (clientIdValue != null && deploymentIdValue != null) {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(loginInitiationDTO.getIss(), clientIdValue, deploymentIdValue);
        } else if (clientIdValue != null) {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIssAndClientId(loginInitiationDTO.getIss(), clientIdValue);
        } else if (deploymentIdValue != null) {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIssAndDeploymentId(loginInitiationDTO.getIss(), deploymentIdValue);
        } else {
            platformDeploymentListEntityList = platformDeploymentRepository.findByIss(loginInitiationDTO.getIss());
        }
        // We deal with some possible errors
        if (platformDeploymentListEntityList.isEmpty()) {  //If we don't have configuration
            model.addAttribute(TextConstants.ERROR, "Not found any existing tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);
            return TextConstants.LTI3ERROR;
        }
        if (platformDeploymentListEntityList.size() > 1) {   // If we have more than one match.
            model.addAttribute(TextConstants.ERROR, "We have more than one tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);
            return TextConstants.LTI3ERROR;
        }
        // If we have arrived here, it means that we have only one result (as expected)
        PlatformDeployment lti3KeyEntity = platformDeploymentListEntityList.get(0);
        if (clientIdValue == null) {
            clientIdValue = lti3KeyEntity.getClientId();
        }
        if (deploymentIdValue == null) {
            deploymentIdValue = lti3KeyEntity.getDeploymentId();
        }
        try {
            // We are going to create the OIDC request,
            Map<String, String> parameters = generateAuthRequestPayload(lti3KeyEntity, loginInitiationDTO, clientIdValue, deploymentIdValue);
            // We add that information so the thymeleaf template can display it (and prepare the links)
            //model.addAllAttributes(parameters);
            // These 3 are to display what we received from the platform.
            if (ltiDataService.getDemoMode()) {
                model.addAllAttributes(parameters);
                model.addAttribute("initiation_dto", loginInitiationDTO);
                model.addAttribute("client_id_received", clientIdValue);
                model.addAttribute("deployment_id_received", deploymentIdValue);
            }
            // This can be implemented in different ways, on this case, we are storing the state and nonce in
            // the httpsession, so we can compare later if they are valid states and nonces.
            HttpSession session = req.getSession();
            List<String> stateList;
            List<String> nonceList;
            String state = parameters.get("state");
            String nonce = parameters.get("nonce");

            // We will keep several states and nonces, and we should delete them once we use them.
            if (session.getAttribute("lti_state") != null) {
                List<String> ltiState = (List) session.getAttribute("lti_state");
                if (ltiState.isEmpty()) {  //If not old states... then just the one we have created
                    stateList = new ArrayList<>();
                    stateList.add(state);
                } else if (ltiState.contains(state)) {  //if the state is already there... then the lti_state is the same. No need to add a duplicate
                    stateList = ltiState;
                } else { // if it is a different state and there are more... we add it with the to the string.
                    ltiState.add(state);
                    stateList = ltiState;
                }
            } else {
                stateList = new ArrayList<>();
                stateList.add(state);
            }
            session.setAttribute("lti_state", stateList);

            log.info("lti_state in session: {}", session.getAttribute("lti_state"));
            log.info("Session ID in OIDC Controller: {}", session.getId());

            if (session.getAttribute("lti_nonce") != null) {
                List<String> ltiNonce = (List) session.getAttribute("lti_nonce");
                if (ltiNonce.isEmpty()) {  //If not old nonces... then just the one we have created
                    nonceList = new ArrayList<>();
                    nonceList.add(nonce);
                } else {
                    ltiNonce.add(nonce);
                    nonceList = ltiNonce;
                }
            } else {
                nonceList = new ArrayList<>();
                nonceList.add(nonce);
            }
            session.setAttribute("lti_nonce", nonceList);

            log.info("set-cookie header in oidc controller: {}", res.getHeader("set-cookie"));

            // Once all is added to the session, and we have the data ready for the html template, we redirect
            if (!ltiDataService.getDemoMode()) {
                return "redirect:" + parameters.get("oicdEndpointComplete");
            } else {
                return "oicdRedirect";
            }
        } catch (Exception ex) {
            model.addAttribute(TextConstants.ERROR, ExceptionUtils.getStackTrace(ex));
            return TextConstants.LTI3ERROR;
        }
    }

    /**
     * This generates a map with all the information that we need to send to the OIDC Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     */
    private Map<String, String> generateAuthRequestPayload(PlatformDeployment platformDeployment, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException, IOException {

        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put(CLIENT_ID, platformDeployment.getClientId()); //As it came from the Platform (if it came... if not we should have it configured)
        authRequestMap.put("login_hint", loginInitiationDTO.getLoginHint()); //As it came from the Platform
        authRequestMap.put("lti_message_hint", loginInitiationDTO.getLtiMessageHint()); //As it came from the Platform
        String nonce = UUID.randomUUID().toString(); // We generate a nonce to allow this auth request to be used only one time.
        String nonceHash = Hashing.sha256()
                .hashString(nonce, StandardCharsets.UTF_8)
                .toString();
        authRequestMap.put("nonce", nonce);  //The nonce
        authRequestMap.put("nonce_hash", nonceHash);  //The hash value of the nonce
        authRequestMap.put("prompt", NONE);  //Always this value, as specified in the standard.
        authRequestMap.put("redirect_uri", ltiDataService.getLocalUrl() + TextConstants.LTI3_SUFFIX);  // One of the valid redirect uris.
        authRequestMap.put("response_mode", FORM_POST); //Always this value, as specified in the standard.
        authRequestMap.put("response_type", ID_TOKEN); //Always this value, as specified in the standard.
        authRequestMap.put("scope", OPEN_ID);  //Always this value, as specified in the standard.
        // The state is something that we can create and add anything we want on it.
        // On this case, we have decided to create a JWT token with some information that we will use as additional security. But it is not mandatory.
        String state = LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap, loginInitiationDTO, clientIdValue, deploymentIdValue);
        authRequestMap.put("state", state); //The state we use later to retrieve some useful information about the OICD request.
        authRequestMap.put("oicdEndpoint", platformDeployment.getOidcEndpoint());  //We need this in the Thymeleaf template in case we decide to use the POST method. It is the endpoint where the LMS receives the OICD requests
        authRequestMap.put("oicdEndpointComplete", generateCompleteUrl(authRequestMap));  //This generates the URL to use in case we decide to use the GET method
        return authRequestMap;
    }

    /**
     * This generates the GET URL with all the query string parameters.
     */
    private String generateCompleteUrl(Map<String, String> model) throws UnsupportedEncodingException {
        StringBuilder getUrl = new StringBuilder();

        getUrl.append(model.get("oicdEndpoint"));
        getUrl = addParameter(getUrl, "client_id", model.get(CLIENT_ID), true);
        getUrl = addParameter(getUrl, "login_hint", model.get("login_hint"), false);
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

}
