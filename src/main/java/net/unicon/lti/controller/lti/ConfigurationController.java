package net.unicon.lti.controller.lti;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.PlatformDeploymentRequest;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This controller is protected by basic authentication
 * Allows to read and change the configuration
 */
@Slf4j
@Controller
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigurationController {
    @Value("${lti13.demoMode}")
    private boolean demoMode;

    private final PlatformDeploymentRepository platformDeploymentRepository;

    private final LTIJWTService ltijwtService;

    @GetMapping(value = "/", produces = "application/json;")
    @ResponseBody
    public ResponseEntity<Page<PlatformDeployment>> displayConfigs(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {

        Page<PlatformDeployment> platformDeploymentListEntityList = platformDeploymentRepository.findAll(PageRequest.of(page, size));
        if (platformDeploymentListEntityList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            // You many decide to return HttpStatus.NOT_FOUND
        }
        return new ResponseEntity<>(platformDeploymentListEntityList, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json;")
    @ResponseBody
    public ResponseEntity<PlatformDeployment> displayConfig(@PathVariable("id") long id) {

        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(id);

        if (!platformDeployment.isPresent()) {
            log.error("platformDeployment with id {} not found.", id);
            return new ResponseEntity("platformDeployment with id " + id
                    + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(platformDeployment.get(), HttpStatus.OK);
        }
    }

    @PostMapping("/")
    public ResponseEntity<PlatformDeployment> createDeployment(@RequestBody PlatformDeploymentRequest request) {
        PlatformDeployment platformDeployment = request.toEntity();
        log.info("Creating Deployment : {}", platformDeployment);

        if (!platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(platformDeployment.getIss(), platformDeployment.getClientId(), platformDeployment.getDeploymentId()).isEmpty()) {
            log.error("Unable to create. A platformDeployment like that already exist");
            return new ResponseEntity("Unable to create. This platformDeployment already exists.", HttpStatus.CONFLICT);
        }
        PlatformDeployment platformDeploymentSaved = platformDeploymentRepository.save(platformDeployment);

        return new ResponseEntity<>(platformDeploymentSaved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatformDeployment> updateDeployment(@PathVariable("id") long id, @RequestBody PlatformDeploymentRequest request) {
        log.info("Updating User with id {}", id);

        Optional<PlatformDeployment> platformDeploymentSearchResult = platformDeploymentRepository.findById(id);

        if (!platformDeploymentSearchResult.isPresent()) {
            log.error("Unable to update. PlatformDeployment with id {} not found.", id);
            return new ResponseEntity("Unable to update. User with id " + id + TextConstants.NOT_FOUND_SUFFIX,
                    HttpStatus.NOT_FOUND);
        }

        PlatformDeployment platformDeploymentToChange = platformDeploymentSearchResult.get();
        request.applyTo(platformDeploymentToChange);

        platformDeploymentRepository.saveAndFlush(platformDeploymentToChange);
        return new ResponseEntity<>(platformDeploymentToChange, HttpStatus.OK);
    }

    @RequestMapping(value = "/lti_advantage/client_assertion", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, String>> generateClientAssertionJWT(@RequestBody Map<String, String> body) throws GeneralSecurityException, IOException {
        if (demoMode) {
            if (StringUtils.isAnyBlank(body.get("iss"), body.get("client_id"), body.get("deployment_id"))) {
                String errorMessage = "iss was " + body.get("iss") + ", client_id was " + body.get("client_id") + ", and deployment_id was " + body.get("deployment_id") + ". All values must be provided.";
                log.error(errorMessage);
                return new ResponseEntity<>(Collections.singletonMap("error", errorMessage), HttpStatus.BAD_REQUEST);
            }
            List<PlatformDeployment> platformDeploymentList = platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(body.get("iss"), body.get("client_id"), body.get("deployment_id"));
            if (platformDeploymentList.size() != 1) {
                String errorMessage = "PlatformDeployment size was " + platformDeploymentList.size() + ". There should be exactly 1.";
                log.error(errorMessage);
                return new ResponseEntity<>(Collections.singletonMap("error", errorMessage), HttpStatus.BAD_REQUEST);
            }
            PlatformDeployment platformDeployment = platformDeploymentList.get(0);

            String clientAssertionJWT = ltijwtService.generateStateOrClientAssertionJWT(platformDeployment);

            return new ResponseEntity<>(Collections.singletonMap("client_assertion", clientAssertionJWT), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Collections.singletonMap("error", "Disabled: You do not have permission."), HttpStatus.FORBIDDEN);
        }
    }
}
