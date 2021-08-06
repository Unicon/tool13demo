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
package net.unicon.lti.service.lti.impl;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Result;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This manages all the Membership call for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class AdvantageAGSServiceImpl implements AdvantageAGSService {

    @Autowired
    AdvantageConnectorHelper advantageConnectorHelper;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    static final Logger log = LoggerFactory.getLogger(AdvantageAGSServiceImpl.class);

    //Asking for a token with the right scope.
    @Override
    public LTIToken getToken(String type, PlatformDeployment platformDeployment) throws ConnectionException {

        String scope = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem";
        if (type.equals("results")) {
            scope = "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly";
        }
        if (type.equals("scores")) {
            scope = "https://purl.imsglobal.org/spec/lti-ags/scope/score";
        }
        return advantageConnectorHelper.getToken(platformDeployment, scope);
    }

    //Calling the AGS service and getting a paginated result of lineitems.
    @Override
    public LineItems getLineItems(LTIToken LTIToken, LtiContextEntity context) throws ConnectionException {
        LineItems lineItems = new LineItems();
        log.debug(TextConstants.TOKEN + LTIToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIToken);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_LINEITEMS = context.getLineitems();
            log.debug("GET_LINEITEMS -  " + GET_LINEITEMS);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.
                    exchange(GET_LINEITEMS, HttpMethod.GET, request, LineItem[].class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(lineItemsGetResponse.getBody())));
                //We deal here with pagination
                log.debug("We have {} lineItems", lineItems.getLineItemList().size());
                String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    ResponseEntity<LineItem[]> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                            request, LineItem[].class);
                    LineItem[] nextLineItemsList = responseForNextPage.getBody();
                    //List<LineItem> nextLineItems = nextLineItemsList.getLineItemList();
                    log.debug("We have {} lineitems in the next page", nextLineItemsList.length);
                    lineItemsList.addAll(Arrays.asList(nextLineItemsList));
                    nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                }
                lineItems.getLineItemList().addAll(lineItemsList);
            } else {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return lineItems;
    }

    @Override
    public boolean deleteLineItem(LTIToken LTIToken, LtiContextEntity context, String id) throws ConnectionException {
        log.debug(TextConstants.TOKEN + LTIToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIToken);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String DELETE_LINEITEM = context.getLineitems() + "/" + id;
            log.debug("DELETE_LINEITEM -  " + DELETE_LINEITEM);
            ResponseEntity<String> lineItemsGetResponse = restTemplate.
                    exchange(DELETE_LINEITEM, HttpMethod.DELETE, request, String.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                return true;
            } else {
                String exceptionMsg = "Can't delete the lineitem with id: " + id;
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't delete the lineitem with id").append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem putLineItem(LTIToken LTIToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        log.debug(TextConstants.TOKEN + LTIToken.getAccess_token());
        LineItem resultlineItem;
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<LineItem> request = advantageConnectorHelper.createTokenizedRequestEntity(LTIToken, lineItem);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String PUT_LINEITEM = context.getLineitems() + "/" + lineItem.getId();
            log.debug("PUT_LINEITEM -  " + PUT_LINEITEM);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.
                    exchange(PUT_LINEITEM, HttpMethod.PUT, request, LineItem.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                resultlineItem = lineItemsGetResponse.getBody();
                //We deal here with pagination
            } else {
                String exceptionMsg = "Can't put the lineitem " + lineItem.getId();
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get put lineitem ").append(lineItem.getId());
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return resultlineItem;
    }

    @Override
    public LineItem getLineItem(LTIToken LTIToken, LtiContextEntity context, String id) throws ConnectionException {
        LineItem lineItem;
        log.debug(TextConstants.TOKEN + LTIToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIToken);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_LINEITEM = context.getLineitems() + "/" + id;
            log.debug("GET_LINEITEMS -  " + GET_LINEITEM);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.
                    exchange(GET_LINEITEM, HttpMethod.GET, request, LineItem.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                lineItem = lineItemsGetResponse.getBody();
                //We deal here with pagination
            } else {
                String exceptionMsg = "Can't get the lineitem " + id;
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the lineitem ").append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return lineItem;
    }

    @Override
    public LineItems postLineItems(LTIToken LTIToken, LtiContextEntity context, LineItems lineItems) throws ConnectionException {

        LineItems resultLineItems = new LineItems();
        log.debug(TextConstants.TOKEN + LTIToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<LineItems> request = advantageConnectorHelper.createTokenizedRequestEntity(LTIToken, lineItems);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String POST_LINEITEMS = context.getLineitems();
            log.debug("POST_LINEITEMS -  " + POST_LINEITEMS);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.
                    exchange(POST_LINEITEMS, HttpMethod.POST, request, LineItem[].class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(lineItemsGetResponse.getBody())));
                //We deal here with pagination
                log.debug("We have {} lineItems", lineItems.getLineItemList().size());
                String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    ResponseEntity<LineItems> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                            request, LineItems.class);
                    LineItems nextLineItemsList = responseForNextPage.getBody();
                    List<LineItem> nextLineItems = Objects.requireNonNull(nextLineItemsList).getLineItemList();
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
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post lineitems");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return resultLineItems;
    }

    @Override
    public Results getResults(LTIToken LTITokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException {
        Results results = new Results();
        log.debug(TextConstants.TOKEN + LTITokenResults.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTITokenResults);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_RESULTS = lineItemId + "/results";
            log.debug("GET_RESULTS -  " + GET_RESULTS  + "/" + lineItemId + "/results");
            ResponseEntity<Result[]> resultsGetResponse = restTemplate.
                    exchange(GET_RESULTS, HttpMethod.GET, request, Result[].class);
            HttpStatus status = resultsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                List<Result> resultList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(resultsGetResponse.getBody())));
                //We deal here with pagination
                log.debug("We have {} results", results.getResultList().size());
                String nextPage = advantageConnectorHelper.nextPage(resultsGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    ResponseEntity<Results> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                            request, Results.class);
                    Results nextResultsList = responseForNextPage.getBody();
                    List<Result> nextResults = nextResultsList
                            .getResultList();
                    log.debug("We have {} results in the next page", nextResultsList.getResultList().size());
                    resultList.addAll(nextResults);
                    nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                }
                results.getResultList().addAll(resultList);
            } else {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return results;
    }

    @Override
    public Results postScore(LTIToken lTITokenScores, LTIToken lTITokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException {
        log.debug(TextConstants.TOKEN + lTITokenScores.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<Score> request = advantageConnectorHelper.createTokenizedRequestEntity(lTITokenScores, score);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String POST_SCORES = lineItemId + "/scores";
            log.debug("POST_SCORES -  " + POST_SCORES);
            ResponseEntity<Void> scoreGetResponse = restTemplate.exchange(POST_SCORES, HttpMethod.POST, request, Void.class);
            HttpStatus status = scoreGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                return getResults(lTITokenResults, context, lineItemId);
            } else {
                String exceptionMsg = "Can't post scores";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post scores");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    //POST SCORES

}
