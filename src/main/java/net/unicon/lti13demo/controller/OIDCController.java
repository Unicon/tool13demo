/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.controller;

import com.google.common.hash.Hashing;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.dto.LoginInitiationDTO;
import net.unicon.lti13demo.repository.PlatformDeploymentRepository;
import net.unicon.lti13demo.service.LTIDataService;
import net.unicon.lti13demo.utils.lti.LtiOidcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
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
    private final static String none = "none";
    private final static String formPost = "form_post";
    private final static String idToken = "id_token";
    private final static String openId = "openid";
    private final static String clientId = "client_id";
    private final static String deploymentId = "lti_deployment_id";

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    LTIDataService ltiDataService;

    /**
     * This will receive the request to start the OIDC process.
     * We receive some parameters (iss, login_hint, target_link_uri, lti_message_hint, and optionally, the deployment_id and the client_id)
     * @param req
     * @param model
     * @return
     */
    @RequestMapping("/login_initiations")
    public String loginInitiations(HttpServletRequest req, Model model) {

        // We need to receive the parameters and search for the deployment of the tool that matches with what we receive.
        LoginInitiationDTO loginInitiationDTO = new LoginInitiationDTO(req);
        List<PlatformDeployment> platformDeploymentListEntityList = new ArrayList<>();
        // Getting the client_id (that is optional) and can come in the form or in the URL.
        String clientIdValue = null;
        // If we already have it in the loginInitiationDTO
        if (loginInitiationDTO.getClientId()!= null){
            clientIdValue = loginInitiationDTO.getClientId();
        } else {  // We try to get it from the URL query parameters.
            clientIdValue = req.getParameter(clientId);
        }
        // Getting the deployment_id (that is optional) and can come in the form or in the URL.
        String deploymentIdValue = null;
        // If we already have it in the loginInitiationDTO
        if (loginInitiationDTO.getDeploymentId()!= null){
            deploymentIdValue = loginInitiationDTO.getDeploymentId();
        } else {  // We try to get it from the URL query getDeploymentId.
            deploymentIdValue = req.getParameter(deploymentId);
        }

        // We search for the platformDeployment.
        // We will try all the options here (from more detailed to less), and we will deal with the error if there are more than one result.
        if (clientIdValue != null && deploymentIdValue !=null){
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
            model.addAttribute("Error","Not found any existing tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);
            return "lti3Error";
        }
        if (platformDeploymentListEntityList.size()>1) {   // If we have more than one match.
            model.addAttribute("Error","We have more than one tool deployment with iss: " + loginInitiationDTO.getIss() +
                    " clientId: " + clientIdValue + " deploymentId: " + deploymentIdValue);
            return "lti3Error";
        }
        // If we have arrived here, it means that we have only one result (as expected)
        PlatformDeployment lti3KeyEntity = platformDeploymentListEntityList.get(0);
        if (clientIdValue == null) {
            clientIdValue = lti3KeyEntity.getClientId();
        }
        if (deploymentIdValue == null) {
            deploymentIdValue =lti3KeyEntity.getDeploymentId();
        }
        try {
            // We are going to create the OIDC request,
            Map<String, String> parameters = generateAuthRequestPayload(lti3KeyEntity, loginInitiationDTO, clientIdValue, deploymentIdValue);
            // We add that information so the thymeleaf template can display it (and prepare the links)
            model.addAllAttributes(parameters);
            // These 3 are to display what we received from the platform.
            model.addAttribute("initiation_dto", loginInitiationDTO);
            model.addAttribute("client_id_received", clientIdValue);
            model.addAttribute("deployment_id_received", deploymentIdValue);
            // This can be implemented in different ways, on this case, we are storing the state and nonce in
            // the httpsession, so we can compare later if they are valid states and nonces.
            HttpSession session = req.getSession();
            List<String> stateList;
            List<String> nonceList;
            String state = parameters.get("state");
            String nonce = parameters.get("nonce");

            // We will keep several states and nonces, and we should delete them once we use them.
            if (session.getAttribute("lti_state") != null) {
                List<String> ltiState = (List)session.getAttribute("lti_state");
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

            if (session.getAttribute("lti_nonce") != null) {
                List<String> ltiNonce = (List)session.getAttribute("lti_nonce");
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
            // Once all is added to the session, and we have the data ready for the html template, we redirect
            return "oicdRedirect";
        } catch (Exception ex) {
            model.addAttribute("Error", ex.getMessage());
            return "lti3Error";
        }
    }

    /**
     * This generates a map with all the information that we need to send to the OIDC Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     * @param platformDeployment
     * @param loginInitiationDTO
     * @return
     */
    private Map<String, String> generateAuthRequestPayload (PlatformDeployment platformDeployment, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws  GeneralSecurityException, IOException{

        Map<String, String> authRequestMap =  new HashMap<>();
        authRequestMap.put("client_id", platformDeployment.getClientId()); //As it came from the Platform (if it came... if not we should have it configured)
        authRequestMap.put("login_hint",loginInitiationDTO.getLoginHint()); //As it came from the Platform
        authRequestMap.put("lti_message_hint",loginInitiationDTO.getLtiMessageHint()); //As it came from the Platform
        String nonce = UUID.randomUUID().toString(); // We generate a nonce to allow this auth request to be used only one time.
        String nonceHash = Hashing.sha256()
                .hashString(nonce, StandardCharsets.UTF_8)
                .toString();
        authRequestMap.put("nonce", nonce);  //The nonce
        authRequestMap.put("nonce_hash", nonceHash);  //The hash value of the nonce
        authRequestMap.put("prompt", none);  //Always this value, as specified in the standard.
        authRequestMap.put("redirect_uri",ltiDataService.getLocalUrl() + "/lti3");  // One of the valids reditect uris.
        authRequestMap.put("response_mode", formPost); //Always this value, as specified in the standard.
        authRequestMap.put("response_type", idToken); //Always this value, as specified in the standard.
        authRequestMap.put("scope", openId);  //Always this value, as specified in the standard.
        // The state is something that we can create and add anything we want on it.
        // On this case, we have decided to create a JWT token with some information that we will use as additional security. But it is not mandatory.
        String state = LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap,loginInitiationDTO, clientIdValue, deploymentIdValue);
        authRequestMap.put("state",state); //The state we use later to retrieve some useful information about the OICD request.
        authRequestMap.put("oicdEndpoint", platformDeployment.getOidcEndpoint());  //We need this in the Thymeleaf template in case we decide to use the POST method. It is the endpoint where the LMS receives the OICD requests
        authRequestMap.put("oicdEndpointComplete",generateCompleteUrl(authRequestMap));  //This generates the URL to use in case we decide to use the GET method
        return authRequestMap;
    }

    /**
     * This generates the GET URL with all the query string parameters.
     * @param model
     * @return
     */
    private String generateCompleteUrl(Map<String, String> model) {
        return new StringBuilder()
                .append(model.get("oicdEndpoint"))
                .append("?client_id=")
                .append(model.get("client_id"))
                .append("&login_hint=")
                .append(model.get("login_hint"))
                .append("&lti_message_hint=")
                .append(model.get("lti_message_hint"))
                .append("&nonce=")
                .append(model.get("nonce_hash"))
                .append("&prompt=")
                .append(model.get("prompt"))
                .append("&redirect_uri=")
                .append(model.get("redirect_uri"))
                .append("&response_mode=")
                .append(model.get("response_mode"))
                .append("&response_type=")
                .append(model.get("response_type"))
                .append("&scope=")
                .append(model.get("scope"))
                .append("&state=")
                .append(model.get("state")).toString();
    }

}
