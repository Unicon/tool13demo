package net.unicon.lti.controller.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.service.app.APIJWTService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping(value = "/api/oauth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TokenController {

    private final APIJWTService apijwtService;

    @SuppressWarnings("rawtypes")
    @RequestMapping(method = RequestMethod.POST, value = "/trade")
    public ResponseEntity getTimedToken(HttpServletRequest req) {

        //TODO, surely we want to do something more complicated here. Like read the previous token values and make the token similar
        String token = apijwtService.extractJwtStringValue(req, true);
        Jws<Claims> claims = apijwtService.validateToken(token);
        if ((Boolean)claims.getBody().get("oneUse")) {
            try {
                return new ResponseEntity<>(apijwtService.buildJwt(false,
                        (List<String>)claims.getBody().get("roles"),
                        Long.parseLong(claims.getBody().get("contextId").toString()),
                        claims.getBody().get("contextKey").toString(),
                        Long.parseLong(claims.getBody().get("platformDeploymentId").toString()),
                        claims.getBody().get("userId").toString(),
                        claims.getBody().get("nonce").toString())
                        , HttpStatus.OK);
            } catch (GeneralSecurityException | IOException e) {
                return new ResponseEntity<>("Error generating token: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Token passed was not a one time valid token", HttpStatus.UNAUTHORIZED);
        }
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping(method = RequestMethod.POST, value = "/refresh")
    public ResponseEntity refreshToken(HttpServletRequest req) {

        String token = apijwtService.extractJwtStringValue(req, true);
        try {
            return new ResponseEntity<>(apijwtService.refreshToken(token), HttpStatus.OK);
        } catch (GeneralSecurityException | IOException e) {
            log.error("Can't refresh the API token", e);
        } catch (BadTokenException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Error generating token", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
