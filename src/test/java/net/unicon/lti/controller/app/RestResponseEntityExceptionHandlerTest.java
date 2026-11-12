package net.unicon.lti.controller.app;

import io.jsonwebtoken.ExpiredJwtException;
import net.unicon.lti.exceptions.DataServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestResponseEntityExceptionHandlerTest {

    private final RestResponseEntityExceptionHandler handler = new RestResponseEntityExceptionHandler();

    private final ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());

    @Test
    public void testDataServiceExceptionIsAConflict() {
        ResponseEntity<Object> response = handler.handleDataServiceException(new DataServiceException("db problem"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("db problem", response.getBody());
    }

    @Test
    public void testExpiredJwtExceptionIsUnauthorized() {
        ResponseEntity<Object> response = handler.handleExpiredJwtException(new ExpiredJwtException(null, null, "token expired"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("token expired", response.getBody());
    }
}
