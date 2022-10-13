package net.unicon.lti.service.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.model.PlatformDeployment;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface LTIJWTService {
    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateState(String state);

    Jws<Claims> validateJWT(String jwt, String clientId);

    String generateStateOrClientAssertionJWT(PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException;
}
