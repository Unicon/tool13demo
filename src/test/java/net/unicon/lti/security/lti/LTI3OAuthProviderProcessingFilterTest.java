package net.unicon.lti.security.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LTI3OAuthProviderProcessingFilterTest {

    private static final String STATE_HASH = "state-hash";
    private static final String STATE = "state-jwt";
    private static final String NONCE = "nonce-1";

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
    private Jws<Claims> jws;

    @Mock
    private Claims claims;

    private LTI3OAuthProviderProcessingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
        request = new MockHttpServletRequest();
        request.setParameter("state", STATE_HASH);
        response = new MockHttpServletResponse();
        when(ltiDataService.getRepos()).thenReturn(allRepositories);
        when(nonceStateRepository.findByStateHash(STATE_HASH)).thenReturn(new NonceState(NONCE, STATE_HASH, STATE, null));
        when(ltijwtService.validateState(STATE)).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.getId()).thenReturn(NONCE);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pwd"));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testConstructorRejectsNullServices() {
        assertThrows(AssertionError.class, () -> new LTI3OAuthProviderProcessingFilter(null, ltijwtService));
        assertThrows(AssertionError.class, () -> new LTI3OAuthProviderProcessingFilter(ltiDataService, null));
    }

    @Test
    public void testNonHttpRequestIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(mock(ServletRequest.class), mock(ServletResponse.class), filterChain));
    }

    @Test
    public void testValidStatePasses() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testUnknownStateIsServerError() throws Exception {
        when(nonceStateRepository.findByStateHash(STATE_HASH)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertEquals(500, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
        verify(ltijwtService, never()).validateState(any());
    }

    @Test
    public void testNonceMismatchIsServerError() throws Exception {
        when(claims.getId()).thenReturn("another-nonce");

        filter.doFilter(request, response, filterChain);

        assertEquals(500, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testExpiredStateIsUnauthorized() throws Exception {
        when(claims.getSubject()).thenReturn("user-1");
        when(ltijwtService.validateState(STATE)).thenThrow(new ExpiredJwtException(null, claims, "expired"));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testBadSignatureIsUnauthorized() throws Exception {
        when(ltijwtService.validateState(STATE)).thenThrow(new SignatureException("bad signature"));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }
}
