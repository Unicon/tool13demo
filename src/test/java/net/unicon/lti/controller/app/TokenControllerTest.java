package net.unicon.lti.controller.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.service.app.APIJWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenControllerTest {

    private static final String TOKEN = "one-use-token";

    @InjectMocks
    private TokenController tokenController;

    @Mock
    private APIJWTService apijwtService;

    @Mock
    private HttpServletRequest req;

    @Mock
    private Jws<Claims> jws;

    @Mock
    private Claims claims;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(apijwtService.extractJwtStringValue(req, true)).thenReturn(TOKEN);
        when(apijwtService.validateToken(TOKEN)).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.get("oneUse")).thenReturn(true);
        when(claims.get("roles")).thenReturn(List.of("Instructor"));
        when(claims.get("contextId")).thenReturn(12);
        when(claims.get("contextKey")).thenReturn("context-key");
        when(claims.get("platformDeploymentId")).thenReturn("34");
        when(claims.get("userId")).thenReturn("user-1");
        when(claims.get("nonce")).thenReturn("nonce-1");
    }

    @Test
    public void testTradeOneUseTokenForATimedOne() throws Exception {
        when(apijwtService.buildJwt(false, List.of("Instructor"), 12L, "context-key", 34L, "user-1", "nonce-1")).thenReturn("timed-token");

        ResponseEntity<?> response = tokenController.getTimedToken(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("timed-token", response.getBody());
    }

    @Test
    public void testTradeFailsWhenTheTokenCannotBeBuilt() throws Exception {
        when(apijwtService.buildJwt(anyBoolean(), anyList(), any(), any(), any(), any(), any())).thenThrow(new GeneralSecurityException("no key"));

        ResponseEntity<?> response = tokenController.getTimedToken(req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error generating token: no key", response.getBody());
    }

    @Test
    public void testTradeRejectsReusableTokens() throws Exception {
        when(claims.get("oneUse")).thenReturn(false);

        ResponseEntity<?> response = tokenController.getTimedToken(req);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token passed was not a one time valid token", response.getBody());
        verify(apijwtService, never()).buildJwt(anyBoolean(), anyList(), any(), any(), any(), any(), any());
    }

    @Test
    public void testRefresh() throws Exception {
        when(apijwtService.refreshToken(TOKEN)).thenReturn("refreshed-token");

        ResponseEntity<?> response = tokenController.refreshToken(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("refreshed-token", response.getBody());
    }

    @Test
    public void testRefreshWithBadToken() throws Exception {
        when(apijwtService.refreshToken(TOKEN)).thenThrow(new BadTokenException("bad token"));

        ResponseEntity<?> response = tokenController.refreshToken(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad token", response.getBody());
    }

    @Test
    public void testRefreshSecurityError() throws Exception {
        when(apijwtService.refreshToken(TOKEN)).thenThrow(new GeneralSecurityException("no key"));

        ResponseEntity<?> response = tokenController.refreshToken(req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error generating token", response.getBody());
    }

    @Test
    public void testRefreshIoError() throws Exception {
        when(apijwtService.refreshToken(TOKEN)).thenThrow(new IOException("io"));

        ResponseEntity<?> response = tokenController.refreshToken(req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error generating token", response.getBody());
    }
}
