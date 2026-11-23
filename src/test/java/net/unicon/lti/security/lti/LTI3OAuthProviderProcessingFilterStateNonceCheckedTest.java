package net.unicon.lti.security.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LTI3OAuthProviderProcessingFilterStateNonceCheckedTest {

    private static final String STATE_HASH = "state-hash";
    private static final String STATE = "state-jwt";
    private static final String NONCE = "nonce-1";
    private static final String ID_TOKEN = "id-token";
    private static final String CLIENT_ID = "client-1";

    @InjectMocks
    private AllRepositories allRepositories;

    @Mock
    private NonceStateRepository nonceStateRepository;

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private LTIJWTService ltijwtService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jws<Claims> stateJws;

    @Mock
    private Claims stateClaims;

    @Mock
    private Jws<Claims> idTokenJws;

    private LTI3OAuthProviderProcessingFilterStateNonceChecked filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new LTI3OAuthProviderProcessingFilterStateNonceChecked(ltiDataService, ltijwtService);
        request = new MockHttpServletRequest();
        request.setParameter("state", STATE_HASH);
        request.setParameter("expected_state", STATE_HASH);
        request.setParameter("nonce", NONCE);
        request.setParameter("expected_nonce", NONCE);
        request.setParameter("cookies", "false");
        request.getSession().setAttribute("lti_state", new ArrayList<>(List.of(STATE_HASH)));
        response = new MockHttpServletResponse();

        when(ltiDataService.getRepos()).thenReturn(allRepositories);
        NonceState nonceState = new NonceState(NONCE, STATE_HASH, STATE, "_parent");
        when(nonceStateRepository.findByStateHash(STATE_HASH)).thenReturn(nonceState);
        when(nonceStateRepository.existsByNonce(NONCE)).thenReturn(true);
        when(nonceStateRepository.findByNonce(NONCE)).thenReturn(nonceState);
        when(ltijwtService.validateState(STATE)).thenReturn(stateJws);
        when(stateJws.getBody()).thenReturn(stateClaims);
        when(stateClaims.get("nonce", String.class)).thenReturn(NONCE);
        when(stateClaims.getAudience()).thenReturn(Set.of(CLIENT_ID));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pwd"));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void assertRejected(String message) {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, filterChain));
        assertEquals(message, e.getMessage());
        Mockito.verifyNoInteractions(filterChain);
    }

    @Test
    public void testConstructorRejectsNullServices() {
        assertThrows(AssertionError.class, () -> new LTI3OAuthProviderProcessingFilterStateNonceChecked(null, ltijwtService));
        assertThrows(AssertionError.class, () -> new LTI3OAuthProviderProcessingFilterStateNonceChecked(ltiDataService, null));
    }

    @Test
    public void testNonHttpRequestIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(mock(ServletRequest.class), mock(ServletResponse.class), filterChain));
    }

    @Test
    public void testUnknownExpectedStateIsRejected() {
        when(nonceStateRepository.findByStateHash(STATE_HASH)).thenReturn(null);
        assertRejected("LTI request doesn't contains the expected state");
    }

    @Test
    public void testNoCookiesWithoutIdTokenPasses() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(ltijwtService, never()).validateJWT(any(), any());
        assertNull(request.getAttribute("LTI3"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testNoCookiesStateMismatchIsRejected() {
        request.setParameter("state", "other-state");
        assertRejected("LTI expected state does not match the retrieved state");
    }

    @Test
    public void testNoCookiesNonceMismatchIsRejected() {
        request.setParameter("nonce", "other-nonce");
        assertRejected("LTI expected nonce does not match the retrieved nonce");
    }

    @Test
    public void testNoCookiesNonceInStateMismatchIsRejected() {
        when(stateClaims.get("nonce", String.class)).thenReturn("other-nonce");
        assertRejected("LTI expected nonce does not match the nonce in the state");
    }

    @Test
    public void testNoCookiesNonceStoredForAnotherStateIsRejected() {
        when(nonceStateRepository.findByNonce(NONCE)).thenReturn(new NonceState(NONCE, "other-hash", STATE, null));
        assertRejected("LTI request doesn't contains the expected state/nonce");
    }

    @Test
    public void testNoCookiesUnknownNonceIsRejected() {
        when(nonceStateRepository.existsByNonce(NONCE)).thenReturn(false);
        assertRejected("LTI request doesn't contains a valid nonce");
    }

    @Test
    public void testCookiesWithKnownStatePasses() throws Exception {
        request.setParameter("cookies", "true");

        filter.doFilter(request, response, filterChain);

        verify(ltijwtService).validateState(STATE);
        verify(nonceStateRepository, never()).existsByNonce(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testCookiesWithUnknownStateIsRejected() {
        request.setParameter("cookies", "true");
        request.getSession().setAttribute("lti_state", new ArrayList<>(List.of("other-state")));
        assertRejected("LTI request doesn't contains the expected state");
    }

    @Test
    public void testUnprocessedStateIsRejected() {
        request.setParameter("cookies", "true");
        when(ltijwtService.validateState(STATE)).thenReturn(null);
        assertRejected("LTI state was not processed as expected");
    }

    @Test
    public void testInvalidIdTokenSkipsTheLtiRequest() throws Exception {
        request.setParameter("id_token", ID_TOKEN);
        when(ltijwtService.validateJWT(ID_TOKEN, CLIENT_ID)).thenReturn(null);

        try (MockedConstruction<LTI3Request> constructed = Mockito.mockConstruction(LTI3Request.class)) {
            filter.doFilter(request, response, filterChain);

            assertEquals(0, constructed.constructed().size());
        }
        assertNull(request.getAttribute("LTI3"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testValidIdTokenAddsTheLtiRequest() throws Exception {
        request.setParameter("id_token", ID_TOKEN);
        when(ltijwtService.validateJWT(ID_TOKEN, CLIENT_ID)).thenReturn(idTokenJws);

        try (MockedConstruction<LTI3Request> constructed = Mockito.mockConstruction(LTI3Request.class, (mock, context) -> {
            when(mock.isLoaded()).thenReturn(true);
            when(mock.isComplete()).thenReturn(true);
            when(mock.getLtiMessageType()).thenReturn("LtiResourceLinkRequest");
        })) {
            filter.doFilter(request, response, filterChain);

            assertSame(constructed.constructed().get(0), request.getAttribute(LTI3Request.class.getName()));
        }
        assertEquals(true, request.getAttribute("LTI3"));
        assertEquals(true, request.getAttribute("lti3_valid"));
        assertEquals("LtiResourceLinkRequest", request.getAttribute("lti3_message_type"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testIncompleteLtiRequestIsMarkedInvalid() throws Exception {
        request.setParameter("id_token", ID_TOKEN);
        when(ltijwtService.validateJWT(ID_TOKEN, CLIENT_ID)).thenReturn(idTokenJws);

        try (MockedConstruction<LTI3Request> ignored = Mockito.mockConstruction(LTI3Request.class,
                (mock, context) -> when(mock.isLoaded()).thenReturn(true))) {
            filter.doFilter(request, response, filterChain);
        }
        assertEquals(false, request.getAttribute("lti3_valid"));
    }

    @Test
    public void testUnloadedLtiRequestIsMarkedInvalid() throws Exception {
        request.setParameter("id_token", ID_TOKEN);
        when(ltijwtService.validateJWT(ID_TOKEN, CLIENT_ID)).thenReturn(idTokenJws);

        try (MockedConstruction<LTI3Request> ignored = Mockito.mockConstruction(LTI3Request.class)) {
            filter.doFilter(request, response, filterChain);
        }
        assertEquals(false, request.getAttribute("lti3_valid"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDataServiceErrorStopsTheChain() throws Exception {
        request.setParameter("id_token", ID_TOKEN);
        when(ltijwtService.validateJWT(ID_TOKEN, CLIENT_ID)).thenAnswer(invocation -> {
            throw new DataServiceException("db down");
        });

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        Mockito.verifyNoInteractions(filterChain);
    }

    @Test
    public void testExpiredStateIsUnauthorized() throws Exception {
        when(stateClaims.getSubject()).thenReturn("user-1");
        when(ltijwtService.validateState(STATE)).thenThrow(new ExpiredJwtException(null, stateClaims, "expired"));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        Mockito.verifyNoInteractions(filterChain);
    }

    @Test
    public void testBadSignatureIsUnauthorized() throws Exception {
        when(ltijwtService.validateState(STATE)).thenThrow(new SignatureException("bad signature"));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        Mockito.verifyNoInteractions(filterChain);
    }
}
