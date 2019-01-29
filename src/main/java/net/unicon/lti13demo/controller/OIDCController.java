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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    LTIDataService ltiDataService;

    /**
     * This will receive the request to start the OIDC process.
     * We receive some parameters in the url (iss, login_hint, target_link_uri, lti_message_hint)
     * @param req
     * @param model
     * @return
     */
    @RequestMapping("/login_initiations")
    public String loginInitiations(HttpServletRequest req, Model model) {

        LoginInitiationDTO loginInitiationDTO = new LoginInitiationDTO(req);
        // Search for the configuration for that issuer
        List<PlatformDeployment> platformDeploymentListEntityList = platformDeploymentRepository.findByIss(loginInitiationDTO.getIss());
        // We deal with some possible errors
        if (platformDeploymentListEntityList.isEmpty()) {  //If we don't have configuration
            model.addAttribute("error_type","iss_nonexisting");
            return "error";
        } else {
            // If we have more than one configuration for the same iss, at this moment we don't know about the client id, so we just pick the first one
            PlatformDeployment lti3KeyEntity = platformDeploymentListEntityList.get(0);
            try {
                Map<String, String> parameters = generateAuthRequestPayload(lti3KeyEntity, loginInitiationDTO);
                model.addAllAttributes(parameters);
                model.addAttribute("initiation_dto", loginInitiationDTO);
                HttpSession session = req.getSession();
                List<String> stateList;
                List<String> nonceList;
                String state = parameters.get("state");
                String nonce = parameters.get("nonce");

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
                return "oicdRedirect";
            } catch (Exception ex) {
                model.addAttribute("Error", ex.getMessage());
                return "lti3Error";
            }
        }
    }

    /**
     * This generates a map with all the information that we need to send to the OIDC Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     * @param platformDeployment
     * @param loginInitiationDTO
     * @return
     */
    private Map<String, String> generateAuthRequestPayload (PlatformDeployment platformDeployment, LoginInitiationDTO loginInitiationDTO) throws  GeneralSecurityException, IOException{

        Map<String, String> authRequestMap =  new HashMap<>();
        authRequestMap.put("client_id", platformDeployment.getClientId()); //As it came from the Platform
        authRequestMap.put("login_hint",loginInitiationDTO.getLoginHint()); //As it came from the Platform
        authRequestMap.put("lti_message_hint",loginInitiationDTO.getLtiMessageHint()); //As it came from the Platform
        String nonce = UUID.randomUUID().toString();
        String nonceHash = Hashing.sha256()
                .hashString(nonce, StandardCharsets.UTF_8)
                .toString();
        authRequestMap.put("nonce", nonce);  //Just a nonce
        authRequestMap.put("nonce_hash", nonceHash);  //Just a nonce
        authRequestMap.put("prompt", none);  //Always this value
        authRequestMap.put("redirect_uri",loginInitiationDTO.getTargetLinkUri());  //As it came from the Platform
        authRequestMap.put("response_mode", formPost); //Always this value
        authRequestMap.put("response_type", idToken); //Always this value
        authRequestMap.put("scope", openId);  //Always this value
        String state = LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap,loginInitiationDTO);
        authRequestMap.put("state",state); //The state we use later to retrieve some useful information about the OICD request.
        authRequestMap.put("oicdEndpoint", platformDeployment.getOidcEndpoint());  //For the post
        authRequestMap.put("oicdEndpointComplete",generateCompleteUrl(authRequestMap));  //For the GET with all the query string parameters
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
