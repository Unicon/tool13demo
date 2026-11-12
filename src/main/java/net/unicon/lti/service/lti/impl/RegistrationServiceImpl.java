package net.unicon.lti.service.lti.impl;

import net.unicon.lti.utils.HttpClients;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.RegistrationException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolConfigurationACKDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.RegistrationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * This manages the Registration call
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final ExceptionMessageGenerator exceptionMessageGenerator;

    private final PlatformDeploymentRepository platformDeploymentRepository;

    private RestTemplate restTemplate;

    //Calling the registration service and getting a registration  result of users.
    @Override
    public ToolConfigurationACKDTO callDynamicRegistration(String token, ToolRegistrationDTO toolRegistrationDTO, String endpoint) throws ConnectionException {
        ToolConfigurationACKDTO answer;
        try {
            restTemplate = restTemplate == null ? HttpClients.restTemplate() : restTemplate;
            DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
            defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
            restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

            //We add the token in the request with this.
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.isNotBlank(token)) {
                log.debug("Token was not blank: {}", token);
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            HttpEntity<ToolRegistrationDTO> request = new HttpEntity<>(toolRegistrationDTO, headers);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.

            if (log.isDebugEnabled()) {
                ObjectMapper objectMapper = new ObjectMapper();
                String toolRegistrationDTOString = objectMapper.writeValueAsString(toolRegistrationDTO);
                log.debug(toolRegistrationDTOString);
            }

            log.debug("Endpoint -  " + endpoint);
            ResponseEntity<ToolConfigurationACKDTO> registrationRequest = restTemplate.exchange(endpoint, HttpMethod.POST, request, ToolConfigurationACKDTO.class);
            HttpStatusCode status = registrationRequest.getStatusCode();
            if (status.is2xxSuccessful()) {
                answer = registrationRequest.getBody();
                log.debug(answer.toString());
            } else {
                log.error(registrationRequest.toString());
                String exceptionMsg = "Can't get confirmation of the registration";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Problem during the registration");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return answer;
    }

    @Override
    public void saveNewPlatformDeployment(ToolConfigurationACKDTO answer, PlatformRegistrationDTO platformRegistrationDTO) throws RegistrationException {
        try {
            PlatformDeployment platformDeployment = new PlatformDeployment();
            platformDeployment.setDeploymentId(answer.getToolConfiguration().getDeployment_id());
            platformDeployment.setClientId(answer.getClient_id());
            if (platformRegistrationDTO.getIssuer().contains("developer.blackboard.com")) { // Blackboard incorrectly sets their issuer in Dynamic Registration
                platformDeployment.setIss("https://blackboard.com");
            } else {
                platformDeployment.setIss(platformRegistrationDTO.getIssuer());
            }
            platformDeployment.setJwksEndpoint(platformRegistrationDTO.getJwks_uri());
            if (platformRegistrationDTO.getIssuer().endsWith("brightspace.com")) {
                platformDeployment.setoAuth2TokenAud(platformRegistrationDTO.getToken_endpoint());
                //If this fails we need to use : https://api.brightspace.com/auth/token
            }
            platformDeployment.setoAuth2TokenUrl(platformRegistrationDTO.getToken_endpoint());
            platformDeployment.setOidcEndpoint(platformRegistrationDTO.getAuthorization_endpoint());
            platformDeploymentRepository.save(platformDeployment);
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Problem during the registration. Not able to save the new configration");
            log.error(exceptionMsg.toString(), e);
            throw new RegistrationException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }


}