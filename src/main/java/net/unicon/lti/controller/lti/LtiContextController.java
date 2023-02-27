package net.unicon.lti.controller.lti;

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import net.unicon.lti.model.harmony.HarmonyFetchDeepLinksBody;
import net.unicon.lti.model.lti.dto.DeepLinkingContentItemDTO;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.harmony.HarmonyService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.lti.DeepLinkUtils;
import net.unicon.lti.utils.lti.LTI3Request;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Controller
@Scope("session")
@RequestMapping("/context")
public class LtiContextController {

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    HarmonyService harmonyService;

    @PutMapping
    ResponseEntity<Object> prepareDeepLinkingResponse(@RequestBody HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody, @RequestParam(required = false, value = "lti_storage_target") String ltiStorageTarget) {
        try {
            if (StringUtils.isEmpty(harmonyFetchDeepLinksBody.getRootOutcomeGuid())) {
                log.error("Root outcome guid cannot be null when preparing deep linking response");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request");
            }
            if (StringUtils.isEmpty(harmonyFetchDeepLinksBody.getIdToken())) {
                log.error("id_token cannot be null when preparing deep linking response");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request");
            }

            // validate id_token, including proving existence of single platformDeployment, generate id_token object
            LTI3Request lti3Request = LTI3Request.makeLTI3Request(ltiDataService, true, null, harmonyFetchDeepLinksBody.getIdToken(), ltiStorageTarget);

            // Retrieve LMS config from db to be used to find the right context
            List<PlatformDeployment> platformDeploymentList = platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(lti3Request.getIss(), lti3Request.getAud(), lti3Request.getLtiDeploymentId());
            PlatformDeployment platformDeployment = platformDeploymentList.get(0);

            // Retrieve context from db
            LtiContextEntity ltiContext = ltiContextRepository.findByContextKeyAndPlatformDeployment(lti3Request.getLtiContextId(), platformDeployment);

            // Update context to have rootOutcomeGuid value
            if (ltiContext != null) {
                // Check if course already paired
                boolean coursePaired = StringUtils.isNotBlank(ltiContext.getRootOutcomeGuid());

                // Retrieve and validate links from Harmony
                List<HarmonyContentItemDTO> harmonyContentItems = harmonyService.fetchDeepLinkingContentItems(harmonyFetchDeepLinksBody.getRootOutcomeGuid(), harmonyFetchDeepLinksBody.getIdToken(), coursePaired, harmonyFetchDeepLinksBody.getModuleIds());

                // Generate Deep Link Response JWT
                if (harmonyContentItems != null && !harmonyContentItems.isEmpty()) {
                    List<DeepLinkingContentItemDTO> deepLinkingContentItems = new ArrayList<>();
                    harmonyContentItems.forEach((harmonyContentItem) -> deepLinkingContentItems.add(harmonyContentItem.toDeepLinkingContentItem()));

                    String deepLinkingResponseJwt = DeepLinkUtils.generateDeepLinkingResponseJWT(ltiDataService, lti3Request, deepLinkingContentItems);
                    log.debug("Generated Deep Linking Response JWT: {}", deepLinkingResponseJwt);

                    if (!StringUtils.isAnyBlank(deepLinkingResponseJwt, lti3Request.getDeepLinkReturnUrl())) {
                        // Pair book to lti context before submitting deep linking response
                        ltiContext.setRootOutcomeGuid(harmonyFetchDeepLinksBody.getRootOutcomeGuid());
                        ltiContext.setLineitemsSynced(false);
                        ltiContextRepository.save(ltiContext);
                        log.debug("Set lineitems_synced to false and paired lti context {} for iss {} client_id {} and deployment_id {} with root_outcome_guid {}",
                                ltiContext.getContextId(), lti3Request.getIss(), lti3Request.getAud(), lti3Request.getLtiDeploymentId(), harmonyFetchDeepLinksBody.getRootOutcomeGuid());

                        // Send deep_link_return_url and JWT to front end
                        HashMap<String, String> response = new HashMap<>();
                        response.put("deep_link_return_url", lti3Request.getDeepLinkReturnUrl());
                        response.put("JWT", deepLinkingResponseJwt);
                        log.debug("Response generated: {}", response);

                        return ResponseEntity.ok(response);
                    } else {
                        log.error("Deep Link Return URL was {} and deepLinkingResponseJwt was {}", lti3Request.getDeepLinkReturnUrl(), deepLinkingResponseJwt);
                        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Could not generate response");
                    }
                } else {
                    log.error("No deep links fetched");
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Error communicating with Harmony");
                }
            } else {
                log.error("ltiContext was null");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Could not find LMS course context");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Exception thrown");
        }
    }
}
