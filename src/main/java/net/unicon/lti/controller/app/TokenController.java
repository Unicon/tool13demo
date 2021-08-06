/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.unicon.lti.controller.app;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.exceptions.BadTokenException;
import net.unicon.lti.service.app.APIJWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Controller
@RequestMapping(value = "/api/oauth", produces = MediaType.APPLICATION_JSON_VALUE)
public class TokenController {

    @Autowired
    APIJWTService apijwtService;

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
                        Long.parseLong(claims.getBody().get("platformDeploymentId").toString()),
                        claims.getBody().get("userId").toString(),
                        claims.getBody().get("canvasUserId").toString(),
                        claims.getBody().get("canvasUserGlobalId").toString(),
                        claims.getBody().get("canvasLoginId").toString(),
                        claims.getBody().get("canvasUserName").toString(),
                        claims.getBody().get("canvasCourseId").toString(),
                        claims.getBody().get("canvasAssignmentId").toString(),
                        claims.getBody().get("dueAt").toString(),
                        claims.getBody().get("lockAt").toString(),
                        claims.getBody().get("unlockAt").toString(),
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
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadTokenException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Error generating token", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
