package net.unicon.lti.security.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

public class APIOAuthProviderProcessingFilterTest {

    @Mock
    private APIJWTService apiJwtService;

    @Mock
    private APIDataService apiDataService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jws<Claims> jws;

    @Mock
    private Claims claims;

    private APIOAuthProviderProcessingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(jws.getBody()).thenReturn(claims);
        when(claims.getIssuer()).thenReturn("ISSUER");
        when(claims.get("oneUse")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pwd"));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testConstructorRejectsNullServices() {
        assertThrows(AssertionError.class, () -> new APIOAuthProviderProcessingFilter(null, apiDataService));
        assertThrows(AssertionError.class, () -> new APIOAuthProviderProcessingFilter(apiJwtService, null, true));
    }

    @Test
    public void testNonHttpRequestIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(mock(ServletRequest.class), mock(ServletResponse.class), filterChain));
    }

    @Test
    public void testMissingHeaderIsUnauthorized() throws Exception {
        request.setParameter("token", "query-token");

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
        verify(apiJwtService, never()).validateToken(any());
    }

    @Test
    public void testNonBearerHeaderIsUnauthorized() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testValidBearerTokenPassesAndResetsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(apiJwtService.validateToken("the-token")).thenReturn(jws);

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        verify(apiDataService, never()).findAndDeleteOneUseToken(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testQueryParamTokenIsUsedWhenAllowed() throws Exception {
        filter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService, true);
        request.setParameter("token", "query-token");
        when(apiJwtService.validateToken("query-token")).thenReturn(jws);

        filter.doFilter(request, response, filterChain);

        verify(apiJwtService).validateToken("query-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testMissingQueryParamIsUnauthorizedWhenAllowed() throws Exception {
        filter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService, true);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testEmptyBearerTokenSkipsValidation() throws Exception {
        request.addHeader("Authorization", "Bearer");

        filter.doFilter(request, response, filterChain);

        verify(apiJwtService, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testNullClaimsStillPasses() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(apiJwtService.validateToken("the-token")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testWrongIssuerIsRejected() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(apiJwtService.validateToken("the-token")).thenReturn(jws);
        when(claims.getIssuer()).thenReturn("someone-else");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, filterChain));

        assertEquals("API token is invalid", e.getMessage());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testOneUseTokenIsConsumed() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(apiJwtService.validateToken("the-token")).thenReturn(jws);
        when(claims.get("oneUse")).thenReturn(true);
        when(apiDataService.findAndDeleteOneUseToken("the-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(apiDataService).findAndDeleteOneUseToken("the-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testUsedOneUseTokenIsRejected() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(apiJwtService.validateToken("the-token")).thenReturn(jws);
        when(claims.get("oneUse")).thenReturn(true);
        when(apiDataService.findAndDeleteOneUseToken("the-token")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, filterChain));

        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testExpiredTokenIsUnauthorized() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(claims.getSubject()).thenReturn("user-1");
        when(apiJwtService.validateToken("the-token")).thenThrow(new ExpiredJwtException(null, claims, "expired"));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    public void testBadSignatureIsUnauthorized() throws Exception {
        request.addHeader("Authorization", "Bearer the-token");
        when(apiJwtService.validateToken("the-token")).thenThrow(new SignatureException("bad signature"));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }
}
