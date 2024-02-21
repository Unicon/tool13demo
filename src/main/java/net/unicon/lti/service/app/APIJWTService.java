package net.unicon.lti.service.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.model.oauth2.SecuredInfo;
import net.unicon.lti.utils.lti.LTI3Request;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface APIJWTService {
    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateToken(String token);

    Jwt<Header, Claims> unsecureToken(String token);

    String buildJwt(boolean oneUse,
                    List<String> roles,
                    Long contextId,
                    Long platformDeploymentId,
                    String userId,
                    String canvasUserId,
                    String canvasUserGlobalId,
                    String canvasLoginId,
                    String canvasUserName,
                    String canvasCourseId,
                    String canvasAssignmentId,
                    String dueAt,
                    String lockAt,
                    String unlockAt,
                    String nonce) throws GeneralSecurityException, IOException;

    String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException;

    String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam);

    SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam);

    boolean isAdmin(SecuredInfo securedInfo);

    boolean isInstructor(SecuredInfo securedInfo);

    boolean isInstructorOrHigher(SecuredInfo securedInfo);

    boolean isLearner(SecuredInfo securedInfo);

    boolean isLearnerOrHigher(SecuredInfo securedInfo);

    boolean isGeneral(SecuredInfo securedInfo);
}
