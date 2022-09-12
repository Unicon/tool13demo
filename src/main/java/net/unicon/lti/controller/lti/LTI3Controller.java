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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiLinkEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.LtiLinkRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.harmony.HarmonyService;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@Scope("session")
public class LTI3Controller {
    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    AdvantageAGSService advantageAGSService;

    @Autowired
    HarmonyService harmonyService;

    LTI3Request lti3Request;

    private CloseableHttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();

    @PostMapping(value={"/lti3","/lti3/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String lti3(HttpServletRequest req, HttpServletResponse res, Model model)  {
        //First we will get the state, validate it
        String state = req.getParameter("state");
        //We will use this link to find the content to display.
        String link = req.getParameter("link");

        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            lti3Request = LTI3Request.getInstance(link); // validates nonce & id_token
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the state (if sent in OIDC initiation request) matches the one coming with the ltiRequest.
            String clientIdFromState = claims.getBody().get("clientId") != null ? claims.getBody().get("clientId").toString() : null;
            if (clientIdFromState != null && !clientIdFromState.equals(lti3Request.getAud())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client_id");
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the state (if sent in the OIDC initiation request) matches the one coming with the ltiRequest.
            String deploymentIdFromState = claims.getBody().get("ltiDeploymentId") != null ? claims.getBody().get("ltiDeploymentId").toString() : null;
            if (deploymentIdFromState != null && !deploymentIdFromState.equals(lti3Request.getLtiDeploymentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid deployment_id");
            }

            // Convert id_token to be signed by middleware so that Harmony can validate it
            String middlewareIdToken = LtiOidcUtils.generateLtiToken(lti3Request, ltiDataService);

            PlatformDeployment platformDeployment = ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(lti3Request.getIss(), lti3Request.getAud(), lti3Request.getLtiDeploymentId()).get(0);
            LtiContextEntity ltiContext = Objects.requireNonNull(
                    ltiDataService.getRepos().contexts.findByContextKeyAndPlatformDeployment(lti3Request.getLtiContextId(), platformDeployment),
                    "LTI context should exist for iss " + lti3Request.getIss() + ", client_id " + lti3Request.getAud() + ", and deployment_id " + lti3Request.getLtiDeploymentId());

            // Sync lineitems and update db if needed
            if (!ltiDataService.getDemoMode()) {
                // check if lti resource link and it is a new lti_context or lineitems are out of sync
                boolean lineitemsAlreadySynced = ltiContext.getLineitemsSynced() != null && ltiContext.getLineitemsSynced();
                if (!lineitemsAlreadySynced) {
                    // fetch lineitems from ags
                    log.debug("Attempting to fetch lineitems from the LMS...");
                    LineItems lineItems = advantageAGSService.getLineItems(platformDeployment, ltiContext.getLineitems());

                    // if there are lineitems in the LMS, sync them to Harmony
                    if (lineItems != null && lineItems.getLineItemList() != null && !lineItems.getLineItemList().isEmpty()) {
                        log.debug("Attempting to send lineitems to Harmony...");
                        ResponseEntity<Map> harmonyLineitemsResponse = harmonyService.postLineitemsToHarmony(lineItems, middlewareIdToken);

                        // if no exceptions were thrown and root_outcome_guid received, set lineitems synced to true for the context
                        if (harmonyLineitemsResponse != null && harmonyLineitemsResponse.getStatusCode().is2xxSuccessful()) {
                            Map<String, String> rogMap = harmonyLineitemsResponse.getBody();
                            String rootOutcomeGuid = rogMap.get("root_outcome_guid");
                            if (StringUtils.isNotBlank(rootOutcomeGuid)) {
                                log.info("{} lineitems have been synced to Harmony successfully for iss {}, client_id {}, deployment_id {}, and LMS context_id {}. We received root_outcome_guid {} from Harmony.",
                                        lineItems.getLineItemList().size(), lti3Request.getIss(), lti3Request.getAud(), lti3Request.getLtiDeploymentId(), ltiContext.getContextKey(), rootOutcomeGuid);
                                if (StringUtils.isBlank(ltiContext.getRootOutcomeGuid())) {
                                    log.info("This is a copied course. Setting root_outcome_guid to {}", rootOutcomeGuid);
                                    ltiContext.setRootOutcomeGuid(rootOutcomeGuid);
                                }
                                ltiContext.setLineitemsSynced(true);
                                ltiDataService.getRepos().contexts.save(ltiContext);
                            } else {
                                log.error("Harmony lineitems API did not return root_outcome_guid");
                                model.addAttribute("Error", "Harmony Lineitems API returned " + harmonyLineitemsResponse.getStatusCode() + "\n" + harmonyLineitemsResponse.getBody());
                                return "lti3Error";
                            }
                        } else {
                            log.error("Harmony Lineitems API returned {}", harmonyLineitemsResponse.getStatusCode());
                            log.error(String.valueOf(harmonyLineitemsResponse.getBody()));
                            model.addAttribute("Error", "Harmony Lineitems API returned " + harmonyLineitemsResponse.getStatusCode() + "\n" + harmonyLineitemsResponse.getBody());
                            return "lti3Error";
                        }
                    } else {
                        log.info("No lineitems found in the LMS for iss {}, client_id {}, deployment_id {}, LMS context_id {}, and lineitems URL {}.",
                                lti3Request.getIss(), lti3Request.getAud(), lti3Request.getLtiDeploymentId(), ltiContext.getContextKey(), ltiContext.getLineitems());
                    }
                } else {
                    log.info("Lineitems are already in sync and will not be synced again at this time");
                }

                // Setup data for the frontend
                String target = lti3Request.getLtiTargetLinkUrl();
                log.debug("Target Link URL: {}", target);

                model.addAttribute("target", target);
                model.addAttribute("id_token", middlewareIdToken);
                model.addAttribute("state", state);
                model.addAttribute("platform_family_code", lti3Request.getLtiToolPlatformFamilyCode());
            } else {
                model.addAttribute("target", ltiDataService.getLocalUrl() + "/demo?link=" + link);
                return "lti3Redirect";
            }

            // When the LTI message type is deep linking we must to display the React UI to select courses from harmony. 
            if (LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(lti3Request.getLtiMessageType())) {
                if (ltiDataService.getDeepLinkingEnabled()) {
                    // Send the relevant LTI attributes to the frontend
                    model.addAttribute("deploymentId", deploymentIdFromState);
                    model.addAttribute("clientId", clientIdFromState);
                    model.addAttribute("iss", lti3Request.getIss());
                    model.addAttribute("context", lti3Request.getLtiContextId());
                    model.addAttribute("root_outcome_guid", ltiContext.getRootOutcomeGuid());
                    log.debug("Deep Linking menu opening for iss: {}, client_id: {}, deployment_id: {}, context: {}, and root_outcome_guid: {}",
                            lti3Request.getIss(), clientIdFromState, deploymentIdFromState, lti3Request.getLtiContextId(), ltiContext.getRootOutcomeGuid());

                    // This redirects to the REACT UI which is a secondary set of templates.
                    return TextConstants.REACT_UI_TEMPLATE;
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deep Linking Disabled");
                }
            }

            return "lti3Redirect";

        } catch (SignatureException e) {
            log.error("Invalid Signature: {}", e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        } catch (ConnectionException e) {
            log.error("Could not fetch lineitems to sync with harmony: {}", e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not fetch lineitems to sync with Harmony");
        } catch (GeneralSecurityException e) {
            log.error("Error: {}", e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error");
        } catch (JsonProcessingException | DataServiceException e) {
            log.error("HarmonyService could not receive lineitems: {}", e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Harmony could not receive lineitems");
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage() + "\n Stack Trace: " + Arrays.toString(e.getStackTrace()));
        }
    }

    @RequestMapping("/demo")
    public String demo(HttpServletRequest req, Model model) {
        String link = req.getParameter("link");
        model.addAttribute("lTI3Request", lti3Request);
        if (link == null) {
            link = lti3Request.getLtiTargetLinkUrl().substring(lti3Request.getLtiTargetLinkUrl().lastIndexOf("?link=") + 6);
        }
        if (StringUtils.isNotBlank(link)) {
            List<LtiLinkEntity> linkEntity = ltiLinkRepository.findByLinkKeyAndContext(link, lti3Request.getContext());
            log.debug("Searching for link " + link + " in the context Key " + lti3Request.getContext().getContextKey() + " And id " + lti3Request.getContext().getContextId());
            if (linkEntity.size() > 0) {
                model.addAttribute(TextConstants.HTML_CONTENT, linkEntity.get(0).createHtmlFromLink());
            } else {
                model.addAttribute(TextConstants.HTML_CONTENT, "<b> No element was found for that context and linkKey</b>");
            }
        } else {
            model.addAttribute(TextConstants.HTML_CONTENT, "<b> No element was requested or it doesn't exists </b>");
        }
        if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING) && ltiDataService.getDeepLinkingEnabled()) {
            //Let's create the LtiLinkEntity's in our database
            //This should be done AFTER the user selects the link in the content selector, and we are doing it before
            //just to keep it simple. The ideal process would be, the user selects a link, sends it to the platform and
            // we create the LtiLinkEntity in our code after that.
            LtiLinkEntity ltiLinkEntity = new LtiLinkEntity("1234", lti3Request.getContext(), "My Test Link");
            if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity.getLinkKey(), ltiLinkEntity.getContext()).size() == 0) {
                ltiLinkRepository.save(ltiLinkEntity);
            }
            LtiLinkEntity ltiLinkEntity2 = new LtiLinkEntity("4567", lti3Request.getContext(), "Another Link");
            if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity2.getLinkKey(), ltiLinkEntity2.getContext()).size() == 0) {
                ltiLinkRepository.save(ltiLinkEntity2);
            }
            return "lti3DeepLink";
        } else if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deep Linking Disabled");
        }
        return "lti3Result";
    }

}
