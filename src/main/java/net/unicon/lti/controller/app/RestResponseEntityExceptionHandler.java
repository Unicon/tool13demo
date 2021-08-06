package net.unicon.lti.controller.app;

import io.jsonwebtoken.ExpiredJwtException;
import net.unicon.lti.exceptions.DataServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    final static Logger log = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value
            = {DataServiceException.class})
    protected ResponseEntity<Object> handleDataServiceException(
            DataServiceException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value
            = { ExpiredJwtException.class})
    protected ResponseEntity<Object> handleExpiredJwtException(
            ExpiredJwtException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

}
