package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.membership.CourseUser;
import net.unicon.lti.model.membership.CourseUsers;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.impl.AdvantageMembershipServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdvantageMembershipServiceTest {

    private static final String MEMBERSHIP_URL = "https://lms.com/api/lti/courses/1/names_and_roles";
    private static final String NRPS_TYPE = "application/vnd.ims.lti-nrps.v2.membershipcontainer+json";

    AdvantageMembershipService advantageMembershipService;

    @Mock
    AdvantageConnectorHelper advantageConnectorHelper;

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    RestTemplate restTemplate;

    private LTIToken token;
    private LtiContextEntity context;
    private final HttpEntity<?> request = new HttpEntity<>(new HttpHeaders());

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        advantageMembershipService = new AdvantageMembershipServiceImpl(advantageConnectorHelper, exceptionMessageGenerator);
        when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any(Exception.class)))
                .thenAnswer(inv -> inv.getArgument(0) + " | " + ((Exception) inv.getArgument(1)).getMessage());
        token = new LTIToken();
        token.setAccess_token("nrps-token");
        context = new LtiContextEntity();
        context.setContext_memberships_url(MEMBERSHIP_URL);
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, NRPS_TYPE)).thenReturn((HttpEntity) request);
    }

    private static CourseUser user(String id) {
        CourseUser u = new CourseUser();
        u.setUserId(id);
        return u;
    }

    private static CourseUsers users(CourseUser... list) {
        CourseUsers cu = new CourseUsers();
        cu.getCourseUserList().addAll(List.of(list));
        return cu;
    }

    @Test
    public void testGetTokenRequestsMembershipScope() throws ConnectionException {
        PlatformDeployment pd = new PlatformDeployment();
        when(advantageConnectorHelper.getToken(pd, "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly"))
                .thenReturn(token);

        assertSame(token, advantageMembershipService.getToken(pd));
    }

    @Test
    public void testCallMembershipServiceCombinesAllPages() throws ConnectionException {
        CourseUser u1 = user("u1");
        CourseUser u2 = user("u2");
        CourseUser u3 = user("u3");
        HttpHeaders page1Headers = new HttpHeaders();
        page1Headers.add("link", "page1");
        HttpHeaders page2Headers = new HttpHeaders();
        page2Headers.add("link", "page2");
        when(restTemplate.exchange(MEMBERSHIP_URL, HttpMethod.GET, request, CourseUsers.class))
                .thenReturn(new ResponseEntity<>(users(u1), page1Headers, HttpStatus.OK));
        when(advantageConnectorHelper.nextPage(page1Headers)).thenReturn("https://lms.com/page2");
        when(restTemplate.exchange("https://lms.com/page2", HttpMethod.GET, request, CourseUsers.class))
                .thenReturn(new ResponseEntity<>(users(u2, u3), page2Headers, HttpStatus.OK));

        CourseUsers result = advantageMembershipService.callMembershipService(token, context);

        assertEquals(List.of(u1, u2, u3), result.getCourseUserList());
        verify(restTemplate).exchange(MEMBERSHIP_URL, HttpMethod.GET, request, String.class);
        verify(advantageConnectorHelper).nextPage(page2Headers);
    }

    @Test
    public void testCallMembershipServiceNon2xxIsWrapped() {
        when(restTemplate.exchange(MEMBERSHIP_URL, HttpMethod.GET, request, CourseUsers.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));

        ConnectionException ex = assertThrows(ConnectionException.class,
                () -> advantageMembershipService.callMembershipService(token, context));

        assertEquals("Can't get the membership | Can't get the membership", ex.getMessage());
    }

    @Test
    public void testCallMembershipServiceRestExceptionIsWrapped() {
        when(restTemplate.exchange(MEMBERSHIP_URL, HttpMethod.GET, request, CourseUsers.class))
                .thenThrow(new RestClientException("timeout"));

        ConnectionException ex = assertThrows(ConnectionException.class,
                () -> advantageMembershipService.callMembershipService(token, context));

        assertEquals("Can't get the membership | timeout", ex.getMessage());
    }
}
