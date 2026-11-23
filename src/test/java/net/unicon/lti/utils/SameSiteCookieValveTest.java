package net.unicon.lti.utils;

import jakarta.servlet.http.Cookie;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SameSiteCookieValveTest {
    private static final String SESSION_COOKIE = "JSESSIONID=abc123; Path=/; Secure; HttpOnly; SameSite=None";

    @Mock
    Request request;

    @Mock
    Response response;

    @Mock
    Valve next;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(request.getContext()).thenReturn(null);
        when(response.getHeaders("Set-Cookie")).thenReturn(List.of());
    }

    private SameSiteCookieValve valve(SameSiteCookieValve valve) {
        valve.setNext(next);
        return valve;
    }

    @Test
    public void testLegacyCookieRestoresSessionId() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("other", "x"),
                new Cookie("JSESSIONID-legacy", "legacy-id"),
                new Cookie("JSESSIONID-legacy", "second-id")});

        valve(new SameSiteCookieValve()).invoke(request, response);

        verify(request).setRequestedSessionId("legacy-id");
        verify(request, never()).setRequestedSessionId("second-id");
        verify(request).setRequestedSessionCookie(true);
        verify(request).setRequestedSessionURL(false);
        verify(next).invoke(request, response);
    }

    @Test
    public void testCustomSuffix() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID-legacy", "ignored"), new Cookie("JSESSIONID-old", "old-id")});
        when(response.getHeaders("Set-Cookie")).thenReturn(List.of(SESSION_COOKIE));

        valve(new SameSiteCookieValve("-old")).invoke(request, response);

        verify(request).setRequestedSessionId("old-id");
        verify(response).addHeader("Set-Cookie", "JSESSIONID-old=abc123; Path=/; Secure; HttpOnly");
    }

    @Test
    public void testNoMatchingCookie() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("other", "x")});

        valve(new SameSiteCookieValve()).invoke(request, response);

        verify(request, never()).setRequestedSessionId(anyString());
        verify(request, never()).setRequestedSessionCookie(anyBoolean());
        verify(next).invoke(request, response);
    }

    @Test
    public void testNullCookies() throws Exception {
        when(request.getCookies()).thenReturn(null);

        valve(new SameSiteCookieValve()).invoke(request, response);

        verify(request, never()).setRequestedSessionId(anyString());
        verify(next).invoke(request, response);
    }

    @Test
    public void testSessionIdFromUrlSkipsLegacyLookup() throws Exception {
        when(request.isRequestedSessionIdFromURL()).thenReturn(true);

        valve(new SameSiteCookieValve()).invoke(request, response);

        verify(request, never()).getCookies();
        verify(next).invoke(request, response);
    }

    @Test
    public void testSessionIdFromCookieSkipsLegacyLookup() throws Exception {
        when(request.isRequestedSessionIdFromCookie()).thenReturn(true);

        valve(new SameSiteCookieValve()).invoke(request, response);

        verify(request, never()).getCookies();
        verify(next).invoke(request, response);
    }

    @Test
    public void testAddsLegacySessionCookieOnlyForSameSiteSessionCookie() throws Exception {
        when(request.getCookies()).thenReturn(null);
        when(response.getHeaders("Set-Cookie")).thenReturn(List.of(
                SESSION_COOKIE,
                "JSESSIONID=abc123; Path=/; HttpOnly",
                "other=value; Path=/; SameSite=None"));

        valve(new SameSiteCookieValve()).invoke(request, response);

        verify(response).addHeader("Set-Cookie", "JSESSIONID-legacy=abc123; Path=/; Secure; HttpOnly");
        verify(response, never()).addHeader("Set-Cookie", "JSESSIONID-legacy=abc123; Path=/; HttpOnly");
        verify(response, never()).addHeader("Set-Cookie", "other-legacy=value; Path=/");
        verify(response, never()).addHeader("Set-Cookie", "other=value; Path=/");
    }
}
