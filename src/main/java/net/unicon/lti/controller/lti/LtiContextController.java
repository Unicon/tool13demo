package net.unicon.lti.controller.lti;

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.lti.LTI3Request;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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

    LTI3Request lti3Request;

    @PutMapping
    ResponseEntity<LtiContextEntity> pairBookToLMSContext(@RequestBody HashMap<String, String> pairBookBody) {
        try {
            lti3Request = new LTI3Request(ltiDataService, true, null, pairBookBody.get("id_token"));

            // Retrieve LMS config from db to be used to find the right context
            List<PlatformDeployment> platformDeploymentList = platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(lti3Request.getIss(), lti3Request.getAud(), lti3Request.getLtiDeploymentId());
            if (platformDeploymentList.size() != 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            PlatformDeployment platformDeployment = platformDeploymentList.get(0);

            // Retrieve context from db
            LtiContextEntity ltiContext = ltiContextRepository.findByContextKeyAndPlatformDeployment(lti3Request.getLtiContextId(), platformDeployment);

            // Update context to have rootOutcomeGuid value
            if (StringUtils.isNotBlank(pairBookBody.get("root_outcome_guid")) && ltiContext != null) {
                ltiContext.setRootOutcomeGuid(pairBookBody.get("root_outcome_guid"));
                ltiContextRepository.save(ltiContext);
                return ResponseEntity.ok(ltiContext);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (DataServiceException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
