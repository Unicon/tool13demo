package net.unicon.lti.utils;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * The HTTP client every outgoing call to an LMS goes through. One shared JDK HttpClient reuses
 * connections between calls, and the timeouts keep a slow or unresponsive LMS from holding a
 * request thread indefinitely.
 */
public final class HttpClients {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        // like HttpURLConnection, follow redirects, but never from https to http
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    private HttpClients() {
        throw new IllegalStateException("Utility class");
    }

    public static ClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HTTP_CLIENT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return new BufferingClientHttpRequestFactory(requestFactory);
    }

    public static RestTemplate restTemplate() {
        return new RestTemplate(requestFactory());
    }

}
