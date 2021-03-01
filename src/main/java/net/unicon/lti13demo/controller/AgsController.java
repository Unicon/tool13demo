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


import net.unicon.lti13demo.exceptions.ConnectionException;
import net.unicon.lti13demo.model.LtiContextEntity;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.ags.LineItem;
import net.unicon.lti13demo.model.ags.LineItems;
import net.unicon.lti13demo.model.oauth2.Token;
import net.unicon.lti13demo.repository.LtiContextRepository;
import net.unicon.lti13demo.repository.PlatformDeploymentRepository;
import net.unicon.lti13demo.service.AdvantageAGSService;
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
@Controller
@Scope("session")
@RequestMapping("/ags")
public class AgsController {

    static final Logger log = LoggerFactory.getLogger(AgsController.class);

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
        if ((session.getAttribute("deployment_key") !=null) && (session.getAttribute("deployment_key") !=null)){
            model.addAttribute("noSessionValues", false);
            Long deployment = (Long) session.getAttribute("deployment_key");
            String contextId = (String) session.getAttribute("context_id");
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to get the users on the context
                // 1. Get the token
                Token token = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info("TOKEN: " + token.getAccess_token());
                // 2. Call the service
                LineItems lineItemsResult = advantageAGSServiceService.getLineItems(token, context);

                // 3. update the model
                model.addAttribute("single", false);
                model.addAttribute("results", lineItemsResult.getLineItemList());
            }
        } else {
            model.addAttribute("noSessionValues", true);
        }
        return "ltiAdvAgsMain";
    }


    // Create a new lineitem
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String agsPostLineItem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItems lineItems) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if ((session.getAttribute("deployment_key") !=null) && (session.getAttribute("deployment_key") !=null)){
            model.addAttribute("noSessionValues", false);
            Long deployment = (Long) session.getAttribute("deployment_key");
            String contextId = (String) session.getAttribute("context_id");
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                Token token = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info("TOKEN: " + token.getAccess_token());

                // 2. Call the service
                LineItems lineItemsResult = advantageAGSServiceService.postLineItems(token, context, lineItems);

                // 3. update the model
                model.addAttribute("single", false);
                model.addAttribute("results", lineItemsResult.getLineItemList());
            }
        } else {
            model.addAttribute("noSessionValues", true);
        }
        return "ltiAdvAgsMain";
    }


    // Get specific lineitem

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String agsGetLineitem(HttpServletRequest req, Principal principal, Model model, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if ((session.getAttribute("deployment_key") !=null) && (session.getAttribute("deployment_key") !=null)){
            model.addAttribute("noSessionValues", false);
            Long deployment = (Long) session.getAttribute("deployment_key");
            String contextId = (String) session.getAttribute("context_id");
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                Token token = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info("TOKEN: " + token.getAccess_token());

                // 2. Call the service
                LineItem lineItemsResult = advantageAGSServiceService.getLineItem(token, context, id);

                // 3. update the model
                model.addAttribute("single", true);
                model.addAttribute("results", Collections.singletonList(lineItemsResult));
            }
        } else {
            model.addAttribute("noSessionValues", true);
        }
        return "ltiAdvAgsMain";
    }


    // Put specific lineitem

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public String agsPutLineitem(HttpServletRequest req, Principal principal, Model model, @RequestBody LineItem lineItem, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if ((session.getAttribute("deployment_key") !=null) && (session.getAttribute("deployment_key") !=null)){
            model.addAttribute("noSessionValues", false);
            Long deployment = (Long) session.getAttribute("deployment_key");
            String contextId = (String) session.getAttribute("context_id");
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                Token token = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info("TOKEN: " + token.getAccess_token());

                // 2. Call the service
                lineItem.setId(id);
                LineItem lineItemsResult = advantageAGSServiceService.putLineItem(token, context, lineItem);

                // 3. update the model
                model.addAttribute("single", true);
                model.addAttribute("results", Collections.singletonList(lineItemsResult));
            }
        } else {
            model.addAttribute("noSessionValues", true);
        }
        return "ltiAdvAgsMain";
    }


    // Delete lineitem

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public String agsPDeleteLineitem(HttpServletRequest req, Principal principal, Model model, @PathVariable("id") String id) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if ((session.getAttribute("deployment_key") !=null) && (session.getAttribute("deployment_key") !=null)){
            model.addAttribute("noSessionValues", false);
            Long deployment = (Long) session.getAttribute("deployment_key");
            String contextId = (String) session.getAttribute("context_id");
            //We find the right deployment:
            Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(deployment);
            if (platformDeployment.isPresent()) {
                //Get the context in the query
                LtiContextEntity context = ltiContextRepository.findByContextKeyAndPlatformDeployment(contextId, platformDeployment.get());

                //Call the ags service to post a lineitem
                // 1. Get the token
                Token token = advantageAGSServiceService.getToken(platformDeployment.get());
                log.info("TOKEN: " + token.getAccess_token());

                // 2. Call the service
                Boolean deleteResult = advantageAGSServiceService.deleteLineItem(token, context, id);
                LineItems lineItemsResult = advantageAGSServiceService.getLineItems(token, context);

                // 3. update the model
                model.addAttribute("single", false);
                model.addAttribute("results", lineItemsResult.getLineItemList());
                model.addAttribute("deleteResults", deleteResult);
            }
        } else {
            model.addAttribute("noSessionValues", true);
        }
        return "ltiAdvAgsMain";
    }


    // View scores

    //Send scores

}
