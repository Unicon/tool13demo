package net.unicon.lti.utils.lti;

import static org.mockito.ArgumentMatchers.anyString;
import com.nimbusds.jose.KeySourceException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.RequiredTypeException;
import net.unicon.lti.config.ApplicationConfig;
import net.unicon.lti.service.lti.impl.LTIDataServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.objenesis.ObjenesisStd;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LTI3RequestTest {
    private static String ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjEtMDktMDFUMDA6MTQ6MzdaIn0.eyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9tZXNzYWdlX3R5cGUiOiJMdGlSZXNvdXJjZUxpbmtSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcmVzb3VyY2VfbGluayI6eyJpZCI6IjcxZTViNWNiLTZmMDUtNGQ1My1iN2U5LWQxMmVjZDQ3NWU3NiIsImRlc2NyaXB0aW9uIjoiIiwidGl0bGUiOiJTdHVkeSBQbGFuOiBUaGUgRXRpb2xvZ3kgYW5kIFRyZWF0bWVudCBvZiBNZW50YWwgRGlzb3JkZXJzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJhdWQiOiI5NzE0MDAwMDAwMDAwMDIzMCIsImF6cCI6Ijk3MTQwMDAwMDAwMDAwMjMwIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vZGVwbG95bWVudF9pZCI6IjQ2MTo1NDQwYTA4NDIyYWIxZWU3Nzk0YTA1ODhiNWU0Y2I0YTA5NGM0MjU2IiwiZXhwIjoxNjMzNTQyMzczLCJpYXQiOjE2MzM1Mzg3NzMsImlzcyI6Imh0dHBzOi8vY2FudmFzLmluc3RydWN0dXJlLmNvbSIsIm5vbmNlIjoiNWQwNGNiMTJmNDVkZjZlZTM3M2M0MmExY2E0Y2RiZTA4ZTJiZmE4ZTVmN2M2NjJhY2EzZjY1NjA2ODdmZGM0NyIsInN1YiI6IjRmM2QxMmRmLWUxYWUtNDg0Zi04YjlhLWI2Njc4NjRlODEwMCIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vZ29sZGlsb2Nrcy5sdW1lbmxlYXJuaW5nLmNvbS9zdHVkeV9wbGFuLzdjM2EzOTE1LWZmZGEtNGU4Ny05MTQ5LTE2YzFhYzUyM2IyZj9laWQ9YUd3d0x3QndrdFpnelp0cENKWGVaZyIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2NvbnRleHQiOnsiaWQiOiIwYTQ3ZGU5MWNmODRlZTE0N2Y2ZTUzNDE5NTk4ODUwODUwNGQzZTgyIiwibGFiZWwiOiJtZ3dvemR6LWx1bWVuIiwidGl0bGUiOiJtZ3dvemR6IEx1bWVuIFRlc3QiLCJ0eXBlIjpbImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2NvdXJzZSNDb3Vyc2VPZmZlcmluZyJdLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3Rvb2xfcGxhdGZvcm0iOnsiZ3VpZCI6InBvdDI4eU5wRFNaczFGdk1RYTQyTWlQV2xST0xRQ0FlZHpRWDZNYzI6Y2FudmFzLWxtcyIsIm5hbWUiOiJVbmljb24iLCJ2ZXJzaW9uIjoiY2xvdWQiLCJwcm9kdWN0X2ZhbWlseV9jb2RlIjoiY2FudmFzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9sYXVuY2hfcHJlc2VudGF0aW9uIjp7ImRvY3VtZW50X3RhcmdldCI6ImlmcmFtZSIsImhlaWdodCI6bnVsbCwid2lkdGgiOm51bGwsInJldHVybl91cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vY291cnNlcy8zMzQ4L2V4dGVybmFsX2NvbnRlbnQvc3VjY2Vzcy9leHRlcm5hbF90b29sX3JlZGlyZWN0IiwibG9jYWxlIjoiZW4iLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImxvY2FsZSI6ImVuIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZXMiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvaW5zdGl0dXRpb24vcGVyc29uI0FkbWluaXN0cmF0b3IiLCJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9pbnN0aXR1dGlvbi9wZXJzb24jSW5zdHJ1Y3RvciIsImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2luc3RpdHV0aW9uL3BlcnNvbiNTdHVkZW50IiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvc3lzdGVtL3BlcnNvbiNVc2VyIl0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2N1c3RvbSI6eyJkdWVfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQuZHVlQXQuaXNvODYwMSIsImxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQubG9ja0F0Lmlzbzg2MDEiLCJ1bmxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQudW5sb2NrQXQuaXNvODYwMSIsImNhbnZhc191c2VyX2lkIjozODYsImNhbnZhc19sb2dpbl9pZCI6Im1nd296ZHpAdW5pY29uLm5ldCIsImNhbnZhc19jb3Vyc2VfaWQiOjMzNDgsImNhbnZhc191c2VyX25hbWUiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJjYW52YXNfYXNzaWdubWVudF9pZCI6NjI3MX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTExX2xlZ2FjeV91c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTFwMSI6eyJ1c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319fSwiZXJyb3JzIjp7ImVycm9ycyI6e319LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3MvY2xhaW0vZW5kcG9pbnQiOnsic2NvcGUiOlsiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtLnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Jlc3VsdC5yZWFkb25seSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9zY29wZS9zY29yZSJdLCJsaW5laXRlbXMiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbGluZV9pdGVtcyIsImxpbmVpdGVtIjoiaHR0cHM6Ly91bmljb24uaW5zdHJ1Y3R1cmUuY29tL2FwaS9sdGkvY291cnNlcy8zMzQ4L2xpbmVfaXRlbXMvNTAzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJwaWN0dXJlIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tL2ltYWdlcy9tZXNzYWdlcy9hdmF0YXItNTAucG5nIiwiZW1haWwiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJuYW1lIjoiTWFyeSBHd296ZHoiLCJnaXZlbl9uYW1lIjoiTWFyeSIsImZhbWlseV9uYW1lIjoiR3dvemR6IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGlzIjp7InBlcnNvbl9zb3VyY2VkaWQiOiJtZ3dvemR6IiwiY291cnNlX29mZmVyaW5nX3NvdXJjZWRpZCI6bnVsbCwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1ucnBzL2NsYWltL25hbWVzcm9sZXNlcnZpY2UiOnsiY29udGV4dF9tZW1iZXJzaGlwc191cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbmFtZXNfYW5kX3JvbGVzIiwic2VydmljZV92ZXJzaW9ucyI6WyIyLjAiXSwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3d3dy5pbnN0cnVjdHVyZS5jb20vcGxhY2VtZW50IjpudWxsfQ.qN6MqcitF8VHuMVk_YmuXnKN43A1TgdjbSV8zHxT6SS5uZo8vtTB2GBGmqPpsknTmJPNijpQsPMoeLN3kKAvkpJ0RJPWMYud89a1SGnons8HuqXxACUCwUa7Lki8j7xSIXB6tgXQqzkrHCPmsBwLQy6rAYseiDNpsLkzIZzBAZjt89262i_UxRScJkhdPF4GEQDw4d2d1YMM5cmSy039BZZyT7AOV4q3qjo_BeFlQ5QJjXDtXGZ5VIcOuyLipAcrG8a2llXd8gLDkxkD0gIwI_zZX2fG-XKHc_co_gp45L2fVz09vKS5R-yCiooZgktOBoe0OEY8vHfCbBk4Vj2sxg";
    private static final String SAMPLE_ISS = "https://platform-lms.com";
    private static final Set<String> SAMPLE_CLIENT_ID_SET = new HashSet<>();
    private static final String SAMPLE_CLIENT_ID = "sample-client-id";
    private static final String SAMPLE_DEPLOYMENT_ID = "sample-deployment-id";
    private static final String SAMPLE_USER = "sample-user";

    @Mock
    private LTIDataService ltiDataService;

    @InjectMocks
    private AllRepositories allRepositories;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private NonceStateRepository nonceStateRepository;

    @Mock
    private HttpServletRequest req;

    @Mock
    private PlatformDeployment platformDeployment;

    @Mock
    Jws<Claims> jwsClaims;

    @Mock
    Claims claims;

    private List<PlatformDeployment> platformDeploymentList;

    private MockHttpSession mockHttpSession = new MockHttpSession();

    @BeforeEach()
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        platformDeploymentList = Arrays.asList(platformDeployment);
        when(ltiDataService.getRepos()).thenReturn(allRepositories);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(req.getSession()).thenReturn(mockHttpSession);
        when(req.getParameter("id_token")).thenReturn(ID_TOKEN);
        SAMPLE_CLIENT_ID_SET.add(SAMPLE_CLIENT_ID);

        Map<String,Object> testMap = new HashMap<>();
        testMap.put("id","value1");
        List<String> testList = new ArrayList<>();
        testList.add("key1");
        when(claims.getExpiration()).thenReturn(new Date());
        when(claims.getIssuedAt()).thenReturn(new Date());
        when(claims.getSubject()).thenReturn(SAMPLE_USER);
        when(claims.containsKey(eq(LtiStrings.LTI_LINK))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_LINK), eq(Map.class))).thenReturn(testMap);
        when(claims.containsKey(eq(LtiStrings.LTI_ROLES))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_ROLES), eq(List.class))).thenReturn(testList);
        when(claims.containsKey(eq(LtiStrings.LTI_VERSION))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_VERSION), eq(String.class))).thenReturn(LtiStrings.LTI_VERSION_3);
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
    public void testLTI3RequestWithoutDataService() {
        AssertionError exception = Assertions.assertThrows(
                AssertionError.class,
                () -> {new LTI3Request(req, null, false, "1234", jwsClaims);}
        );
        assertEquals(exception.getMessage(), "LTIDataService cannot be null");
    }

    @Test
    public void testLTI3RequestWithoutDeployment() {
        when(req.getParameter("cookies")).thenReturn("false");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(new ArrayList<>());
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        assertEquals("PlatformDeployment does not exist or is duplicated for issuer: https://platform-lms.com, clientId: sample-client-id, and deploymentId: sample-deployment-id", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithDuplicateDeployment() {
        when(req.getParameter("cookies")).thenReturn("false");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(Arrays.asList(platformDeployment, platformDeployment));
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        assertEquals("PlatformDeployment does not exist or is duplicated for issuer: https://platform-lms.com, clientId: sample-client-id, and deploymentId: sample-deployment-id", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithInvalidMessageType() {
        when(req.getParameter("cookies")).thenReturn("false");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);

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
    public void testLTI3RequestWithMissingNonce() {
        when(req.getParameter("cookies")).thenReturn("true");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        mockHttpSession.setAttribute("lti_nonce", new ArrayList<String>());
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47");

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Nonce = null in the session.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithMissingNonceCookieless() {

        when(req.getParameter("cookies")).thenReturn("false");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        mockHttpSession.setAttribute("lti_nonce", new ArrayList<String>());
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47");

        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);}
        );
        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Nonce not found in the database.", exception.getMessage());
    }

    @Test
    public void testLTI3RequestWithValidNonce() {
        when(req.getParameter("cookies")).thenReturn("false");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.containsKey(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID), eq(String.class))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.containsKey(eq(LtiStrings.LTI_MESSAGE_TYPE))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);

        mockHttpSession.setAttribute("lti_nonce", Arrays.asList("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47"));
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47");
        NonceState nonceState = new NonceState("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47", "HASH_STATE", "VALID_STATE", "_parent");
        when(nonceStateRepository.findByNonce("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47")).thenReturn(nonceState);
        try{
            LTI3Request request = new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);
            verify(platformDeploymentRepository,times(2)).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
            verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
            verify(claims, times(2)).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
            verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        } catch (Exception ex) {
            Assertions.fail("Failure creating the LTI Request");
        }
    }

    @Test
    public void testLTI3RequestWithInvalidNonce() {
        when(req.getParameter("cookies")).thenReturn("true");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.getSubject()).thenReturn(SAMPLE_USER);
        when(claims.containsKey(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID), eq(String.class))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.containsKey(eq(LtiStrings.LTI_MESSAGE_TYPE))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        mockHttpSession.setAttribute("lti_nonce", Arrays.asList("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47"));
        NonceState nonceState = new NonceState("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47", "HASH_STATE", "VALID_STATE", "_parent");
        when(nonceStateRepository.findByNonce("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47")).thenReturn(nonceState);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("ba5938df7756a32ca3a4f89c40d88ff742651bc3ff417f6a4b7443f6f1e6a16c");

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);});

        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Unknown or already used nonce.", exception.getMessage());

    }

    @Test
    public void testLTI3RequestWithInvalidNonceCookieless() {
        when(req.getParameter("cookies")).thenReturn("false");
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(SAMPLE_CLIENT_ID_SET);
        when(claims.containsKey(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.get(eq(LtiStrings.LTI_DEPLOYMENT_ID), eq(String.class))).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(claims.containsKey(eq(LtiStrings.LTI_MESSAGE_TYPE))).thenReturn(true);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(claims.get(eq(LtiStrings.LTI_MESSAGE_TYPE))).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        mockHttpSession.setAttribute("lti_nonce", Arrays.asList("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47"));
        NonceState nonceState = new NonceState("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47", "HASH_STATE", "VALID_STATE", "_parent");
        when(nonceStateRepository.findByNonce("5d04cb12f45df6ee373c42a1ca4cdbe08e2bfa8e5f7c662aca3f6560687fdc47")).thenReturn(nonceState);
        when(claims.get(eq(LtiStrings.LTI_NONCE), eq(String.class))).thenReturn("ba5938df7756a32ca3a4f89c40d88ff742651bc3ff417f6a4b7443f6f1e6a16c");

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {new LTI3Request(req, ltiDataService, false, "1234", jwsClaims);});

        verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class));
        verify(claims).get(eq(LtiStrings.LTI_VERSION), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_MESSAGE_TYPE), eq(String.class));
        verify(claims).get(eq(LtiStrings.LTI_NONCE), eq(String.class));
        assertEquals("Nonce error: Nonce not found in the database.", exception.getMessage());

    }






    private static final String NONCE = "launch-nonce";
    private static final String KID = "platform-kid";
    private static final String JWKS_URL = "https://platform-lms.com/jwks";
    private static KeyPair platformKeys;

    @AfterEach
    public void clearRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static synchronized KeyPair platformKeys() throws Exception {
        if (platformKeys == null) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            platformKeys = kpg.generateKeyPair();
        }
        return platformKeys;
    }

    private static String privateKeyPem() throws Exception {
        return "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(platformKeys().getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
    }

    private static Map<String, Object> launchClaims(String aud, String messageType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", SAMPLE_ISS);
        claims.put("aud", aud);
        claims.put("sub", SAMPLE_USER);
        claims.put("iat", new Date().getTime() / 1000);
        claims.put("exp", new Date().getTime() / 1000 + 3600);
        claims.put(LtiStrings.LTI_NONCE, NONCE);
        claims.put(LtiStrings.LTI_AZP, aud);
        claims.put(LtiStrings.LTI_MESSAGE_TYPE, messageType);
        claims.put(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3);
        claims.put(LtiStrings.LTI_DEPLOYMENT_ID, SAMPLE_DEPLOYMENT_ID);
        claims.put(LtiStrings.LTI_GIVEN_NAME, "Jane");
        claims.put(LtiStrings.LTI_FAMILY_NAME, "Doe");
        claims.put(LtiStrings.LTI_MIDDLE_NAME, "Q");
        claims.put(LtiStrings.LTI_PICTURE, "https://platform-lms.com/jane.png");
        claims.put(LtiStrings.LTI_EMAIL, "jane@example.com");
        claims.put(LtiStrings.LTI_NAME, "Jane Doe");
        claims.put(LtiStrings.LTI_ROLES, List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR));
        claims.put(LtiStrings.LTI_LINK, Map.of("id", "resource-link-1", "title", "Link title", "description", "Link description"));
        claims.put(LtiStrings.LTI_CONTEXT, Map.of("id", "context-1", "label", "CTX", "title", "Context title", "type", List.of(LtiStrings.LTI_CONTEXT_TYPE_COURSE_OFFERING)));
        claims.put(LtiStrings.LTI_PLATFORM, Map.of("name", "Platform", "contact_email", "admin@platform-lms.com", "description", "A platform",
                "url", "https://platform-lms.com", "product_family_code", "canvas", "version", "cloud"));
        claims.put(LtiStrings.LTI_ENDPOINT, Map.of("scope", List.of("lineitem"), "lineitems", "https://platform-lms.com/lineitems"));
        claims.put(LtiStrings.LTI_NAMES_ROLE_SERVICE, Map.of("context_memberships_url", "https://platform-lms.com/memberships", "service_versions", List.of("2.0")));
        claims.put(LtiStrings.LTI_CALIPER_ENDPOINT_SERVICE, Map.of("scopes", List.of("caliper"), "caliper_endpoint_url", "https://platform-lms.com/caliper", "caliper_federated_session_id", "session-1"));
        claims.put(LtiStrings.LTI_LIS, Map.of("person_sourcedid", "jane"));
        claims.put(LtiStrings.LTI_11_LEGACY_USER_ID, "legacy-1");
        claims.put(LtiStrings.LTI_PRES_LOCALE, "es-MX");
        claims.put(LtiStrings.LTI_LAUNCH_PRESENTATION, Map.of("height", 400, "width", "wide", "return_url", "https://platform-lms.com/return", "document_target", "iframe"));
        claims.put(LtiStrings.LTI_CUSTOM, Map.of("canvas_user_id", 386));
        claims.put(LtiStrings.LTI_EXTENSION, Map.of("ext", "value"));
        claims.put(LtiStrings.LTI_TARGET_LINK_URI, "https://tool.example.com/lti3?link=1234");
        if (LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(messageType)) {
            claims.put(LtiStrings.DEEP_LINKING_SETTINGS, Map.of("deep_link_return_url", "https://platform-lms.com/deeplink",
                    "accept_types", List.of("ltiResourceLink"), "accept_media_types", "text/html",
                    "accept_presentation_document_targets", List.of("iframe"), "accept_multiple", "true", "auto_create", "true",
                    "title", "DL title", "text", "DL text", "data", "DL data"));
        }
        return claims;
    }

    private static String signedToken(Map<String, Object> claims) throws Exception {
        return Jwts.builder().header().keyId(KID).and().claims(claims).signWith(platformKeys().getPrivate()).compact();
    }

    private static Jws<Claims> parsed(Map<String, Object> claims) throws Exception {
        return Jwts.parser().verifyWith(platformKeys().getPublic()).build().parseSignedClaims(signedToken(claims));
    }


    private void stubValidLaunch() {
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(platformDeploymentList);
        when(nonceStateRepository.findByNonce(NONCE)).thenReturn(new NonceState(NONCE, "HASH_STATE", "VALID_STATE", "_parent"));
    }

    private static MockHttpServletRequest launchRequest(String cookies) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("cookies", cookies);
        return request;
    }

    private LTI3Request bareRequest() {
        LTI3Request lti = new ObjenesisStd().newInstance(LTI3Request.class);
        lti.setLtiDataService(ltiDataService);
        lti.setHttpServletRequest(req);
        return lti;
    }

    @Test
    public void testResourceLinkLaunchPopulatesTheRequestAndUpdatesTheDatabase() throws Exception {
        stubValidLaunch();
        MockHttpServletRequest request = launchRequest("false");

        LTI3Request lti = new LTI3Request(request, ltiDataService, true, "1234", parsed(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)));

        verify(ltiDataService).loadLTIDataFromDB(lti, "1234");
        verify(ltiDataService).upsertLTIDataInDB(lti, platformDeployment, "1234");
        assertSame(request, lti.getHttpServletRequest());
        assertTrue(lti.isComplete());
        assertTrue(lti.isCorrect());
        assertEquals(SAMPLE_ISS, lti.getIss());
        assertEquals(SAMPLE_CLIENT_ID, lti.getAud());
        assertEquals(SAMPLE_USER, lti.getSub());
        assertEquals(NONCE, lti.getNonce());
        assertEquals(SAMPLE_CLIENT_ID, lti.getAzp());
        assertEquals(SAMPLE_DEPLOYMENT_ID, lti.getLtiDeploymentId());
        assertEquals(LtiStrings.LTI_VERSION_3, lti.getLtiVersion());
        assertEquals("Jane", lti.getLtiGivenName());
        assertEquals("Doe", lti.getLtiFamilyName());
        assertEquals("Q", lti.getLtiMiddleName());
        assertEquals("https://platform-lms.com/jane.png", lti.getLtiPicture());
        assertEquals("jane@example.com", lti.getLtiEmail());
        assertEquals("Jane Doe", lti.getLtiName());
        assertEquals(1, lti.getUserRoleNumber());
        assertTrue(lti.getLtiRoleScopeMentor().isEmpty());
        assertEquals("resource-link-1", lti.getLtiLinkId());
        assertEquals("Link title", lti.getLtiLinkTitle());
        assertEquals("Link description", lti.getLtiLinkDescription());
        assertEquals("context-1", lti.getLtiContextId());
        assertEquals("CTX", lti.getLtiContextLabel());
        assertEquals("Context title", lti.getLtiContextTitle());
        assertEquals(List.of(LtiStrings.LTI_CONTEXT_TYPE_COURSE_OFFERING), lti.getLtiContextType());
        assertEquals("Platform", lti.getLtiToolPlatformName());
        assertEquals("admin@platform-lms.com", lti.getLtiToolPlatformContactEmail());
        assertEquals("A platform", lti.getLtiToolPlatformDesc());
        assertEquals("https://platform-lms.com", lti.getLtiToolPlatformUrl());
        assertEquals("canvas", lti.getLtiToolPlatformProduct());
        assertEquals("canvas", lti.getLtiToolPlatformFamilyCode());
        assertEquals("cloud", lti.getLtiToolPlatformVersion());
        assertEquals(List.of("lineitem"), lti.getLtiEndpointScope());
        assertEquals("https://platform-lms.com/lineitems", lti.getLtiEndpointLineItems());
        assertEquals("https://platform-lms.com/memberships", lti.getLtiNamesRoleServiceContextMembershipsUrl());
        assertEquals(List.of("2.0"), lti.getLtiNamesRoleServiceVersions());
        assertEquals(List.of("caliper"), lti.getLtiCaliperEndpointServiceScopes());
        assertEquals("https://platform-lms.com/caliper", lti.getLtiCaliperEndpointServiceUrl());
        assertEquals("session-1", lti.getLtiCaliperEndpointServiceSessionId());
        assertEquals("jane", lti.getLtiLis().get("person_sourcedid"));
        assertEquals("legacy-1", lti.getLti11LegacyUserId());
        assertEquals("es-MX", lti.getLocale());
        assertEquals(Locale.forLanguageTag("es-MX"), lti.getLtiPresLocale());
        assertEquals(400, lti.getLtiPresHeight());
        // "wide" is not a number
        assertNull(lti.getLtiPresWidth());
        assertEquals("https://platform-lms.com/return", lti.getLtiPresReturnUrl());
        assertEquals("iframe", lti.getLtiPresTarget());
        assertEquals(386, lti.getLtiCustom().get("canvas_user_id"));
        assertEquals("value", lti.getLtiExtension().get("ext"));
        assertEquals("https://tool.example.com/lti3?link=1234", lti.getLtiTargetLinkUrl());
        assertTrue(lti.getDeepLinkingSettings().isEmpty());
        assertNull(lti.getDeepLinkReturnUrl());
        assertNull(lti.getDeepLinkJwts());
    }

    @Test
    public void testDeepLinkingLaunchGeneratesDemoJwtsAndUpdatesWithoutALink() throws Exception {
        stubValidLaunch();
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setClientId(SAMPLE_CLIENT_ID);
        when(platformDeploymentRepository.findByDeploymentId(SAMPLE_DEPLOYMENT_ID)).thenReturn(List.of(deployment));
        when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKeyPem());
        when(ltiDataService.getLocalUrl()).thenReturn("https://tool.example.com");

        LTI3Request lti = new LTI3Request(launchRequest("false"), ltiDataService, true, "1234", parsed(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)));

        verify(ltiDataService).upsertLTIDataInDB(lti, platformDeployment, null);
        assertTrue(lti.isComplete());
        assertTrue(lti.isCorrect());
        assertEquals("https://platform-lms.com/deeplink", lti.getDeepLinkReturnUrl());
        assertEquals(List.of("ltiResourceLink"), lti.getDeepLinkAcceptTypes());
        assertEquals("text/html", lti.getDeepLinkAcceptMediaTypes());
        assertEquals(List.of("iframe"), lti.getDeepLinkAcceptPresentationDocumentTargets());
        assertEquals("true", lti.getDeepLinkAcceptMultiple());
        assertEquals("true", lti.getDeepLinkAutoCreate());
        assertEquals("DL title", lti.getDeepLinkTitle());
        assertEquals("DL text", lti.getDeepLinkText());
        assertEquals("DL data", lti.getDeepLinkData());
        assertNotNull(lti.getDeepLinkJwts());
        assertFalse(lti.getDeepLinkJwts().isEmpty());
    }

    @Test
    public void testDeepLinkingLaunchSurvivesAFailureBuildingTheDemoJwts() throws Exception {
        stubValidLaunch();
        when(platformDeploymentRepository.findByDeploymentId(SAMPLE_DEPLOYMENT_ID)).thenReturn(List.of(new PlatformDeployment()));
        // no private key configured, so building the demo JWTs fails
        when(ltiDataService.getOwnPrivateKey()).thenReturn(null);

        LTI3Request lti = new LTI3Request(launchRequest("false"), ltiDataService, false, "1234", parsed(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)));

        assertTrue(lti.isComplete());
        assertNull(lti.getDeepLinkJwts());
        verify(ltiDataService).loadLTIDataFromDB(lti, "1234");
        verify(ltiDataService, never()).upsertLTIDataInDB(any(), any(), any());
    }

    @Test
    public void testBracketedAudienceIsUnwrapped() throws Exception {
        stubValidLaunch();

        LTI3Request lti = new LTI3Request(launchRequest("false"), ltiDataService, false, "1234", parsed(launchClaims("[" + SAMPLE_CLIENT_ID + "]", LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)));

        assertEquals(SAMPLE_CLIENT_ID, lti.getAud());
        verify(platformDeploymentRepository, times(2)).findByIssAndClientIdAndDeploymentId(SAMPLE_ISS, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID);
    }

    @Test
    public void testHalfBracketedAudienceIsKeptAsIs() throws Exception {
        stubValidLaunch();

        LTI3Request lti = new LTI3Request(launchRequest("false"), ltiDataService, false, "1234", parsed(launchClaims("[" + SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)));

        assertEquals("[" + SAMPLE_CLIENT_ID, lti.getAud());
        verify(platformDeploymentRepository, times(2)).findByIssAndClientIdAndDeploymentId(SAMPLE_ISS, "[" + SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID);
    }

    @Test
    public void testIncompleteLaunchIsRejected() throws Exception {
        stubValidLaunch();
        Map<String, Object> claims = launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        claims.remove(LtiStrings.LTI_ROLES);
        Jws<Claims> jws = parsed(claims);

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> new LTI3Request(launchRequest("false"), ltiDataService, true, "1234", jws));

        assertEquals("Request is not a valid LTI3 request: false", exception.getMessage());
        verify(ltiDataService, never()).loadLTIDataFromDB(any(), any());
    }

    @Test
    public void testValidateAndRetrieveJWTClaimsUsesThePlatformJwks() throws Exception {
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setJwksEndpoint(JWKS_URL);
        when(platformDeploymentRepository.findByIssAndClientId(SAMPLE_ISS, SAMPLE_CLIENT_ID)).thenReturn(List.of(deployment));
        String plain = signedToken(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK));
        String bracketed = signedToken(launchClaims("[" + SAMPLE_CLIENT_ID + "]", LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK));

        try (MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenReturn(platformKeys().getPublic());

            Jws<Claims> result = bareRequest().validateAndRetrieveJWTClaims(ltiDataService, plain);
            assertEquals(SAMPLE_USER, result.getPayload().getSubject());
            assertEquals(NONCE, result.getPayload().get(LtiStrings.LTI_NONCE));

            Jws<Claims> bracketedResult = bareRequest().validateAndRetrieveJWTClaims(ltiDataService, bracketed);
            assertEquals(SAMPLE_USER, bracketedResult.getPayload().getSubject());
            jwkSet.verify(() -> PlatformJwks.publicKey(JWKS_URL, KID), times(2));
        }
        verify(platformDeploymentRepository, times(2)).findByIssAndClientId(SAMPLE_ISS, SAMPLE_CLIENT_ID);
    }

    @Test
    public void testValidateAndRetrieveJWTClaimsKeepsAHalfBracketedAudience() throws Exception {
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setJwksEndpoint(JWKS_URL);
        when(platformDeploymentRepository.findByIssAndClientId(SAMPLE_ISS, "[" + SAMPLE_CLIENT_ID)).thenReturn(List.of(deployment));
        String token = signedToken(launchClaims("[" + SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK));

        try (MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenReturn(platformKeys().getPublic());
            assertEquals(SAMPLE_USER, bareRequest().validateAndRetrieveJWTClaims(ltiDataService, token).getPayload().getSubject());
        }
    }

    @Test
    public void testValidateAndRetrieveJWTClaimsFailsWithoutAJwksEndpoint() throws Exception {
        when(platformDeploymentRepository.findByIssAndClientId(SAMPLE_ISS, SAMPLE_CLIENT_ID)).thenReturn(List.of(new PlatformDeployment()));
        String token = signedToken(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK));

        Assertions.assertThrows(JwtException.class, () -> bareRequest().validateAndRetrieveJWTClaims(ltiDataService, token));
    }

    @Test
    public void testValidateAndRetrieveJWTClaimsFailsWhenTheJwksCannotBeLoaded() throws Exception {
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setJwksEndpoint(JWKS_URL);
        when(platformDeploymentRepository.findByIssAndClientId(SAMPLE_ISS, SAMPLE_CLIENT_ID)).thenReturn(List.of(deployment));
        String token = signedToken(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK));

        try (MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenThrow(new KeySourceException("unreachable"));
            Assertions.assertThrows(JwtException.class, () -> bareRequest().validateAndRetrieveJWTClaims(ltiDataService, token));
        }
    }

    @Test
    public void testGetInstanceWithoutARequestReturnsNull() {
        RequestContextHolder.resetRequestAttributes();
        assertNull(LTI3Request.getInstance("1234"));
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> LTI3Request.getInstanceOrDie("1234"));
        assertEquals("No ServletRequestAttributes can be found, cannot get the LTIRequest unless we are currently in a request", exception.getMessage());
    }

    @Test
    public void testGetInstanceReturnsTheRequestAttribute() {
        LTI3Request existing = bareRequest();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(LTI3Request.class.getName(), existing);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertSame(existing, LTI3Request.getInstance("1234"));
        assertSame(existing, LTI3Request.getInstanceOrDie("1234"));
    }

    @Test
    public void testGetInstanceOrDieFailsWithoutADataService() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(launchRequest("false")));
        try (MockedStatic<ApplicationConfig> config = mockStatic(ApplicationConfig.class)) {
            config.when(ApplicationConfig::getContext).thenReturn(null);
            IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> LTI3Request.getInstanceOrDie("1234"));
            assertTrue(exception.getMessage().startsWith("Invalid LTI request, cannot create LTIRequest from request: "), exception.getMessage());
            assertNull(LTI3Request.getInstance("1234"));
        }
    }

    @Test
    public void testGetInstanceOrDieBuildsARequestFromTheIdToken() throws Exception {
        LTIDataServiceImpl dataService = mock(LTIDataServiceImpl.class);
        when(dataService.getRepos()).thenReturn(allRepositories);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(LTIDataServiceImpl.class)).thenReturn(dataService);
        PlatformDeployment deployment = new PlatformDeployment();
        deployment.setJwksEndpoint(JWKS_URL);
        when(platformDeploymentRepository.findByIssAndClientId(SAMPLE_ISS, SAMPLE_CLIENT_ID)).thenReturn(List.of(deployment));
        stubValidLaunch();
        MockHttpServletRequest request = launchRequest("false");
        request.setParameter("id_token", signedToken(launchClaims(SAMPLE_CLIENT_ID, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try (MockedStatic<ApplicationConfig> config = mockStatic(ApplicationConfig.class);
             MockedStatic<PlatformJwks> jwkSet = mockStatic(PlatformJwks.class)) {
            config.when(ApplicationConfig::getContext).thenReturn(context);
            jwkSet.when(() -> PlatformJwks.publicKey(anyString(), anyString())).thenReturn(platformKeys().getPublic());

            LTI3Request lti = LTI3Request.getInstanceOrDie("1234");

            assertEquals(SAMPLE_USER, lti.getSub());
            assertEquals("resource-link-1", lti.getLtiLinkId());
            verify(dataService).loadLTIDataFromDB(lti, "1234");
            verify(dataService).upsertLTIDataInDB(lti, platformDeployment, "1234");
        }
    }

    @Test
    public void testProcessRequestParametersToleratesMalformedClaims() {
        Map<String, Object> context = new HashMap<>();
        context.put(LtiStrings.LTI_CONTEXT_ID, null);
        context.put(LtiStrings.LTI_CONTEXT_TYPE, "not-a-list");
        Map<String, Object> presentation = new HashMap<>();
        presentation.put(LtiStrings.LTI_PRES_HEIGHT, "tall");
        presentation.put(LtiStrings.LTI_PRES_WIDTH, 300);
        when(claims.getIssuer()).thenReturn(SAMPLE_ISS);
        when(claims.getAudience()).thenReturn(Set.of(SAMPLE_CLIENT_ID));
        when(claims.containsKey(LtiStrings.LTI_NONCE)).thenReturn(true);
        when(claims.containsKey(LtiStrings.LTI_MESSAGE_TYPE)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE)).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        when(claims.containsKey(LtiStrings.LTI_CONTEXT)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_CONTEXT, Map.class)).thenReturn(context);
        when(claims.containsKey(LtiStrings.LTI_LAUNCH_PRESENTATION)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_LAUNCH_PRESENTATION, Map.class)).thenReturn(presentation);
        when(claims.containsKey(LtiStrings.LTI_PLATFORM)).thenReturn(true);
        when(claims.get(LtiStrings.LTI_PLATFORM, Map.class)).thenThrow(new RequiredTypeException("not a map"));
        when(claims.get(LtiStrings.LTI_ROLES, List.class)).thenThrow(new RequiredTypeException("not a list"));
        LTI3Request lti = bareRequest();

        Boolean valid = lti.processRequestParameters(null, jwsClaims);

        assertFalse(valid);
        assertSame(req, lti.getHttpServletRequest());
        assertNull(lti.getNonce());
        assertNull(lti.getLtiContextId());
        assertTrue(lti.getLtiContextType().isEmpty());
        assertNull(lti.getLtiPresHeight());
        assertEquals(300, lti.getLtiPresWidth());
        assertTrue(lti.getLtiToolPlatform().isEmpty());
        assertTrue(lti.getLtiRoles().isEmpty());
        assertEquals(Locale.getDefault(), lti.getLtiPresLocale());
        assertTrue(lti.checkCompleteLTIRequest().contains("Lti Roles is empty"));

        MockHttpServletRequest other = new MockHttpServletRequest();
        lti.processRequestParameters(other, jwsClaims);
        assertSame(other, lti.getHttpServletRequest());
    }

    @Test
    public void testProcessRequestParametersRequiresAServletRequest() {
        LTI3Request lti = bareRequest();
        lti.setHttpServletRequest(null);
        Assertions.assertThrows(AssertionError.class, () -> lti.processRequestParameters(null, jwsClaims));
    }

    @Test
    public void testCheckCompleteLTIRequestListsEverythingMissing() {
        LTI3Request lti = bareRequest();
        String result = lti.checkCompleteLTIRequest();
        assertTrue(result.contains("Lti Deployment Id is empty"));
        assertTrue(result.contains("Lti Resource Link is empty"));
        assertTrue(result.contains("User (sub) is empty"));
        assertTrue(result.contains("Lti Roles is empty"));
        assertTrue(result.contains("Exp is empty or invalid"));
        assertTrue(result.contains("Iat is empty or invalid"));

        lti.setLtiResourceLink(new HashMap<>());
        lti.setLtiRoles(new ArrayList<>());
        result = lti.checkCompleteLTIRequest();
        assertTrue(result.contains("Lti Resource Link is empty"));
        assertTrue(result.contains("Lti Roles is empty"));

        lti.setLtiResourceLink(Map.of("title", "no id"));
        result = lti.checkCompleteLTIRequest();
        assertTrue(result.contains("Lti Resource Link ID is empty"));
        assertFalse(result.contains("Lti Resource Link is empty"));
    }

    @Test
    public void testCheckCompleteDeepLinkingRequest() {
        LTI3Request lti = bareRequest();
        String result = lti.checkCompleteDeepLinkingRequest();
        assertTrue(result.contains("Lti Deployment Id is empty"));
        assertTrue(result.contains("User (sub) is empty"));
        assertTrue(result.contains("Exp is empty or invalid"));
        assertTrue(result.contains("Iat is empty or invalid"));
        assertTrue(result.contains("DeepLinkingSettings is empty or invalid"));
        assertTrue(result.contains("deepLinkReturnUrl is empty"));
        assertTrue(result.contains("deepLink AcceptTypes is empty"));
        assertTrue(result.contains("deepLink AcceptPresentationDocumentTargets is empty"));

        lti.setDeepLinkingSettings(new HashMap<>());
        lti.setDeepLinkAcceptTypes(new ArrayList<>());
        lti.setDeepLinkAcceptPresentationDocumentTargets(new ArrayList<>());
        result = lti.checkCompleteDeepLinkingRequest();
        assertTrue(result.contains("DeepLinkingSettings is empty or invalid"));
        assertTrue(result.contains("deepLink AcceptTypes is empty"));
        assertTrue(result.contains("deepLink AcceptPresentationDocumentTargets is empty"));

        lti.setLtiDeploymentId(SAMPLE_DEPLOYMENT_ID);
        lti.setSub(SAMPLE_USER);
        lti.setExp(new Date());
        lti.setIat(new Date());
        lti.setDeepLinkingSettings(Map.of("deep_link_return_url", "https://platform-lms.com/deeplink"));
        lti.setDeepLinkReturnUrl("https://platform-lms.com/deeplink");
        lti.setDeepLinkAcceptTypes(List.of("ltiResourceLink"));
        lti.setDeepLinkAcceptPresentationDocumentTargets(List.of("iframe"));
        assertEquals("true", lti.checkCompleteDeepLinkingRequest());
    }

    @Test
    public void testCheckNonceWithoutANonce() {
        assertEquals("Nonce = null in the JWT or in the session.", bareRequest().checkNonce(jwsClaims, true));
    }

    @Test
    public void testCheckNonceConsumesTheMatchingSessionNonce() {
        when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(NONCE);
        when(nonceStateRepository.findByNonce(NONCE)).thenReturn(new NonceState(NONCE, "HASH_STATE", "VALID_STATE", "_parent"));
        mockHttpSession.setAttribute("lti_nonce", Arrays.asList("other-nonce", NONCE));

        assertEquals("true", bareRequest().checkNonce(jwsClaims, true));
        assertEquals(List.of("other-nonce"), mockHttpSession.getAttribute("lti_nonce"));
    }

    @Test
    public void testIsLTI3Request() {
        when(claims.get(LtiStrings.LTI_VERSION, String.class)).thenReturn(null);
        assertEquals("LTI Version = null. LTI Message Type = null. ", LTI3Request.isLTI3Request(jwsClaims));

        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        assertEquals("LTI Version = null. ", LTI3Request.isLTI3Request(jwsClaims));

        when(claims.get(LtiStrings.LTI_VERSION, String.class)).thenReturn(LtiStrings.LTI_VERSION_3);
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn("SomethingElse");
        assertEquals("LTI Message Type is not right: SomethingElse. ", LTI3Request.isLTI3Request(jwsClaims));

        when(claims.get(LtiStrings.LTI_VERSION, String.class)).thenReturn("1.1");
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        assertEquals("LTI Version is not right: 1.1", LTI3Request.isLTI3Request(jwsClaims));

        when(claims.get(LtiStrings.LTI_VERSION, String.class)).thenReturn(LtiStrings.LTI_VERSION_3);
        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK, LTI3Request.isLTI3Request(jwsClaims));

        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING, LTI3Request.isLTI3Request(jwsClaims));
    }

    @Test
    public void testMakeUserRoleNum() {
        LTI3Request lti = bareRequest();
        assertEquals(0, lti.makeUserRoleNum(null));
        assertEquals(0, lti.makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER)));
        assertEquals(1, lti.makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR)));
        assertEquals(2, lti.makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR, LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN)));
    }

    @Test
    public void testRoleChecksAndNormalizedRoleName() throws Exception {
        Method normalizedRoleName = LTI3Request.class.getDeclaredMethod("getNormalizedRoleName");
        normalizedRoleName.setAccessible(true);
        LTI3Request lti = bareRequest();

        lti.setUserRoleNumber(2);
        assertFalse(lti.isRoleAdministrator());
        assertFalse(lti.isRoleInstructor());
        assertFalse(lti.isRoleLearner());
        assertEquals(LtiStrings.LTI_ROLE_GENERAL, normalizedRoleName.invoke(lti));

        lti.setLtiRoles(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN));
        assertTrue(lti.isRoleAdministrator());
        assertTrue(lti.isRoleInstructor());
        assertEquals(LtiStrings.LTI_ROLE_ADMIN, normalizedRoleName.invoke(lti));

        lti.setUserRoleNumber(1);
        assertFalse(lti.isRoleAdministrator());
        assertTrue(lti.isRoleInstructor());
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR, normalizedRoleName.invoke(lti));

        lti.setUserRoleNumber(0);
        lti.setLtiRoles(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER));
        assertFalse(lti.isRoleInstructor());
        assertTrue(lti.isRoleLearner());
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER, normalizedRoleName.invoke(lti));

        lti.setLtiRoles(List.of(LtiStrings.LTI_ROLE_GENERAL));
        assertFalse(lti.isRoleLearner());
        assertEquals(LtiStrings.LTI_ROLE_GENERAL, normalizedRoleName.invoke(lti));
    }
}
