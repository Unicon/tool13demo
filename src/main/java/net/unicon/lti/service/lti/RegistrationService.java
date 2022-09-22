package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;

public interface RegistrationService {
    //Calling the membership service and getting a paginated result of users.
    String callDynamicRegistration(String token, ToolRegistrationDTO toolRegistrationDTO, String endpoint) throws ConnectionException;

    ToolRegistrationDTO generateToolConfiguration(PlatformRegistrationDTO platformConfiguration);
}
