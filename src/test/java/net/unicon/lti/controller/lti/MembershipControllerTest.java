package net.unicon.lti.controller.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.membership.CourseUser;
import net.unicon.lti.model.membership.CourseUsers;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.AdvantageMembershipService;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MembershipControllerTest {

    private static final String JWT = "advantage-jwt";

    @InjectMocks
    private MembershipController membershipController;

    @Mock
    private LtiContextRepository ltiContextRepository;

    @Mock
    private APIJWTService apijwtService;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private AdvantageMembershipService advantageMembershipService;

    @Mock
    private HttpServletRequest req;

    private ExtendedModelMap model;
    private PlatformDeployment platformDeployment;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        model = new ExtendedModelMap();
        platformDeployment = new PlatformDeployment();
    }

    private void validToken(boolean deploymentFound) {
        when(req.getParameter(TextConstants.ADVANTAGE_TOKEN)).thenReturn(JWT);
        Claims claims = mock(Claims.class);
        when(claims.get("platformDeploymentId")).thenReturn("8");
        when(claims.get("contextKey")).thenReturn("ctx");
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        when(apijwtService.validateToken(JWT)).thenReturn(jws);
        when(platformDeploymentRepository.findById(8L))
                .thenReturn(deploymentFound ? Optional.of(platformDeployment) : Optional.empty());
    }

    @Test
    public void testMembershipGetPopulatesResults() throws ConnectionException {
        validToken(true);
        LtiContextEntity context = new LtiContextEntity();
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment("ctx", platformDeployment)).thenReturn(context);
        LTIToken token = new LTIToken();
        when(advantageMembershipService.getToken(platformDeployment)).thenReturn(token);
        CourseUser user = new CourseUser();
        user.setUserId("u1");
        CourseUsers courseUsers = new CourseUsers();
        courseUsers.getCourseUserList().add(user);
        when(advantageMembershipService.callMembershipService(token, context)).thenReturn(courseUsers);

        String view = membershipController.membershipGet(req, null, model);

        assertEquals("ltiAdvMembershipMain", view);
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertEquals(List.of(user), model.getAttribute("results"));
    }

    @Test
    public void testMembershipGetWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvMembershipMain", membershipController.membershipGet(req, null, model));

        assertEquals(true, model.getAttribute("noSessionValues"));
        assertFalse(model.containsAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        verifyNoInteractions(apijwtService, advantageMembershipService);
    }

    @Test
    public void testMembershipGetUnknownDeployment() throws ConnectionException {
        validToken(false);

        assertEquals("ltiAdvMembershipMain", membershipController.membershipGet(req, null, model));

        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertFalse(model.containsAttribute("results"));
        verifyNoInteractions(ltiContextRepository, advantageMembershipService);
    }

    @Test
    public void testMembershipGetInvalidToken() throws ConnectionException {
        when(req.getParameter(TextConstants.ADVANTAGE_TOKEN)).thenReturn(JWT);
        when(apijwtService.validateToken(JWT)).thenThrow(new JwtException("expired"));

        assertEquals("ltiAdvMembershipMain", membershipController.membershipGet(req, null, model));

        assertEquals(true, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertFalse(model.containsAttribute("results"));
        verifyNoInteractions(platformDeploymentRepository, advantageMembershipService);
    }

    @Test
    public void testMembershipGetServiceFailureFlagsToken() throws ConnectionException {
        validToken(true);
        when(advantageMembershipService.getToken(platformDeployment)).thenThrow(new ConnectionException("no token"));

        assertEquals("ltiAdvMembershipMain", membershipController.membershipGet(req, null, model));

        assertEquals(true, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertFalse(model.containsAttribute("results"));
    }
}
