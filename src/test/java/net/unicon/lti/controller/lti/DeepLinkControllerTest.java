package net.unicon.lti.controller.lti;

import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.model.lti.dto.DeepLinkJWTDTO;
import net.unicon.lti.model.lti.dto.DeepLinkRequest;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.service.lti.DeepLinkService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.service.lti.NonceStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeepLinkControllerTest {
    private static final String NONCE = "nonce-1";
    private static final String STATE_HASH = "state-hash";
    private static final String ID_TOKEN = "id-token";
    private static final String TOKEN = "nonce-token";
    private static final String STATE = "state-jwt";

    @Mock
    LTIJWTService ltijwtService;

    @Mock
    DeepLinkService deepLinkService;

    @Mock
    NonceStateService nonceStateService;

    private DeepLinkController controller;

    private DeepLinkRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new DeepLinkController(ltijwtService, deepLinkService, nonceStateService);
        request = new DeepLinkRequest();
        request.setNonce(NONCE);
        request.setState_hash(STATE_HASH);
        request.setId_token(ID_TOKEN);
        request.setToken(TOKEN);
        request.setSelectedIds(List.of("a", "b"));
    }

    private static String expectedHash() {
        return Hashing.sha256().hashString(ID_TOKEN + STATE_HASH + NONCE, StandardCharsets.UTF_8).toString();
    }

    @SuppressWarnings("unchecked")
    private static Jws<Claims> jws(Claims claims) {
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getBody()).thenReturn(claims);
        return jws;
    }

    private void grantAccess() {
        when(nonceStateService.getNonce(NONCE)).thenReturn(new NonceState(NONCE, STATE_HASH, STATE, null));
        Jws<Claims> nonceToken = jws(Jwts.claims().add("expected_hash", expectedHash()).build());
        when(ltijwtService.validateNonceState(TOKEN)).thenReturn(nonceToken);
    }

    @SuppressWarnings("unchecked")
    private static void assertError(ResponseEntity<Object> response, String error, String message) {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(error, body.get("error"));
        assertEquals(message, body.get("message"));
    }

    @Test
    public void testDeepLinksToJwtNonceNotFound() throws Exception {
        when(nonceStateService.getNonce(NONCE)).thenReturn(null);
        assertError(controller.deepLinksToJwt(request), "Nonce state not found", "Invalid nonce");
        verify(ltijwtService, never()).validateNonceState(anyString());
    }

    @Test
    public void testDeepLinksToJwtStateHashMismatch() throws Exception {
        when(nonceStateService.getNonce(NONCE)).thenReturn(new NonceState(NONCE, "other-hash", STATE, null));
        assertError(controller.deepLinksToJwt(request), "State does not match", "Invalid State");
        verify(ltijwtService, never()).validateNonceState(anyString());
    }

    @Test
    public void testDeepLinksToJwtInvalidToken() throws Exception {
        when(nonceStateService.getNonce(NONCE)).thenReturn(new NonceState(NONCE, STATE_HASH, STATE, null));
        Jws<Claims> nonceToken = jws(Jwts.claims().add("expected_hash", "wrong").build());
        when(ltijwtService.validateNonceState(TOKEN)).thenReturn(nonceToken);
        assertError(controller.deepLinksToJwt(request), "Token not valid", "Invalid Token");
        verify(ltijwtService, never()).validateState(anyString());
    }

    @Test
    public void testDeepLinksToJwtEmptySelection() throws Exception {
        grantAccess();
        Jws<Claims> stateClaims = jws(Jwts.claims().add("clientId", "client-id").build());
        when(ltijwtService.validateState(STATE)).thenReturn(stateClaims);
        request.setSelectedIds(Collections.emptyList());

        assertError(controller.deepLinksToJwt(request), "Not items selected", "Empty list of items");
        verify(ltijwtService).validateJWT(ID_TOKEN, "client-id");
        verify(deepLinkService, never()).generateDeepLinkJWT(any(), any());
    }

    @Test
    public void testDeepLinksToJwtSuccess() throws Exception {
        grantAccess();
        Jws<Claims> stateClaims = jws(Jwts.claims().add("clientId", "client-id").build());
        when(ltijwtService.validateState(STATE)).thenReturn(stateClaims);
        Jws<Claims> idToken = jws(Jwts.claims().build());
        when(ltijwtService.validateJWT(ID_TOKEN, "client-id")).thenReturn(idToken);
        DeepLinkJWTDTO dto = new DeepLinkJWTDTO(List.of(Map.of("type", "ltiResourceLink")), "signed-jwt");
        when(deepLinkService.generateDeepLinkJWT(List.of("a", "b"), idToken)).thenReturn(dto);

        ResponseEntity<Object> response = controller.deepLinksToJwt(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dto, response.getBody());
    }

    @Test
    public void testDeleteNonceAccessDenied() throws Exception {
        when(nonceStateService.getNonce(NONCE)).thenReturn(null);
        assertError(controller.deleteNonce(request), "Nonce state not found", "Invalid nonce");
        verify(nonceStateService, never()).deleteNonce(anyString());
    }

    @Test
    public void testDeleteNonceSuccess() throws Exception {
        grantAccess();
        ResponseEntity<Object> response = controller.deleteNonce(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Deleted", response.getBody());
        verify(nonceStateService).deleteNonce(NONCE);
    }

    @Test
    public void testDeleteNonceFailure() throws Exception {
        grantAccess();
        doThrow(new RuntimeException("db down")).when(nonceStateService).deleteNonce(NONCE);
        assertError(controller.deleteNonce(request), "Error deleting nonce", "Error deleting nonce");
    }
}
