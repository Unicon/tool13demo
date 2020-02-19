/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Serving the public key of the tool
 */
@Controller
@Scope("session")
@RequestMapping("/jwks")
public class JWKController {

    static final Logger log = LoggerFactory.getLogger(JWKController.class);

    @RequestMapping(value = "/jwk",method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public String jkw(HttpServletRequest req,  Model model) {
        log.info("Someone is calling the jwk endpoint!");
        log.info(req.getQueryString());
        return "{\n" +
                "\"keys\": [{\n" +
                "    \"kty\": \"RSA\",\n" +
                "    \"n\": \"1emJEYJebrnPAvrAf6FDCQAOldKF3W-LY8i91L3NvUPgrkKsPjjRO-g0B-sRqKsoWVaN8wZ2j0y-e2YX5-ig1k2bMmNHMgRGISf1rvgMEJA1k9RiGxWuMeWrP9Aa_nYEs7Wau5dCB0SelGCPHEjrHmHmIzfZGsJG_i1AZ7EKOER90cxQG3pG8tnQqWNordtxJ7Cqr2_jSAFb5zW--AV9D6xjlSTuk1V3uJbtEH4q2Zid8fA8aAwaNPvL7QbW5IhrZw_chGxD_z3wHb1VQFiyycVjI6LTTmzI4IB9Dkt6QS3jzxft-AkTsJ4250xbCYr2lWsbd1n1-E3uzjipOS5EGQ\",\n" +
                "    \"e\": \"AQAB\",\n" +
                "\"kid\": \"000000000000000001\"," +
                "\"alg\": \"RS256\",\n" +
                "\"use\": \"sig\"}]}";


    }

}
