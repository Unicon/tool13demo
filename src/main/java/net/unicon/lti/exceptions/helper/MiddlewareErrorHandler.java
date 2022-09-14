package net.unicon.lti.exceptions.helper;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class MiddlewareErrorHandler extends DefaultResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) {
        return false; // Errors thrown during REST calls (e.g. 4xx or 5xx statuses) will be handled by the middleware, not by Spring
    }
}
