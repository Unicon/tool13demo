package net.unicon.lti.service.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.DeepLinkDTO;
import net.unicon.lti.model.lti.dto.DeepLinkJWTDTO;
import net.unicon.lti.model.lti.dto.DeepLinkRequest;
import net.unicon.lti.utils.lti.LTI3Request;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface DeepLinkService {

    List<DeepLinkDTO> getDeepLinks();

    DeepLinkJWTDTO generateDeepLinkJWT(List<String> deepLinkRequestIds, Jws<Claims> id_token) throws GeneralSecurityException, IOException;
}
