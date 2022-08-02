package net.unicon.lti.utils.lti;

import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.unicon.lti.utils.TextConstants.LTI_NONCE_COOKIE_NAME;
import static net.unicon.lti.utils.TextConstants.LTI_STATE_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LTI3RequestTest {
    private static final String ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjEtMDktMDFUMDA6MTQ6MzdaIn0.eyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9tZXNzYWdlX3R5cGUiOiJMdGlSZXNvdXJjZUxpbmtSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcmVzb3VyY2VfbGluayI6eyJpZCI6IjcxZTViNWNiLTZmMDUtNGQ1My1iN2U5LWQxMmVjZDQ3NWU3NiIsImRlc2NyaXB0aW9uIjoiIiwidGl0bGUiOiJTdHVkeSBQbGFuOiBUaGUgRXRpb2xvZ3kgYW5kIFRyZWF0bWVudCBvZiBNZW50YWwgRGlzb3JkZXJzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJhdWQiOiI5NzE0MDAwMDAwMDAwMDIzMCIsImF6cCI6Ijk3MTQwMDAwMDAwMDAwMjMwIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vZGVwbG95bWVudF9pZCI6IjQ2MTo1NDQwYTA4NDIyYWIxZWU3Nzk0YTA1ODhiNWU0Y2I0YTA5NGM0MjU2IiwiZXhwIjoxNjMzNTQyMzczLCJpYXQiOjE2MzM1Mzg3NzMsImlzcyI6Imh0dHBzOi8vY2FudmFzLmluc3RydWN0dXJlLmNvbSIsIm5vbmNlIjoiNWQwNGNiMTJmNDVkZjZlZTM3M2M0MmExY2E0Y2RiZTA4ZTJiZmE4ZTVmN2M2NjJhY2EzZjY1NjA2ODdmZGM0NyIsInN1YiI6IjRmM2QxMmRmLWUxYWUtNDg0Zi04YjlhLWI2Njc4NjRlODEwMCIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vZ29sZGlsb2Nrcy5sdW1lbmxlYXJuaW5nLmNvbS9zdHVkeV9wbGFuLzdjM2EzOTE1LWZmZGEtNGU4Ny05MTQ5LTE2YzFhYzUyM2IyZj9laWQ9YUd3d0x3QndrdFpnelp0cENKWGVaZyIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2NvbnRleHQiOnsiaWQiOiIwYTQ3ZGU5MWNmODRlZTE0N2Y2ZTUzNDE5NTk4ODUwODUwNGQzZTgyIiwibGFiZWwiOiJtZ3dvemR6LWx1bWVuIiwidGl0bGUiOiJtZ3dvemR6IEx1bWVuIFRlc3QiLCJ0eXBlIjpbImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2NvdXJzZSNDb3Vyc2VPZmZlcmluZyJdLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3Rvb2xfcGxhdGZvcm0iOnsiZ3VpZCI6InBvdDI4eU5wRFNaczFGdk1RYTQyTWlQV2xST0xRQ0FlZHpRWDZNYzI6Y2FudmFzLWxtcyIsIm5hbWUiOiJVbmljb24iLCJ2ZXJzaW9uIjoiY2xvdWQiLCJwcm9kdWN0X2ZhbWlseV9jb2RlIjoiY2FudmFzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9sYXVuY2hfcHJlc2VudGF0aW9uIjp7ImRvY3VtZW50X3RhcmdldCI6ImlmcmFtZSIsImhlaWdodCI6bnVsbCwid2lkdGgiOm51bGwsInJldHVybl91cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vY291cnNlcy8zMzQ4L2V4dGVybmFsX2NvbnRlbnQvc3VjY2Vzcy9leHRlcm5hbF90b29sX3JlZGlyZWN0IiwibG9jYWxlIjoiZW4iLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImxvY2FsZSI6ImVuIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZXMiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvaW5zdGl0dXRpb24vcGVyc29uI0FkbWluaXN0cmF0b3IiLCJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9pbnN0aXR1dGlvbi9wZXJzb24jSW5zdHJ1Y3RvciIsImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2luc3RpdHV0aW9uL3BlcnNvbiNTdHVkZW50IiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvc3lzdGVtL3BlcnNvbiNVc2VyIl0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2N1c3RvbSI6eyJkdWVfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQuZHVlQXQuaXNvODYwMSIsImxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQubG9ja0F0Lmlzbzg2MDEiLCJ1bmxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQudW5sb2NrQXQuaXNvODYwMSIsImNhbnZhc191c2VyX2lkIjozODYsImNhbnZhc19sb2dpbl9pZCI6Im1nd296ZHpAdW5pY29uLm5ldCIsImNhbnZhc19jb3Vyc2VfaWQiOjMzNDgsImNhbnZhc191c2VyX25hbWUiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJjYW52YXNfYXNzaWdubWVudF9pZCI6NjI3MX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTExX2xlZ2FjeV91c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTFwMSI6eyJ1c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319fSwiZXJyb3JzIjp7ImVycm9ycyI6e319LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3MvY2xhaW0vZW5kcG9pbnQiOnsic2NvcGUiOlsiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtLnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Jlc3VsdC5yZWFkb25seSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9zY29wZS9zY29yZSJdLCJsaW5laXRlbXMiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbGluZV9pdGVtcyIsImxpbmVpdGVtIjoiaHR0cHM6Ly91bmljb24uaW5zdHJ1Y3R1cmUuY29tL2FwaS9sdGkvY291cnNlcy8zMzQ4L2xpbmVfaXRlbXMvNTAzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJwaWN0dXJlIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tL2ltYWdlcy9tZXNzYWdlcy9hdmF0YXItNTAucG5nIiwiZW1haWwiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJuYW1lIjoiTWFyeSBHd296ZHoiLCJnaXZlbl9uYW1lIjoiTWFyeSIsImZhbWlseV9uYW1lIjoiR3dvemR6IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGlzIjp7InBlcnNvbl9zb3VyY2VkaWQiOiJtZ3dvemR6IiwiY291cnNlX29mZmVyaW5nX3NvdXJjZWRpZCI6bnVsbCwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1ucnBzL2NsYWltL25hbWVzcm9sZXNlcnZpY2UiOnsiY29udGV4dF9tZW1iZXJzaGlwc191cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbmFtZXNfYW5kX3JvbGVzIiwic2VydmljZV92ZXJzaW9ucyI6WyIyLjAiXSwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3d3dy5pbnN0cnVjdHVyZS5jb20vcGxhY2VtZW50IjpudWxsfQ.qN6MqcitF8VHuMVk_YmuXnKN43A1TgdjbSV8zHxT6SS5uZo8vtTB2GBGmqPpsknTmJPNijpQsPMoeLN3kKAvkpJ0RJPWMYud89a1SGnons8HuqXxACUCwUa7Lki8j7xSIXB6tgXQqzkrHCPmsBwLQy6rAYseiDNpsLkzIZzBAZjt89262i_UxRScJkhdPF4GEQDw4d2d1YMM5cmSy039BZZyT7AOV4q3qjo_BeFlQ5QJjXDtXGZ5VIcOuyLipAcrG8a2llXd8gLDkxkD0gIwI_zZX2fG-XKHc_co_gp45L2fVz09vKS5R-yCiooZgktOBoe0OEY8vHfCbBk4Vj2sxg";
    private static final String SAMPLE_ISS = "https://platform-lms.com";
    private static final String SAMPLE_CLIENT_ID = "sample-client-id";
    private static final String SAMPLE_DEPLOYMENT_ID = "sample-deployment-id";
    private static final String SAMPLE_NONCE = "sample-nonce";
    private static final String SAMPLE_NONCE_HASH = Hashing.sha256().hashString(SAMPLE_NONCE, StandardCharsets.UTF_8).toString();
    private static final Cookie NONCE_COOKIE = new Cookie(LTI_NONCE_COOKIE_NAME, SAMPLE_NONCE);
    private static final Cookie JSESSIONID_COOKIE = new Cookie("JSESSIONID", "test");
    private static final Cookie STATE_COOKIE = new Cookie(LTI_STATE_COOKIE_NAME, "test-state");
    private static final String ID_TOKEN_PARAM = "id_token";


    @Mock
    private LTIDataService ltiDataService;

    @InjectMocks
    private AllRepositories allRepositories;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private HttpServletRequest req;

    @Mock
    private PlatformDeployment platformDeployment;

    @Mock
    Jws<Claims> jwsClaims;

    @Mock
    Claims claims;

    @Mock
    JwtParser jwtParser;

    private MockHttpSession mockHttpSession = new MockHttpSession();

    @Configuration
    static class ContextConfiguration {

    }

    @BeforeEach()
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(ltiDataService.getRepos()).thenReturn(allRepositories);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(req.getSession()).thenReturn(mockHttpSession);
        when(jwtParser.parseClaimsJws(anyString())).thenReturn(jwsClaims);
    }

    @Test
    public void testLTI3RequestWithoutRequest() {
        AssertionError exception = Assertions.assertThrows(
                AssertionError.class,
                () -> {new LTI3Request(null, ltiDataService, false, "1234", jwsClaims);}
        );
        assertEquals(exception.getMessage(), "cannot make an LtiRequest without a request");
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithoutPlatformJWKS() {
        when(platformDeploymentRepository.findByIssAndClientId(any(String.class), any(String.class))).thenReturn(List.of(platformDeployment));

        IllegalArgumentException e = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
        );
        verify(platformDeploymentRepository).findByIssAndClientId(any(String.class), any(String.class));
        assertEquals("A signing key must be specified if the specified JWT is digitally signed.", e.getMessage());
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithoutUniquePlatformDeployment() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class)) {
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(new ArrayList<>());

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("PlatformDeployment does not exist or is duplicated for issuer: https://platform-lms.com, clientId: sample-client-id, and deploymentId: sample-deployment-id", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithInvalidLTIType() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class)) {
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            prepareIdTokenForInitialValidations(null);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not a valid LTI3 request: LTI Message Type = null. ", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithoutServletRequestAttributes() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class)) {
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("No ServletRequestAttributes can be found, cannot validate the request.", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithoutCookies() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Nonce error: Nonce = null in the JWT or in the session.", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithoutIdTokenNonce() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Nonce error: Nonce = null in the JWT or in the session.", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestWithInvalidNonce() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Nonce error: Unknown or already used nonce.", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestInvalidNoResourceLink() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            // ltiResourceLink not null, non-zero size, includes non-empty ltiLinkId
            when(claims.containsKey(LtiStrings.LTI_LINK)).thenReturn(false);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " Lti Resource Link is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestInvalidNoSub() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            // sub not empty
            when(claims.getSubject()).thenReturn("");

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " User (sub) is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestInvalidNoRoles() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            // ltiRoles not null or empty
            when(claims.containsKey(LtiStrings.LTI_ROLES)).thenReturn(false);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " Lti Roles is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestInvalidNoExp() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            // exp not null
            when(claims.getExpiration()).thenReturn(null);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {
                        new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);
                    }
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " Exp is empty or invalid.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestInvalidNoIat() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            // iat not null
            when(claims.getIssuedAt()).thenReturn(null);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " Iat is empty or invalid.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequest() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);

            new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));

        } catch (DataServiceException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestDeepLinkingRequestInvalidDLSettings() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // deepLinkingSettings not null or empty
            when(claims.containsKey(LtiStrings.DEEP_LINKING_SETTINGS)).thenReturn(true);
            when(claims.get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class)).thenReturn(new HashMap());

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " DeepLinkingSettings is empty or invalid.\n" +
                    "  deepLinkReturnUrl is empty.\n" +
                    "  deepLink AcceptTypes is empty.\n" +
                    "  deepLink AcceptPresentationDocumentTargets is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestDeepLinkingRequestInvalidDLReturnUrl() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // deepLinkingSettings not null or empty
            when(claims.containsKey(LtiStrings.DEEP_LINKING_SETTINGS)).thenReturn(true);
            Map<String, Object> deepLinkingSettings = Map.of(
                    LtiStrings.DEEP_LINK_RETURN_URL, "", // deepLinkReturnUrl not empty
                    LtiStrings.DEEP_LINK_ACCEPT_TYPES, List.of("LtiResourceLink"), // deepLinkAcceptTypes not empty
                    LtiStrings.DEEP_LINK_DOCUMENT_TARGETS, List.of("LtiResourceLink") // deepLinkAcceptPresentationDocumentTargets not empty
            );
            when(claims.get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class)).thenReturn(deepLinkingSettings);
            when(ltiDataService.getDemoMode()).thenReturn(false);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " deepLinkReturnUrl is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestDeepLinkingRequestInvalidDLAcceptTypes() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // deepLinkingSettings not null or empty
            when(claims.containsKey(LtiStrings.DEEP_LINKING_SETTINGS)).thenReturn(true);
            Map<String, Object> deepLinkingSettings = Map.of(
                    LtiStrings.DEEP_LINK_RETURN_URL, "https://platform.com/deep-link-return", // deepLinkReturnUrl not empty
                    LtiStrings.DEEP_LINK_ACCEPT_TYPES, new ArrayList(), // deepLinkAcceptTypes not empty
                    LtiStrings.DEEP_LINK_DOCUMENT_TARGETS, List.of("LtiResourceLink") // deepLinkAcceptPresentationDocumentTargets not empty
            );
            when(claims.get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class)).thenReturn(deepLinkingSettings);
            when(ltiDataService.getDemoMode()).thenReturn(false);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " deepLink AcceptTypes is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestDeepLinkingRequestInvalidDLDocTargets() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // deepLinkingSettings not null or empty
            when(claims.containsKey(LtiStrings.DEEP_LINKING_SETTINGS)).thenReturn(true);
            Map<String, Object> deepLinkingSettings = Map.of(
                    LtiStrings.DEEP_LINK_RETURN_URL, "https://platform.com/deep-link-return", // deepLinkReturnUrl not empty
                    LtiStrings.DEEP_LINK_ACCEPT_TYPES, List.of("LtiResourceLink"), // deepLinkAcceptTypes not empty
                    LtiStrings.DEEP_LINK_DOCUMENT_TARGETS, new ArrayList() // deepLinkAcceptPresentationDocumentTargets not empty
            );
            when(claims.get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class)).thenReturn(deepLinkingSettings);
            when(ltiDataService.getDemoMode()).thenReturn(false);

            IllegalStateException e = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> {new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);}
            );
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            assertEquals("Request is not complete and correct: \n" +
                    " deepLink AcceptPresentationDocumentTargets is empty.\n" +
                    " true", e.getMessage());
        }
    }

    @Test
    public void testLTI3RequestForIdTokenWithoutRequestDeepLinkingRequest() {
        try (MockedStatic<Jwts> jwts = Mockito.mockStatic(Jwts.class);
             MockedStatic<RequestContextHolder> requestContextHolder = Mockito.mockStatic(RequestContextHolder.class)
        ) {
            prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            jwts.when(Jwts::parser).thenReturn(jwtParser);
            // Givens to validate nonce
            ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
            requestContextHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(req);
            when(req.getCookies()).thenReturn(new Cookie[]{NONCE_COOKIE});
            when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(SAMPLE_NONCE_HASH);
            prepareIdTokenForFinalValidations(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // deepLinkingSettings not null or empty
            when(claims.containsKey(LtiStrings.DEEP_LINKING_SETTINGS)).thenReturn(true);
            Map<String, Object> deepLinkingSettings = Map.of(
                    LtiStrings.DEEP_LINK_RETURN_URL, "https://platform.com/deep-link-return", // deepLinkReturnUrl not empty
                    LtiStrings.DEEP_LINK_ACCEPT_TYPES, List.of("LtiResourceLink"), // deepLinkAcceptTypes not empty
                    LtiStrings.DEEP_LINK_DOCUMENT_TARGETS, List.of("LtiResourceLink") // deepLinkAcceptPresentationDocumentTargets not empty
            );
            when(claims.get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class)).thenReturn(deepLinkingSettings);
            when(ltiDataService.getDemoMode()).thenReturn(false);

            new LTI3Request(ltiDataService, false, "1234", ID_TOKEN);
            verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3RequestWithoutDataService() {
        AssertionError exception = Assertions.assertThrows(
                AssertionError.class,
                () -> {new LTI3Request(req, null, false, "1234", jwsClaims);}
        );
        assertEquals(exception.getMessage(), "LTIDataService cannot be null");
    }

    @Test
    public void testLTI3RequestWithoutDeployment() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(new ArrayList<>());

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        assertEquals("PlatformDeployment does not exist or is duplicated for issuer: https://platform-lms.com, clientId: sample-client-id, and deploymentId: sample-deployment-id", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithDuplicateDeployment() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(Arrays.asList(platformDeployment, platformDeployment));

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        assertEquals("PlatformDeployment does not exist or is duplicated for issuer: https://platform-lms.com, clientId: sample-client-id, and deploymentId: sample-deployment-id", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithInvalidMessageType() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(null);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        assertEquals("Request is not a valid LTI3 request: LTI Message Type = null. ", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithMissingNonceCookie() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        Cookie[] cookies = {JSESSIONID_COOKIE, STATE_COOKIE};
        when(req.getCookies()).thenReturn(cookies);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn(SAMPLE_NONCE);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Nonce = null in the JWT or in the session.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithMissingCookies() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(req.getCookies()).thenReturn(new Cookie[]{});
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn(SAMPLE_NONCE);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Nonce = null in the JWT or in the session.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithMissingNonce() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        Cookie[] cookies = {new Cookie(LTI_NONCE_COOKIE_NAME, ""), JSESSIONID_COOKIE, STATE_COOKIE};
        when(req.getCookies()).thenReturn(cookies);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn(SAMPLE_NONCE);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Unknown or already used nonce.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithInvalidNonceCookie() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        Cookie[] cookies = {new Cookie(LTI_NONCE_COOKIE_NAME, SAMPLE_NONCE), JSESSIONID_COOKIE, STATE_COOKIE};
        when(req.getCookies()).thenReturn(cookies);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn(SAMPLE_NONCE);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Unknown or already used nonce.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithInvalidNonce() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        Cookie[] cookies = {new Cookie(LTI_NONCE_COOKIE_NAME, SAMPLE_NONCE), JSESSIONID_COOKIE, STATE_COOKIE};
        when(req.getCookies()).thenReturn(cookies);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn(SAMPLE_NONCE);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Unknown or already used nonce.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithValidNonce() {
        when(req.getParameter(ID_TOKEN_PARAM)).thenReturn(ID_TOKEN);
        prepareIdTokenForInitialValidations(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        Cookie[] cookies = {new Cookie(LTI_NONCE_COOKIE_NAME, SAMPLE_NONCE), JSESSIONID_COOKIE, STATE_COOKIE};
        when(req.getCookies()).thenReturn(cookies);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn(SAMPLE_NONCE_HASH);

        Assertions.assertThrows(NullPointerException.class, () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);});

        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
    }

    private void prepareIdTokenForInitialValidations(String messageType) {
        when(platformDeploymentRepository.findByIssAndClientId(any(String.class), any(String.class))).thenReturn(List.of(platformDeployment));
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(List.of(platformDeployment));
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.get(LtiStrings.LTI_VERSION, String.class)).thenReturn(LtiStrings.LTI_VERSION_3);
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn(messageType);
        when(platformDeployment.getJwksEndpoint()).thenReturn("https://platform.com/jwk/jwks");
    }

    private void prepareIdTokenForFinalValidations(String messageType) {
        // verifying ResourceLink id_token is "complete and correct"
        when(claims.containsKey(LtiStrings.LTI_MESSAGE_TYPE)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE)).thenReturn(messageType);
        when(claims.containsKey(LtiStrings.LTI_DEPLOYMENT_ID)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_DEPLOYMENT_ID, String.class)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        // ltiResourceLink not null, non-zero size, includes non-empty ltiLinkId
        when(claims.containsKey(LtiStrings.LTI_LINK)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_LINK, Map.class)).thenReturn(Collections.singletonMap(LtiStrings.LTI_LINK_ID, "test-link-id"));
        // sub not empty
        when(claims.getSubject()).thenReturn("lti-middleware");
        // ltiRoles not null or empty
        when(claims.containsKey(LtiStrings.LTI_ROLES)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_ROLES, List.class)).thenReturn(List.of(LtiStrings.LTI_ROLE_INSTRUCTOR));
        // exp not null
        when(claims.getExpiration()).thenReturn(new Date());
        // iat not null
        when(claims.getIssuedAt()).thenReturn(new Date());
    }
}
