package net.unicon.lti.controller.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@WebMvcTest(LTI3Controller.class)
public class LTI3ControllerTest {
    private static String VALID_STATE = "eyJraWQiOiJPV05LRVkiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJsdGlTdGFydGVyIiwic3ViIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tIiwiYXVkIjoiOTcxNDAwMDAwMDAwMDAyMzAiLCJleHAiOjE2MzM1NDE3MTAsIm5iZiI6MTYzMzUzODExMCwiaWF0IjoxNjMzNTM4MTEwLCJqdGkiOiJkYmIzNWI5Ny02MTEzLTQ3YmUtYWYxOC1jYTg3MTM5NTkzMWEiLCJvcmlnaW5hbF9pc3MiOiJodHRwczovL2NhbnZhcy5pbnN0cnVjdHVyZS5jb20iLCJsb2dpbkhpbnQiOiI0MDdhNDE4MjUwN2YwZjM3YTU0ZWM1NWQ1MDE5MzljMWQzZDEwMmU2IiwibHRpTWVzc2FnZUhpbnQiOiJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpJVXpJMU5pSjkuZXlKMlpYSnBabWxsY2lJNkltRXdOek16WldZelpXUXlNbUl5WlRCa1ltSXlZakpoTlRZME5qWm1OakpoTXpFMlptWXlNMlptTkdKa09XSTVZVFpqWmpaaVlqUTVZbVkwTXpsa05UUmlZalJtWXpaalpETmpPRFE1TldGbVpEZzJNbVZoTXpkbE1UZzRORGcyTnpJNFlqUXhPVGcwWm1WbFpqVXhPVEF5TnpGbFptTmxOR0V4T0RVM05UZ3dJaXdpWTJGdWRtRnpYMlJ2YldGcGJpSTZJblZ1YVdOdmJpNXBibk4wY25WamRIVnlaUzVqYjIwaUxDSmpiMjUwWlhoMFgzUjVjR1VpT2lKRGIzVnljMlVpTENKamIyNTBaWGgwWDJsa0lqbzVOekUwTURBd01EQXdNREF3TXpNME9Dd2lZMkZ1ZG1GelgyeHZZMkZzWlNJNkltVnVJaXdpWlhod0lqb3hOak16TlRNNE5EQTRmUS5lRjh5a1BIVjY4M0FyaFA2MzZUdlNLQ2tDSUxfWEJEX2tFSENfLVJKVHhnIiwidGFyZ2V0TGlua1VyaSI6Imh0dHBzOi8vZ29sZGlsb2Nrcy5sdW1lbmxlYXJuaW5nLmNvbS9zdHVkeV9wbGFuLzdjM2EzOTE1LWZmZGEtNGU4Ny05MTQ5LTE2YzFhYzUyM2IyZj9laWQ9YUd3d0x3QndrdFpnelp0cENKWGVaZyIsImNsaWVudElkIjoiOTcxNDAwMDAwMDAwMDAyMzAiLCJsdGlEZXBsb3ltZW50SWQiOiI0NjE6NTQ0MGEwODQyMmFiMWVlNzc5NGEwNTg4YjVlNGNiNGEwOTRjNDI1NiIsImNvbnRyb2xsZXIiOiIvb2lkYy9sb2dpbl9pbml0aWF0aW9ucyJ9.MyLodskYaqWNDqMbjQpmB8izEEfGRAI58KvMqtaXtkP0RL9SKFLV8hTOHPZsDhgmgGTDL71wRa6kxLEEBiXImDMDSpgkTZIgB3vf1vBmcBz03zZWa0uHHVlyLxQwWJTB65E-w6RlxJuM9wphxUVpdvRXhBr1jHiVKGdgFOm2MkJNKMOdEIQ7sT1l7anTElvcEOtYt7KqSxLFPDRORvSf1Pv3gbY5_IBR1SjLZw3h788_BFH9QSqU4yhJxHdn-tFBoycdnjJ9qqtFor6m7m44U76kDpjCU3b5XLFWOogqbb8sbTLgfwLk0-UFQTENOOUiWih1wA6Fk66vuMX3yHXUOw";
    private static String ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjEtMDktMDFUMDA6MTQ6MzdaIn0.eyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9tZXNzYWdlX3R5cGUiOiJMdGlSZXNvdXJjZUxpbmtSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcmVzb3VyY2VfbGluayI6eyJpZCI6IjcxZTViNWNiLTZmMDUtNGQ1My1iN2U5LWQxMmVjZDQ3NWU3NiIsImRlc2NyaXB0aW9uIjoiIiwidGl0bGUiOiJTdHVkeSBQbGFuOiBUaGUgRXRpb2xvZ3kgYW5kIFRyZWF0bWVudCBvZiBNZW50YWwgRGlzb3JkZXJzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJhdWQiOiI5NzE0MDAwMDAwMDAwMDIzMCIsImF6cCI6Ijk3MTQwMDAwMDAwMDAwMjMwIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vZGVwbG95bWVudF9pZCI6IjQ2MTo1NDQwYTA4NDIyYWIxZWU3Nzk0YTA1ODhiNWU0Y2I0YTA5NGM0MjU2IiwiZXhwIjoxNjMzNTQyMzczLCJpYXQiOjE2MzM1Mzg3NzMsImlzcyI6Imh0dHBzOi8vY2FudmFzLmluc3RydWN0dXJlLmNvbSIsIm5vbmNlIjoiNWQwNGNiMTJmNDVkZjZlZTM3M2M0MmExY2E0Y2RiZTA4ZTJiZmE4ZTVmN2M2NjJhY2EzZjY1NjA2ODdmZGM0NyIsInN1YiI6IjRmM2QxMmRmLWUxYWUtNDg0Zi04YjlhLWI2Njc4NjRlODEwMCIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vZ29sZGlsb2Nrcy5sdW1lbmxlYXJuaW5nLmNvbS9zdHVkeV9wbGFuLzdjM2EzOTE1LWZmZGEtNGU4Ny05MTQ5LTE2YzFhYzUyM2IyZj9laWQ9YUd3d0x3QndrdFpnelp0cENKWGVaZyIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2NvbnRleHQiOnsiaWQiOiIwYTQ3ZGU5MWNmODRlZTE0N2Y2ZTUzNDE5NTk4ODUwODUwNGQzZTgyIiwibGFiZWwiOiJtZ3dvemR6LWx1bWVuIiwidGl0bGUiOiJtZ3dvemR6IEx1bWVuIFRlc3QiLCJ0eXBlIjpbImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2NvdXJzZSNDb3Vyc2VPZmZlcmluZyJdLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3Rvb2xfcGxhdGZvcm0iOnsiZ3VpZCI6InBvdDI4eU5wRFNaczFGdk1RYTQyTWlQV2xST0xRQ0FlZHpRWDZNYzI6Y2FudmFzLWxtcyIsIm5hbWUiOiJVbmljb24iLCJ2ZXJzaW9uIjoiY2xvdWQiLCJwcm9kdWN0X2ZhbWlseV9jb2RlIjoiY2FudmFzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9sYXVuY2hfcHJlc2VudGF0aW9uIjp7ImRvY3VtZW50X3RhcmdldCI6ImlmcmFtZSIsImhlaWdodCI6bnVsbCwid2lkdGgiOm51bGwsInJldHVybl91cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vY291cnNlcy8zMzQ4L2V4dGVybmFsX2NvbnRlbnQvc3VjY2Vzcy9leHRlcm5hbF90b29sX3JlZGlyZWN0IiwibG9jYWxlIjoiZW4iLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImxvY2FsZSI6ImVuIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZXMiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvaW5zdGl0dXRpb24vcGVyc29uI0FkbWluaXN0cmF0b3IiLCJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9pbnN0aXR1dGlvbi9wZXJzb24jSW5zdHJ1Y3RvciIsImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2luc3RpdHV0aW9uL3BlcnNvbiNTdHVkZW50IiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvc3lzdGVtL3BlcnNvbiNVc2VyIl0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2N1c3RvbSI6eyJkdWVfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQuZHVlQXQuaXNvODYwMSIsImxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQubG9ja0F0Lmlzbzg2MDEiLCJ1bmxvY2tfYXQiOiIkQ2FudmFzLmFzc2lnbm1lbnQudW5sb2NrQXQuaXNvODYwMSIsImNhbnZhc191c2VyX2lkIjozODYsImNhbnZhc19sb2dpbl9pZCI6Im1nd296ZHpAdW5pY29uLm5ldCIsImNhbnZhc19jb3Vyc2VfaWQiOjMzNDgsImNhbnZhc191c2VyX25hbWUiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJjYW52YXNfYXNzaWdubWVudF9pZCI6NjI3MX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTExX2xlZ2FjeV91c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTFwMSI6eyJ1c2VyX2lkIjoiNDA3YTQxODI1MDdmMGYzN2E1NGVjNTVkNTAxOTM5YzFkM2QxMDJlNiIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319fSwiZXJyb3JzIjp7ImVycm9ycyI6e319LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3MvY2xhaW0vZW5kcG9pbnQiOnsic2NvcGUiOlsiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtLnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Jlc3VsdC5yZWFkb25seSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9zY29wZS9zY29yZSJdLCJsaW5laXRlbXMiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbGluZV9pdGVtcyIsImxpbmVpdGVtIjoiaHR0cHM6Ly91bmljb24uaW5zdHJ1Y3R1cmUuY29tL2FwaS9sdGkvY291cnNlcy8zMzQ4L2xpbmVfaXRlbXMvNTAzIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJwaWN0dXJlIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tL2ltYWdlcy9tZXNzYWdlcy9hdmF0YXItNTAucG5nIiwiZW1haWwiOiJtZ3dvemR6QHVuaWNvbi5uZXQiLCJuYW1lIjoiTWFyeSBHd296ZHoiLCJnaXZlbl9uYW1lIjoiTWFyeSIsImZhbWlseV9uYW1lIjoiR3dvemR6IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGlzIjp7InBlcnNvbl9zb3VyY2VkaWQiOiJtZ3dvemR6IiwiY291cnNlX29mZmVyaW5nX3NvdXJjZWRpZCI6bnVsbCwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1ucnBzL2NsYWltL25hbWVzcm9sZXNlcnZpY2UiOnsiY29udGV4dF9tZW1iZXJzaGlwc191cmwiOiJodHRwczovL3VuaWNvbi5pbnN0cnVjdHVyZS5jb20vYXBpL2x0aS9jb3Vyc2VzLzMzNDgvbmFtZXNfYW5kX3JvbGVzIiwic2VydmljZV92ZXJzaW9ucyI6WyIyLjAiXSwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3d3dy5pbnN0cnVjdHVyZS5jb20vcGxhY2VtZW50IjpudWxsfQ.qN6MqcitF8VHuMVk_YmuXnKN43A1TgdjbSV8zHxT6SS5uZo8vtTB2GBGmqPpsknTmJPNijpQsPMoeLN3kKAvkpJ0RJPWMYud89a1SGnons8HuqXxACUCwUa7Lki8j7xSIXB6tgXQqzkrHCPmsBwLQy6rAYseiDNpsLkzIZzBAZjt89262i_UxRScJkhdPF4GEQDw4d2d1YMM5cmSy039BZZyT7AOV4q3qjo_BeFlQ5QJjXDtXGZ5VIcOuyLipAcrG8a2llXd8gLDkxkD0gIwI_zZX2fG-XKHc_co_gp45L2fVz09vKS5R-yCiooZgktOBoe0OEY8vHfCbBk4Vj2sxg";

    @InjectMocks
    private LTI3Controller lti3Controller = new LTI3Controller();

    @MockBean
    private LTIJWTService ltijwtService;

    @MockBean
    private LTIDataService ltiDataService;

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

    private static MockedStatic<LTI3Request> lti3RequestMockedStatic;

    @Configuration
    static class ContextConfiguration {

    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        lti3RequestMockedStatic = Mockito.mockStatic(LTI3Request.class);
        when(req.getParameter("state")).thenReturn(VALID_STATE);
        when(req.getParameter("link")).thenReturn("https://tool.com/test");
        when(req.getParameter("id_token")).thenReturn(ID_TOKEN);
        when(jwsClaims.getBody()).thenReturn(claims);
        when(ltijwtService.validateState(VALID_STATE)).thenReturn(jwsClaims);
        lti3RequestMockedStatic.when(() -> LTI3Request.getInstance("https://tool.com/test")).thenReturn(lti3Request);
    }

    @AfterEach
    public void close() {
        lti3RequestMockedStatic.close();
    }

    @Test
    public void testLTI3InvalidClientId() {
        when(claims.get("clientId")).thenReturn("client-id-1");
        when(lti3Request.getAud()).thenReturn("bad-client-id");

        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> {lti3Controller.lti3(req, res, model);}
        );
        Mockito.verify(ltijwtService).validateState(VALID_STATE);
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), "Invalid client_id");
    }

    @Test
    public void testLTI3InvalidDeploymentIdAndValidClientId() {
        when(claims.get("clientId")).thenReturn("client-id-1");
        when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
        when(lti3Request.getAud()).thenReturn("client-id-1");
        when(lti3Request.getLtiDeploymentId()).thenReturn("bad-deployment-id");

        ResponseStatusException exception = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> {lti3Controller.lti3(req, res, model);}
        );
        Mockito.verify(ltijwtService).validateState(VALID_STATE);
        assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), "Invalid deployment_id");
    }

    @Test
    public void testLTI3DemoModeOff() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(ltiDataService.getDemoMode()).thenReturn(false);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getClaims()).thenReturn(claims);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute("target"), "https://tool.com/test");
            String finalIdToken = (String) model.getAttribute("id_token");
            assertNotEquals(finalIdToken, ID_TOKEN);

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, "lti3Redirect");

        } catch (NoSuchAlgorithmException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3DemoModeOffWithoutClientIdOrDeploymentIdInState() {
        try {
            when(claims.get("clientId")).thenReturn(null);
            when(claims.get("ltiDeploymentId")).thenReturn(null);
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(ltiDataService.getDemoMode()).thenReturn(false);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getClaims()).thenReturn(claims);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute("target"), "https://tool.com/test");
            String finalIdToken = (String) model.getAttribute("id_token");
            assertNotEquals(finalIdToken, ID_TOKEN);

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, "lti3Redirect");

        } catch (NoSuchAlgorithmException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3DemoModeOn() {
        when(claims.get("clientId")).thenReturn("client-id-1");
        when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
        when(lti3Request.getAud()).thenReturn("client-id-1");
        when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
        when(ltiDataService.getDemoMode()).thenReturn(true);

        String finalResponse = lti3Controller.lti3(req, res, model);
        Mockito.verify(ltijwtService).validateState(VALID_STATE);
        Mockito.verify(ltiDataService).getDemoMode();
        assertEquals(finalResponse, "lti3Redirect");
    }

    @Test
    public void testLTI3DeepLinkingRequest() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(ltiDataService.getDemoMode()).thenReturn(false);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");

            // Set the message type to Deep Linking
            when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
            // Enable the Deep Linking feature
            when(ltiDataService.getDeepLinkingEnabled()).thenReturn(true);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getClaims()).thenReturn(claims);

            String response = lti3Controller.lti3(req, res, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(model.getAttribute("target"), "https://tool.com/test");
            String finalIdToken = (String) model.getAttribute("id_token");
            assertNotEquals(finalIdToken, ID_TOKEN);

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(finalIdToken);
            assertNotNull(finalClaims);

            assertEquals(response, TextConstants.REACT_UI_TEMPLATE);

        } catch (NoSuchAlgorithmException e) {
            fail("Exception should not be thrown.");
        }
    }
}
