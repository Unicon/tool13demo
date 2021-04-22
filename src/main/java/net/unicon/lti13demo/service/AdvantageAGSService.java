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
package net.unicon.lti13demo.service;

import net.unicon.lti13demo.exceptions.ConnectionException;
import net.unicon.lti13demo.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti13demo.model.LtiContextEntity;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.ags.LineItem;
import net.unicon.lti13demo.model.ags.LineItems;
import net.unicon.lti13demo.model.oauth2.Token;
import net.unicon.lti13demo.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This manages all the Membership call for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Component
public class AdvantageAGSService {

    @Autowired
    AdvantageConnectorHelper advantageConnectorHelper;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    static final Logger log = LoggerFactory.getLogger(AdvantageAGSService.class);

    //Asking for a token with the right scope.
    public Token getToken(PlatformDeployment platformDeployment) throws ConnectionException {
        String scope = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem";
        return advantageConnectorHelper.getToken(platformDeployment, scope);
    }

    //Calling the AGS service and getting a paginated result of lineitems.
    public LineItems getLineItems(Token token, LtiContextEntity context) throws ConnectionException {
        LineItems lineItems = new LineItems();
        log.debug(TextConstants.TOKEN + token.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(token);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_LINEITEMS = context.getLineitems();
            log.debug("GET_LINEITEMS -  "+ GET_LINEITEMS);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.
                    exchange(GET_LINEITEMS, HttpMethod.GET, request, LineItem[].class);
            List<LineItem> lineItemsList = new ArrayList<>();
            if (lineItemsGetResponse != null) {
                HttpStatus status = lineItemsGetResponse.getStatusCode();
                if (status.is2xxSuccessful()) {
                    lineItemsList.addAll(Arrays.asList(lineItemsGetResponse.getBody()));
                    //We deal here with pagination
                    log.debug("We have {} lineItems",lineItems.getLineItemList().size());
                    String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());
                    log.debug("We have next page: " + nextPage);
                    while (nextPage != null) {
                        ResponseEntity<LineItems> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                                request, LineItems.class);
                        LineItems nextLineItemsList = responseForNextPage.getBody();
                        List<LineItem> nextLineItems = nextLineItemsList
                                .getLineItemList();
                        log.debug("We have {} lineitems in the next page",nextLineItemsList.getLineItemList().size());
                        lineItemsList.addAll(nextLineItems);
                        nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                    }
                    lineItems.getLineItemList().addAll(lineItemsList);
                } else {
                    String exceptionMsg = "Can't get the AGS";
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Problem getting the AGS");
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(),e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return lineItems;
    }

    public boolean deleteLineItem(Token token, LtiContextEntity context, String id) throws ConnectionException {
        log.debug(TextConstants.TOKEN + token.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(token);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String DELETE_LINEITEM = context.getLineitems() + "/" + id;
            log.debug("DELETE_LINEITEM -  "+ DELETE_LINEITEM);
            ResponseEntity<String> lineItemsGetResponse = restTemplate.
                    exchange(DELETE_LINEITEM, HttpMethod.DELETE, request, String.class);
            if (lineItemsGetResponse != null) {
                HttpStatus status = lineItemsGetResponse.getStatusCode();
                if (status.is2xxSuccessful()) {
                    return true;
                } else {
                    String exceptionMsg = "Can't delete the lineitem with id: " + id;
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Can't delete the lineitem with id: " + id);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't delete the lineitem with id" + id);
            log.error(exceptionMsg.toString(),e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return false;
    }

    public LineItem putLineItem(Token token, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        log.debug(TextConstants.TOKEN + token.getAccess_token());
        LineItem resultlineItem = null;
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<LineItem> request = advantageConnectorHelper.createTokenizedRequestEntity(token, lineItem);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String PUT_LINEITEM = context.getLineitems() + "/" + lineItem.getId();
            log.debug("PUT_LINEITEM -  "+ PUT_LINEITEM);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.
                    exchange(PUT_LINEITEM, HttpMethod.PUT, request, LineItem.class);
            if (lineItemsGetResponse != null) {
                HttpStatus status = lineItemsGetResponse.getStatusCode();
                if (status.is2xxSuccessful()) {
                    resultlineItem = lineItemsGetResponse.getBody();
                    //We deal here with pagination
                } else {
                    String exceptionMsg = "Can't put the lineitem " + lineItem.getId();
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Problem putting the lineitem " + lineItem.getId());
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get put lineitem " + lineItem.getId());
            log.error(exceptionMsg.toString(),e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return resultlineItem;
    }

    public LineItem getLineItem(Token token, LtiContextEntity context, String id) throws ConnectionException {
        LineItem lineItem = null;
        log.debug(TextConstants.TOKEN + token.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(token);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_LINEITEM = context.getLineitems() + "/" + id;
            log.debug("GET_LINEITEMS -  "+ GET_LINEITEM);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.
                    exchange(GET_LINEITEM, HttpMethod.GET, request, LineItem.class);
            if (lineItemsGetResponse != null) {
                HttpStatus status = lineItemsGetResponse.getStatusCode();
                if (status.is2xxSuccessful()) {
                    lineItem = lineItemsGetResponse.getBody();
                    //We deal here with pagination
                } else {
                    String exceptionMsg = "Can't get the lineitem " + id;
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Problem getting the lineitem " + id);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the lineitem " + id);
            log.error(exceptionMsg.toString(),e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return lineItem;
    }

    public LineItems postLineItems(Token token, LtiContextEntity context, LineItems lineItems) throws ConnectionException {

        LineItems resultLineItems = new LineItems();
        log.debug(TextConstants.TOKEN  + token.getAccess_token());
        LineItem resultlineItem = null;
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<LineItems> request = advantageConnectorHelper.createTokenizedRequestEntity(token, lineItems);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String POST_LINEITEMS = context.getLineitems();
            log.debug("POST_LINEITEMS -  " + POST_LINEITEMS);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.
                    exchange(POST_LINEITEMS, HttpMethod.POST, request, LineItem[].class);
            List<LineItem> lineItemsList = new ArrayList<>();
            if (lineItemsGetResponse != null) {
                HttpStatus status = lineItemsGetResponse.getStatusCode();
                if (status.is2xxSuccessful()) {
                    lineItemsList.addAll(Arrays.asList(lineItemsGetResponse.getBody()));
                    //We deal here with pagination
                    log.debug("We have {} lineItems", lineItems.getLineItemList().size());
                    String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());
                    log.debug("We have next page: " + nextPage);
                    while (nextPage != null) {
                        ResponseEntity<LineItems> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                                request, LineItems.class);
                        LineItems nextLineItemsList = responseForNextPage.getBody();
                        List<LineItem> nextLineItems = nextLineItemsList
                                .getLineItemList();
                        log.debug("We have {} lineitems in the next page", nextLineItemsList.getLineItemList().size());
                        lineItemsList.addAll(nextLineItems);
                        nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                    }
                    resultLineItems.getLineItemList().addAll(lineItemsList);
                } else {
                    String exceptionMsg = "Can't post lineitems";
                    log.error(exceptionMsg);
                    throw new ConnectionException(exceptionMsg);
                }
            } else {
                log.warn("Problem posting lineitems");
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post lineitems");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return resultLineItems;
    }

    //GET RESULTS


    //POST SCORES

}
