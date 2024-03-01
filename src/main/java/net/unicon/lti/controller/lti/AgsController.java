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
package net.unicon.lti.controller.lti;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@SuppressWarnings("SameReturnValue")
@Controller
@Scope("session")
@RequestMapping("/ags")
public class AgsController {

    static final Logger log = LoggerFactory.getLogger(AgsController.class);
    static final String LTIADVAGSMAIN = "ltiAdvAgsMain";
    static final String LTIADVAGSDETAIL = "ltiAdvAgsDetail";

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    AdvantageAGSService advantageAGSServiceServiceImpl;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String agsGetLineItems(HttpServletRequest req, Principal principal, Model model) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        if (req.getParameter(TextConstants.ADVANTAGE_TOKEN) != null) {
            try {
                Jws<Claims> claims = apijwtService.validateToken(req.getParameter(TextConstants.ADVANTAGE_TOKEN).toString());

                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, false);
                Long deployment = Long.parseLong(claims.getPayload().get("platformDeploymentId").toString());
                String contextKey = claims.getPayload().get("contextKey").toString();
                //We find the right deployment:
                Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
                if (platformDeployment.isPresent()) {
                    //Get the context in the query
                    LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextKey, platformDeployment.get());

                    //Call the ags service to get the users on the context
                    // 1. Get the token
                    LTIToken LTIToken = advantageAGSServiceServiceImpl.getToken("lineitems", platformDeployment.get());
                    LTIToken resultsToken = advantageAGSServiceServiceImpl.getToken("results", platformDeployment.get());
                    log.info(TextConstants.TOKEN + LTIToken.getAccess_token());
                    // 2. Call the service
                    LineItems lineItemsResult = advantageAGSServiceServiceImpl.getLineItems(LTIToken, context, true, resultsToken);

                    // 3. update the model
                    model.addAttribute(TextConstants.LINEITEMS, lineItemsResult.getLineItemList());
                    model.addAttribute(TextConstants.ADVANTAGE_TOKEN, req.getParameter(TextConstants.ADVANTAGE_TOKEN));
                }
            } catch (Exception ex){
                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, true);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // Create a new lineitem
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public String agsPostLineItem(HttpServletRequest req, Principal principal, Model model, LineItem lineItem) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        if (req.getParameter(TextConstants.ADVANTAGE_TOKEN) != null) {
            try {
                Jws<Claims> claims = apijwtService.validateToken(req.getParameter(TextConstants.ADVANTAGE_TOKEN).toString());

                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, false);
                Long deployment = Long.parseLong(claims.getPayload().get("platformDeploymentId").toString());
                String contextKey = claims.getPayload().get("contextKey").toString();
                //We find the right deployment:
                Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
                if (platformDeployment.isPresent()) {
                    //Get the context in the query
                    LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextKey, platformDeployment.get());

                    //Call the ags service to post a lineitem
                    // 1. Get the token
                    log.debug("RETRIEVING TOKEN TO CREATE LINEITEM:");
                    LTIToken LTIToken = advantageAGSServiceServiceImpl.getToken("lineitems", platformDeployment.get());
                    LTIToken resultsToken = advantageAGSServiceServiceImpl.getToken("results", platformDeployment.get());
                    log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                    // 2. Call the service
                    log.debug("POST TO CREATE LINEITEM:");
                    advantageAGSServiceServiceImpl.cleanLineItem(lineItem);
                    LineItems lineItemsResult = advantageAGSServiceServiceImpl.postLineItem(LTIToken, context, lineItem);
                    log.debug("GET ALL LINEITEMS:");
                    LineItems lineItemsResults = advantageAGSServiceServiceImpl.getLineItems(LTIToken, context, true, resultsToken);

                    // 3. update the model
                    model.addAttribute(TextConstants.RESULTS, lineItemsResult.getLineItemList());
                    model.addAttribute(TextConstants.LINEITEMS, lineItemsResults.getLineItemList());
                    model.addAttribute(TextConstants.ADVANTAGE_TOKEN, req.getParameter(TextConstants.ADVANTAGE_TOKEN));

                }
            } catch (Exception ex){
                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, true);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // Get specific lineitem

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String agsGetLineitem(HttpServletRequest req, Principal principal, Model model, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        if (req.getParameter(TextConstants.ADVANTAGE_TOKEN) != null) {
            try {
                Jws<Claims> claims = apijwtService.validateToken(req.getParameter(TextConstants.ADVANTAGE_TOKEN).toString());

                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, false);
                Long deployment = Long.parseLong(claims.getPayload().get("platformDeploymentId").toString());
                String contextKey = claims.getPayload().get("contextKey").toString();
                //We find the right deployment:
                Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
                if (platformDeployment.isPresent()) {
                    //Get the context in the query
                    LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextKey, platformDeployment.get());

                    //Call the ags service to post a lineitem
                    // 1. Get the token
                    log.debug("RETREIVING TOKEN TO GET SINGLE LINEITEM:");
                    LTIToken LTIToken = advantageAGSServiceServiceImpl.getToken("lineitems", platformDeployment.get());
                    LTIToken resultsToken = advantageAGSServiceServiceImpl.getToken("results", platformDeployment.get());
                    log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                    // 2. Call the service
                    log.debug("GET SINGLE LINEITEM:");
                    LineItem lineItemsResult = advantageAGSServiceServiceImpl.getLineItem(LTIToken, resultsToken, context, id);

                    // 3. update the model
                    model.addAttribute(TextConstants.LINEITEMS, Collections.singletonList(lineItemsResult));
                    model.addAttribute(TextConstants.ADVANTAGE_TOKEN, req.getParameter(TextConstants.ADVANTAGE_TOKEN));

                }
            } catch (Exception ex){
                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, true);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSDETAIL;
    }


    // Put specific lineitem

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public String agsPutLineitem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItem lineItem, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        if (req.getParameter(TextConstants.ADVANTAGE_TOKEN) != null) {
            try {
                Jws<Claims> claims = apijwtService.validateToken(req.getParameter(TextConstants.ADVANTAGE_TOKEN).toString());

                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, false);
                Long deployment = Long.parseLong(claims.getPayload().get("platformDeploymentId").toString());
                String contextKey = claims.getPayload().get("contextKey").toString();
                //We find the right deployment:
                Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
                if (platformDeployment.isPresent()) {
                    //Get the context in the query
                    LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextKey, platformDeployment.get());

                    //Call the ags service to post a lineitem
                    // 1. Get the token
                    LTIToken LTIToken = advantageAGSServiceServiceImpl.getToken("lineitems", platformDeployment.get());
                    log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                    // 2. Call the service
                    lineItem.setId(id);
                    LineItem lineItemsResult = advantageAGSServiceServiceImpl.putLineItem(LTIToken, context, lineItem);

                    // 3. update the model
                    model.addAttribute(TextConstants.RESULTS, Collections.singletonList(lineItemsResult));
                    model.addAttribute(TextConstants.ADVANTAGE_TOKEN, req.getParameter(TextConstants.ADVANTAGE_TOKEN));

                }
            } catch (Exception ex){
                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, true);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // Delete lineitem

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public String agsPDeleteLineitem(HttpServletRequest req, Principal principal, Model model, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        if (req.getParameter(TextConstants.ADVANTAGE_TOKEN) != null) {
            try {
                Jws<Claims> claims = apijwtService.validateToken(req.getParameter(TextConstants.ADVANTAGE_TOKEN).toString());

                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, false);
                Long deployment = Long.parseLong(claims.getPayload().get("platformDeploymentId").toString());
                String contextKey = claims.getPayload().get("contextKey").toString();
                //We find the right deployment:
                Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
                if (platformDeployment.isPresent()) {
                    //Get the context in the query
                    LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextKey, platformDeployment.get());

                    //Call the ags service to post a lineitem
                    // 1. Get the token
                    LTIToken LTIToken = advantageAGSServiceServiceImpl.getToken("lineitems", platformDeployment.get());
                    log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                    // 2. Call the service
                    Boolean deleteResult = advantageAGSServiceServiceImpl.deleteLineItem(LTIToken, context, id);
                    LineItems lineItemsResult = advantageAGSServiceServiceImpl.getLineItems(LTIToken, context);

                    // 3. update the model
                    model.addAttribute(TextConstants.LINEITEMS, lineItemsResult.getLineItemList());
                    model.addAttribute(TextConstants.ADVANTAGE_TOKEN, req.getParameter(TextConstants.ADVANTAGE_TOKEN));
                    model.addAttribute("deleteResults", deleteResult);
                }
            } catch (Exception ex){
                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, true);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // Post Score
    @RequestMapping(value = "/score/{id}", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public String agsPostScore(HttpServletRequest req, Principal principal, Model model, Score score, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        if (req.getParameter(TextConstants.ADVANTAGE_TOKEN) != null) {
            try {
                Jws<Claims> claims = apijwtService.validateToken(req.getParameter(TextConstants.ADVANTAGE_TOKEN).toString());

                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, false);
                Long deployment = Long.parseLong(claims.getPayload().get("platformDeploymentId").toString());
                String contextKey = claims.getPayload().get("contextKey").toString();
                //We find the right deployment:
                Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
                if (platformDeployment.isPresent()) {
                    //Get the context in the query
                    LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextKey, platformDeployment.get());

                    //Call the ags service to post a lineitem
                    // 1. Get the token
                    log.debug("RETRIEVING TOKEN FOR FETCHING ALL LINEITEMS:");
                    LTIToken LTIToken = advantageAGSServiceServiceImpl.getToken("lineitems", platformDeployment.get());
                    log.info(TextConstants.TOKEN + LTIToken.getAccess_token());
                    log.debug("RETRIEVING TOKEN FOR FETCHING ALL RESULTS:");
                    LTIToken resultsToken = advantageAGSServiceServiceImpl.getToken("results", platformDeployment.get());
                    log.debug("RETRIEVING TOKEN FOR POSTING SCORE:");
                    LTIToken scoresToken = advantageAGSServiceServiceImpl.getToken("scores", platformDeployment.get());
                    log.debug("Scores Bearer Token: {}", scoresToken.getAccess_token());


                    // 2. Call the service
                    log.debug("CALLING GET SINGLE LINEITEM:");
                    LineItem lineItemsResult = advantageAGSServiceServiceImpl.getLineItem(LTIToken, resultsToken, context, id);
                    log.debug("CALLING POST SCORE:");
                    Results scoreResults = advantageAGSServiceServiceImpl.postScore(scoresToken, resultsToken, context, lineItemsResult.getId(), score);
                    log.debug("CALLING GET SINGLE LINEITEM:");
                    LineItem lineItemsResultNew = advantageAGSServiceServiceImpl.getLineItem(LTIToken, resultsToken, context, id);

                    // 3. update the model
                    model.addAttribute(TextConstants.LINEITEMS, Collections.singletonList(lineItemsResultNew));
                    model.addAttribute(TextConstants.ADVANTAGE_TOKEN, req.getParameter(TextConstants.ADVANTAGE_TOKEN));

                }
            } catch (Exception ex){
                model.addAttribute(TextConstants.NO_ADVANTAGE_TOKEN, true);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSDETAIL;
    }

}
