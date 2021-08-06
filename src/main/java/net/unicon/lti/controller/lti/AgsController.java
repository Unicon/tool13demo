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


import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    AdvantageAGSService advantageAGSServiceService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String agsGetLineItems(HttpServletRequest req, Principal principal, Model model) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY) != null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
            Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY);
            String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to get the users on the context
                // 1. Get the token
                LTIToken LTIToken = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info(TextConstants.TOKEN + LTIToken.getAccess_token());
                // 2. Call the service
                LineItems lineItemsResult = advantageAGSServiceService.getLineItems(LTIToken, context);

                // 3. update the model
                model.addAttribute(TextConstants.SINGLE, false);
                model.addAttribute(TextConstants.RESULTS, lineItemsResult.getLineItemList());
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // Create a new lineitem
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String agsPostLineItem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItems lineItems) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY) != null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
            Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY);
            String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                LTIToken LTIToken = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                // 2. Call the service
                LineItems lineItemsResult = advantageAGSServiceService.postLineItems(LTIToken, context, lineItems);

                // 3. update the model
                model.addAttribute(TextConstants.SINGLE, false);
                model.addAttribute(TextConstants.RESULTS, lineItemsResult.getLineItemList());
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
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY) != null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
            Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY);
            String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                LTIToken LTIToken = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                // 2. Call the service
                LineItem lineItemsResult = advantageAGSServiceService.getLineItem(LTIToken, context, id);

                // 3. update the model
                model.addAttribute(TextConstants.SINGLE, true);
                model.addAttribute(TextConstants.RESULTS, Collections.singletonList(lineItemsResult));
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // Put specific lineitem

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public String agsPutLineitem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItem lineItem, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY) != null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
            Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY);
            String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                LTIToken LTIToken = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                // 2. Call the service
                lineItem.setId(id);
                LineItem lineItemsResult = advantageAGSServiceService.putLineItem(LTIToken, context, lineItem);

                // 3. update the model
                model.addAttribute(TextConstants.SINGLE, true);
                model.addAttribute(TextConstants.RESULTS, Collections.singletonList(lineItemsResult));
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
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY) != null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
            Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_DEPLOYMENT_KEY);
            String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                LTIToken LTIToken = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info(TextConstants.TOKEN + LTIToken.getAccess_token());

                // 2. Call the service
                Boolean deleteResult = advantageAGSServiceService.deleteLineItem(LTIToken, context, id);
                LineItems lineItemsResult = advantageAGSServiceService.getLineItems(LTIToken, context);

                // 3. update the model
                model.addAttribute(TextConstants.SINGLE, false);
                model.addAttribute(TextConstants.RESULTS, lineItemsResult.getLineItemList());
                model.addAttribute("deleteResults", deleteResult);
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return LTIADVAGSMAIN;
    }


    // View scores

    //Send scores

}
