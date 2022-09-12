package net.unicon.lti.controller.lti;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.harmony.HarmonyService;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@WebMvcTest(LTI3Controller.class)
public class LTI3ControllerTest {
    private static final String VALID_STATE = "eyJraWQiOiJPV05LRVkiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJsdGlTdGFydGVyIiwic3ViIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tIiwiYXVkIjoiOTcxNDAwMDAwMDAwMDAyMzAiLCJleHAiOjE2MzM1NDE3MTAsIm5iZiI6MTYzMzUzODExMCwiaWF0IjoxNjMzNTM4MTEwLCJqdGkiOiJkYmIzNWI5Ny02MTEzLTQ3YmUtYWYxOC1jYTg3MTM5NTkzMWEiLCJvcmlnaW5hbF9pc3MiOiJodHRwczovL2NhbnZhcy5pbnN0cnVjdHVyZS5jb20iLCJsb2dpbkhpbnQiOiI0MDdhNDE4MjUwN2YwZjM3YTU0ZWM1NWQ1MDE5MzljMWQzZDEwMmU2IiwibHRpTWVzc2FnZUhpbnQiOiJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpJVXpJMU5pSjkuZXlKMlpYSnBabWxsY2lJNkltRXdOek16WldZelpXUXlNbUl5WlRCa1ltSXlZakpoTlRZME5qWm1OakpoTXpFMlptWXlNMlptTkdKa09XSTVZVFpqWmpaaVlqUTVZbVkwTXpsa05UUmlZalJtWXpaalpETmpPRFE1TldGbVpEZzJNbVZoTXpkbE1UZzRORGcyTnpJNFlqUXhPVGcwWm1WbFpqVXhPVEF5TnpGbFptTmxOR0V4T0RVM05UZ3dJaXdpWTJGdWRtRnpYMlJ2YldGcGJpSTZJblZ1YVdOdmJpNXBibk4wY25WamRIVnlaUzVqYjIwaUxDSmpiMjUwWlhoMFgzUjVjR1VpT2lKRGIzVnljMlVpTENKamIyNTBaWGgwWDJsa0lqbzVOekUwTURBd01EQXdNREF3TXpNME9Dd2lZMkZ1ZG1GelgyeHZZMkZzWlNJNkltVnVJaXdpWlhod0lqb3hOak16TlRNNE5EQTRmUS5lRjh5a1BIVjY4M0FyaFA2MzZUdlNLQ2tDSUxfWEJEX2tFSENfLVJKVHhnIiwidGFyZ2V0TGlua1VyaSI6Imh0dHBzOi8vZ29sZGlsb2Nrcy5sdW1lbmxlYXJuaW5nLmNvbS9zdHVkeV9wbGFuLzdjM2EzOTE1LWZmZGEtNGU4Ny05MTQ5LTE2YzFhYzUyM2IyZj9laWQ9YUd3d0x3QndrdFpnelp0cENKWGVaZyIsImNsaWVudElkIjoiOTcxNDAwMDAwMDAwMDAyMzAiLCJsdGlEZXBsb3ltZW50SWQiOiI0NjE6NTQ0MGEwODQyMmFiMWVlNzc5NGEwNTg4YjVlNGNiNGEwOTRjNDI1NiIsImNvbnRyb2xsZXIiOiIvb2lkYy9sb2dpbl9pbml0aWF0aW9ucyJ9.MyLodskYaqWNDqMbjQpmB8izEEfGRAI58KvMqtaXtkP0RL9SKFLV8hTOHPZsDhgmgGTDL71wRa6kxLEEBiXImDMDSpgkTZIgB3vf1vBmcBz03zZWa0uHHVlyLxQwWJTB65E-w6RlxJuM9wphxUVpdvRXhBr1jHiVKGdgFOm2MkJNKMOdEIQ7sT1l7anTElvcEOtYt7KqSxLFPDRORvSf1Pv3gbY5_IBR1SjLZw3h788_BFH9QSqU4yhJxHdn-tFBoycdnjJ9qqtFor6m7m44U76kDpjCU3b5XLFWOogqbb8sbTLgfwLk0-UFQTENOOUiWih1wA6Fk66vuMX3yHXUOw";
    private static final String ID_TOKEN = "id_token";
    private static final String SAMPLE_LTI_CONTEXT_ID = "sample-lti-context-id";
    private static final String SAMPLE_LINEITEMS_URL = "https://lms.com/lineitems";
    private static final String UNIT_TEST_EXCEPTION_TEXT = "Exception should not be thrown.";
    private static final String SAMPLE_ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjEtMDktMDFUMDA6MTQ6MzdaIn0.eyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9tZXNzYWdlX3R5cGUiOiJMdGlSZXNvdXJjZUxpbmtSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcmVzb3VyY2VfbGluayI6eyJpZCI6IjcxZTViNWNiLTZmMDUtNGQ1My1iN2U5LWQxMmVjZDQ3NWU3NiIsImRlc2NyaXB0aW9uIjoiIiwidGl0bGUiOiJTdHVkeSBQbGFuOiBUaGUgRXRpb2xvZ3kgYW5kIFRyZWF0bWVudCBvZiBNZW50YWwgRGlzb3JkZXJzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJhdWQiOiI5NzE0MDAwMDAwMDAwMDIzMCIsImF6cCI6Ijk3MTQwMDAwMDAwMDAwMjMwIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vZGVwbG95bWVudF9pZCI6IjQ2MTo1NDQwYTA4NDIyYWIxZWU3Nzk0YTA1ODhiNWU0Y2I0YTA5NGM0MjU2IiwiZXhwIjoxNjMzNTQyMzczLCJpYXQiOjE2MzM1Mzg3NzMsImlzcyI6Imh0dHBzOi8vY2FudmFzLmluc3RydWN0dXJlLmNvbSIsIm5vbmNlIjoiNWQwNGNiMTJmNDVkZjZlZTM3M2M0MmExY2E0Y2RiZTA4ZTJiZmE4ZTVmN2M2NjJhY2EzZjY1NjA2ODdmZGM0NyIsInN1YiI6IjRmM2QxMmRmLWUxYWUtNDg0Zi04YjlhLWI2Njc4NjRlODEwMCIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vZ29sZGlsb2Nrcy5sdW1lbmxlYXJuaW5nLmNvbS9zdHVkeV9wbGFuLzdjM2EzOTE1LWZmZGEtNGU4Ny05MTQ5LTE2YzFhYzUyM2IyZj9laWQ9YUd3d0x3QndrdFpnelp0cENKWGVaZyIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2NvbnRleHQiOnsiaWQiOiIwYTQ3ZGU5MWNmODRlZTE0N2Y2ZTUzNDE5NTk4ODUwODUwNGQzZTgyIiwibGFiZWwiOiJtZ3dvemR6LWx1bWVuIiwidGl0bGUiOiJtZ3dvemR6IEx1bWVuIFRlc3QiLCJ0eXBlIjpbImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2NvdXJzZSNDb3Vyc2VPZmZlcmluZyJdLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3Rvb2xfcGxhdGZvcm0iOnsiZ3VpZCI6InBvdDI4eU5wRFNaczFGdk1RYTQyTWlQV2xST0xRQ0FlZHpRWDZNYzI6Y2FudmFzLWxtcyIsIm5hbWUiOiJVbmljb24iLCJ2ZXJzaW9uIjoiY2xvdWQiLCJwcm9kdWN0X2ZhbWlseV9jb2RlIjoiY2FudmFzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9sYXVuY2hfcHJlc2VudGF0aW9uIjp7ImRvY3VtZW50X3RhcmdldCI6ImlmcmFtZSIsImhlaWdodCI6bnVsbCwid2lkdGgiOm51bGwsInJldHVybl91cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vY291cnNlcy8zMzQ4L2V4dGVybmFsX2NvbnRlbnQvc3VjY2Vzcy9leHRlcm5hbF90b29sX3JlZGlyZWN0IiwibG9jYWxlIjoiZW4iLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImxvY2FsZSI6ImVuIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZXMiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvaW5zdGl0dXRpb24vcGVyc29uI0FkbWluaXN0cmF0b3IiLCJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9pbnN0aXR1dGlvbi9wZXJzb24jSW5zdHJ1Y3RvciIsImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2luc3RpdHV0aW9uL3BlcnNvbiNTdHVkZW50IiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvc3lzdGVtL3BlcnNvbiNVc2VyIl0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2N1c3RvbSI6eyJkdWVfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQuZHVlQXQuaXNvODYwMSIsImxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQubG9ja0F0Lmlzbzg2MDEiLCJ1bmxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQudW5sb2NrQXQuaXNvODYwMSIsImNhbnZhc191c2VyX2lkIjozODYsImNhbnZhc19sb2dpbl9pZCI6Im1nd296ZHpAdW5pY29uLm5ldCIsImNhbnZhc19jb3Vyc2VfaWQiOjMzNDgsImNhbnZhc191c2VyX25hbWUiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJjYW52YXNfYXNzaWdubWVudF9pZCI6NjI3MX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTExX2xlZ2FjeV91c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTFwMSI6eyJ1c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319fSwiZXJyb3JzIjp7ImVycm9ycyI6e319LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3MvY2xhaW0vZW5kcG9pbnQiOnsic2NvcGUiOlsiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtLnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Jlc3VsdC5yZWFkb25seSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9zY29wZS9zY29yZSJdLCJsaW5laXRlbXMiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbGluZV9pdGVtcyIsImxpbmVpdGVtIjoiaHR0cHM6Ly91bmljb24uaW5zdHJ1Y3R1cmUuY29tL2FwaS9sdGkvY291cnNlcy8zMzQ4L2xpbmVfaXRlbXMvNTAzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJwaWN0dXJlIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tL2ltYWdlcy9tZXNzYWdlcy9hdmF0YXItNTAucG5nIiwiZW1haWwiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJuYW1lIjoiTWFyeSBHd296ZHoiLCJnaXZlbl9uYW1lIjoiTWFyeSIsImZhbWlseV9uYW1lIjoiR3dvemR6IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGlzIjp7InBlcnNvbl9zb3VyY2VkaWQiOiJtZ3dvemR6IiwiY291cnNlX29mZmVyaW5nX3NvdXJjZWRpZCI6bnVsbCwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1ucnBzL2NsYWltL25hbWVzcm9sZXNlcnZpY2UiOnsiY29udGV4dF9tZW1iZXJzaGlwc191cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbmFtZXNfYW5kX3JvbGVzIiwic2VydmljZV92ZXJzaW9ucyI6WyIyLjAiXSwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3d3dy5pbnN0cnVjdHVyZS5jb20vcGxhY2VtZW50IjpudWxsfQ.qN6MqcitF8VHuMVk_YmuXnKN43A1TgdjbSV8zHxT6SS5uZo8vtTB2GBGmqPpsknTmJPNijpQsPMoeLN3kKAvkpJ0RJPWMYud89a1SGnons8HuqXxACUCwUa7Lki8j7xSIXB6tgXQqzkrHCPmsBwLQy6rAYseiDNpsLkzIZzBAZjt89262i_UxRScJkhdPF4GEQDw4d2d1YMM5cmSy039BZZyT7AOV4q3qjo_BeFlQ5QJjXDtXGZ5VIcOuyLipAcrG8a2llXd8gLDkxkD0gIwI_zZX2fG-XKHc_co_gp45L2fVz09vKS5R-yCiooZgktOBoe0OEY8vHfCbBk4Vj2sxg";
    private static final String CLIENT_ID = "clientId";
    private static final String SAMPLE_CLIENT_ID = "client-id-1";
    private static final String LTI_DEPLOYMENT_ID = "ltiDeploymentId";
    private static final String SAMPLE_DEPLOYMENT_ID = "deployment-id-1";
    private static final String SAMPLE_ISS = "https://lms.com";
    private static final String SAMPLE_TARGET = "https://tool.com/test";
    private static final String TARGET = "target";
    private static final String CANVAS = "canvas";
    private static final String PLATFORM_FAMILY_CODE = "platform_family_code";

    @InjectMocks
    private LTI3Controller lti3Controller = new LTI3Controller();

    @MockBean
    private LTIJWTService ltijwtService;

    @MockBean
    private LTIDataService ltiDataService;

    @Mock
    private AdvantageAGSService advantageAGSService;

    @Mock
    private HarmonyService harmonyService;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private LtiContextRepository ltiContextRepository;

    @InjectMocks
    private AllRepositories allRepositories;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse res;

    private Model model = new ExtendedModelMap();

    @Mock
    Jws<Claims> jwsClaims;

    @Mock
    Claims claims;

    @Mock
    LTI3Request lti3Request;

    private PlatformDeployment platformDeployment = new PlatformDeployment();

    private LtiContextEntity ltiContext = new LtiContextEntity();

    private KeyPair kp;

    private static MockedStatic<LTI3Request> lti3RequestMockedStatic;

    ArgumentCaptor<String> middlewareIdTokenCaptor = ArgumentCaptor.forClass(String.class);

    @Configuration
    static class ContextConfiguration {

    }

    @BeforeEach
    public void setUp() {
        try {
            MockitoAnnotations.openMocks(this);
            lti3RequestMockedStatic = Mockito.mockStatic(LTI3Request.class);
            when(req.getParameter("state")).thenReturn(VALID_STATE);
            when(ltiDataService.getRepos()).thenReturn(allRepositories);
            when(jwsClaims.getBody()).thenReturn(claims);
            when(ltijwtService.validateState(VALID_STATE)).thenReturn(jwsClaims);
            when(req.getParameter("link")).thenReturn(SAMPLE_TARGET);
            when(req.getParameter(ID_TOKEN)).thenReturn(SAMPLE_ID_TOKEN);
            lti3RequestMockedStatic.when(() -> LTI3Request.getInstance(SAMPLE_TARGET)).thenReturn(lti3Request);

            when(claims.get(CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
            when(claims.get(LTI_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISS);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn(SAMPLE_TARGET);
            when(ltiDataService.getDemoMode()).thenReturn(false);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");
            when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_LTI_CONTEXT_ID);
            when(lti3Request.getLtiToolPlatformFamilyCode()).thenReturn("canvas");
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class))).thenReturn(List.of(platformDeployment));
            ltiContext.setLineitems(SAMPLE_LINEITEMS_URL);
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContext);
            ResponseEntity<Map> harmonyResponse = new ResponseEntity<>(Map.of("root_outcome_guid", "test-rog"), HttpStatus.OK);
            LineItems sampleLineItems = new LineItems();
            sampleLineItems.setLineItemList(List.of(new LineItem()));
            when(advantageAGSService.getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL))).thenReturn(sampleLineItems);
            when(harmonyService.postLineitemsToHarmony(any(), anyString())).thenReturn(harmonyResponse);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getClaims()).thenReturn(claims);
        } catch (ConnectionException | JsonProcessingException | DataServiceException | NoSuchAlgorithmException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @AfterEach
    public void close() {
        lti3RequestMockedStatic.close();
    }

    @Test
    public void testLTI3InvalidClientId() {
        try {
            when(lti3Request.getAud()).thenReturn("bad-client-id");

            ResponseStatusException exception = Assertions.assertThrows(
                    ResponseStatusException.class,
                    () -> {lti3Controller.lti3(req, res, model);}
            );
            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
            assertTrue(Objects.requireNonNull(exception.getReason()).contains("Invalid client_id"));

            // validate lineitems not synced
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));
        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLTI3InvalidDeploymentIdAndValidClientId() {
        try {
            when(lti3Request.getLtiDeploymentId()).thenReturn("bad-deployment-id");

            ResponseStatusException exception = Assertions.assertThrows(
                    ResponseStatusException.class,
                    () -> {
                        lti3Controller.lti3(req, res, model);
                    }
            );
            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
            assertTrue(Objects.requireNonNull(exception.getReason()).contains("Invalid deployment_id"));

            // validate lineitems not synced
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));
        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLineitemsNotSyncedIfLineitemsSyncedIsTrue() {
        try {
            ltiContext.setLineitemsSynced(true);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems not synced
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));

            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute("target"), "https://tool.com/test");
            String finalIdToken = (String) model.getAttribute("id_token");
            assertNotEquals(finalIdToken, ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, "lti3Redirect");

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testErrorIfLtiContextIsNull() {
        try {
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), eq(platformDeployment))).thenReturn(null);

            ResponseStatusException exception = Assertions.assertThrows(
                    ResponseStatusException.class,
                    () -> {lti3Controller.lti3(req, res, model);}
            );
            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
            assertTrue(Objects.requireNonNull(exception.getReason()).contains("LTI context should exist for iss https://lms.com, client_id client-id-1, and deployment_id deployment-id-1"));

            // validate lineitems not synced
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));
            Mockito.verify(ltiDataService, never()).getDemoMode();
        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLineitemsSyncedIfLineitemsSyncedIsNull() {
        try {
            ltiContext.setLineitemsSynced(null);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());

            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute("target"), "https://tool.com/test");
            String finalIdToken = (String) model.getAttribute("id_token");
            assertEquals(finalIdToken, middlewareIdTokenCaptor.getValue());
            assertNotEquals(finalIdToken, ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, "lti3Redirect");

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLineItemsSyncedIfLineitemsSyncedIsFalse() {
        try {
            ltiContext.setLineitemsSynced(false);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());

            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute("target"), "https://tool.com/test");
            String finalIdToken = (String) model.getAttribute("id_token");
            assertEquals(finalIdToken, middlewareIdTokenCaptor.getValue());
            assertNotEquals(finalIdToken, ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, "lti3Redirect");

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testExceptionFetchingLineitemsFromLMS() {
        try {
            when(advantageAGSService.getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL))).thenThrow(new ConnectionException("Exception fetching lineitems"));

            ResponseStatusException exception = Assertions.assertThrows(
                    ResponseStatusException.class,
                    () -> {lti3Controller.lti3(req, res, model);}
            );

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
            assertTrue(Objects.requireNonNull(exception.getReason()).contains("Could not fetch lineitems to sync with Harmony"));

            // validate lineitems not synced
            Mockito.verify(ltiDataService).getDemoMode();
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));
        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testNoErrorZeroLineitemsFetchedFromLMS() {
        try {
            LineItems sampleLineItems = new LineItems();
            when(advantageAGSService.getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL))).thenReturn(new LineItems());

            String response = lti3Controller.lti3(req, res, model);

            // validate attempt to fetch lineitems but no sync to Harmony
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, "lti3Redirect");
        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testErrorSendingLineitemsToHarmony() {
        try {
            ResponseEntity<Map> harmonyResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            when(harmonyService.postLineitemsToHarmony(any(LineItems.class), anyString())).thenReturn(harmonyResponse);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems synced
            Mockito.verify(ltiDataService).getDemoMode();
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));

            assertEquals("Harmony Lineitems API returned 500 INTERNAL_SERVER_ERROR\nnull", model.getAttribute("Error"));

            assertEquals("lti3Error", response);

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLTI3StandardLaunch() {
        try {
            when(ltiDataService.getDemoMode()).thenReturn(false);
            ltiContext.setRootOutcomeGuid("root-outcome-guid-1");
            ltiContext.setLineitemsSynced(false);

            String response = lti3Controller.lti3(req, res, model);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());
            assertEquals("root-outcome-guid-1", ltiContext.getRootOutcomeGuid());

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, "lti3Redirect");

        } catch (JsonProcessingException | DataServiceException | ConnectionException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLTI3StandardLaunchLineitemsAlreadySynced() {
        try {
            when(ltiDataService.getDemoMode()).thenReturn(false);
            ltiContext.setRootOutcomeGuid("root-outcome-guid-1");
            ltiContext.setLineitemsSynced(true);

            String response = lti3Controller.lti3(req, res, model);

            // validate lineitems synced
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());
            assertEquals("root-outcome-guid-1", ltiContext.getRootOutcomeGuid());

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, "lti3Redirect");

        } catch (JsonProcessingException | DataServiceException | ConnectionException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLTI3StandardLaunchCopiedCourse() {
        try {
            when(ltiDataService.getDemoMode()).thenReturn(false);
            ltiContext.setRootOutcomeGuid(null);
            ltiContext.setLineitemsSynced(null);

            String response = lti3Controller.lti3(req, res, model);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());
            assertEquals("test-rog", ltiContext.getRootOutcomeGuid());

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, "lti3Redirect");

        } catch (JsonProcessingException | DataServiceException | ConnectionException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLTI3DemoModeOffWithoutClientIdOrDeploymentIdInState() {
        try {
            when(claims.get(CLIENT_ID)).thenReturn(null);
            when(claims.get(LTI_DEPLOYMENT_ID)).thenReturn(null);
            when(ltiDataService.getDemoMode()).thenReturn(false);

            String response = lti3Controller.lti3(req, res, model);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertEquals(finalIdToken, middlewareIdTokenCaptor.getValue());
            assertNotEquals(finalIdToken, ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, "lti3Redirect");

        } catch (JsonProcessingException | DataServiceException | ConnectionException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testLTI3DemoModeOn() {
        try {
            when(ltiDataService.getDemoMode()).thenReturn(true);

            String finalResponse = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems not synced
            Mockito.verify(ltiDataService).getDemoMode();
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), middlewareIdTokenCaptor.capture());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));

            assertEquals(finalResponse, "lti3Redirect");
        } catch (JsonProcessingException | DataServiceException | ConnectionException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testNewCourseFirstLTI3DeepLinkingRequest() {
        try {
            // Set the message type to Deep Linking
            when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // Enable the Deep Linking feature
            when(ltiDataService.getDeepLinkingEnabled()).thenReturn(true);
            ltiContext.setRootOutcomeGuid(null);
            ltiContext.setLineitemsSynced(null);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID))).thenReturn(List.of(new PlatformDeployment()));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), any(PlatformDeployment.class))).thenReturn(ltiContext);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());
            assertEquals("test-rog", ltiContext.getRootOutcomeGuid());

            Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
            Mockito.verify(ltiContextRepository).findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), any(PlatformDeployment.class));
            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            assertEquals(model.getAttribute("root_outcome_guid"), "test-rog");
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, TextConstants.REACT_UI_TEMPLATE);

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testReturningUserPreLineitemsSyncLTI3DeepLinkingRequest() {
        try {
            // Set the message type to Deep Linking
            when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // Enable the Deep Linking feature
            when(ltiDataService.getDeepLinkingEnabled()).thenReturn(true);
            ltiContext.setRootOutcomeGuid("root-outcome-guid-1");
            ltiContext.setLineitemsSynced(false);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID))).thenReturn(List.of(new PlatformDeployment()));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), any(PlatformDeployment.class))).thenReturn(ltiContext);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems synced
            Mockito.verify(advantageAGSService).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());
            assertEquals("root-outcome-guid-1", ltiContext.getRootOutcomeGuid());

            Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
            Mockito.verify(ltiContextRepository).findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), any(PlatformDeployment.class));
            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            assertEquals(model.getAttribute("root_outcome_guid"), "root-outcome-guid-1");
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, TextConstants.REACT_UI_TEMPLATE);

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }

    @Test
    public void testReturningUserPostLineitemsSyncLTI3DeepLinkingRequest() {
        try {
            // Set the message type to Deep Linking
            when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // Enable the Deep Linking feature
            when(ltiDataService.getDeepLinkingEnabled()).thenReturn(true);
            ltiContext.setRootOutcomeGuid("root-outcome-guid-1");
            ltiContext.setLineitemsSynced(true);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID))).thenReturn(List.of(new PlatformDeployment()));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), any(PlatformDeployment.class))).thenReturn(ltiContext);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);

            // validate lineitems not synced
            Mockito.verify(advantageAGSService, never()).getLineItems(eq(platformDeployment), eq(SAMPLE_LINEITEMS_URL));
            Mockito.verify(harmonyService, never()).postLineitemsToHarmony(any(LineItems.class), anyString());
            Mockito.verify(ltiContextRepository, never()).save(eq(ltiContext));
            assertTrue(ltiContext.getLineitemsSynced());
            assertEquals("root-outcome-guid-1", ltiContext.getRootOutcomeGuid());

            Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
            Mockito.verify(ltiContextRepository).findByContextKeyAndPlatformDeployment(eq(SAMPLE_LTI_CONTEXT_ID), any(PlatformDeployment.class));
            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute(TARGET), SAMPLE_TARGET);
            assertEquals(model.getAttribute("root_outcome_guid"), "root-outcome-guid-1");
            String finalIdToken = (String) model.getAttribute(ID_TOKEN);
            assertNotEquals(finalIdToken, SAMPLE_ID_TOKEN);
            assertEquals(CANVAS, model.getAttribute(PLATFORM_FAMILY_CODE));
            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);
            assertEquals(response, TextConstants.REACT_UI_TEMPLATE);

        } catch (ConnectionException | JsonProcessingException | DataServiceException e) {
            fail(UNIT_TEST_EXCEPTION_TEXT);
        }
    }
}
