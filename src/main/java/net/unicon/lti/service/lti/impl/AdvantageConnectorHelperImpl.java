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

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import static org.springframework.http.MediaType.TEXT_HTML;

@Slf4j
@Service
public class AdvantageConnectorHelperImpl implements AdvantageConnectorHelper {
    @Autowired
    LTIJWTService ltijwtService;

    RestTemplate restTemplate;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    @Override
    public HttpEntity createRequestEntity(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, TextConstants.BEARER + apiKey);
        return new HttpEntity<>(headers);
    }

    // We put the token in the Authorization as a simple Bearer one.
    @Override
    public HttpEntity createTokenizedRequestEntity(LTIToken LTIToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, TextConstants.BEARER + LTIToken.getAccess_token());
        return new HttpEntity<>(headers);
    }

    // We put the token in the Authorization as a simple Bearer one.
    @Override
    public HttpEntity<LineItem> createTokenizedRequestEntity(LTIToken LTIToken, LineItem lineItem, String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, TextConstants.BEARER + LTIToken.getAccess_token());
        if (type!=null) {
            headers.add(HttpHeaders.CONTENT_TYPE, type);
        }
        return new HttpEntity<>(lineItem, headers);
    }

    // We put the token in the Authorization as a simple Bearer one.
    @Override
    public HttpEntity<LineItems> createTokenizedRequestEntity(LTIToken LTIToken, LineItems lineItems) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, TextConstants.BEARER + LTIToken.getAccess_token());
        return new HttpEntity<>(lineItems, headers);
    }

    // We put the token in the Authorization as a simple Bearer one.
    @Override
    public HttpEntity<Score> createTokenizedRequestEntity(LTIToken LTIToken, Score score) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, TextConstants.BEARER + LTIToken.getAccess_token());
        headers.set(HttpHeaders.CONTENT_TYPE, "application/vnd.ims.lis.v1.score+json");
        return new HttpEntity<>(score, headers);
    }

    //Asking for a token. The scope will come in the scope parameter
    //The platformDeployment has the URL to ask for the token.
    @Override
    public LTIToken getToken(PlatformDeployment platformDeployment, String scope) throws ConnectionException {
        LTIToken ltiToken = null;
        ResponseEntity<LTIToken> reportPostResponse = null;

        try {
            // We need a specific request for the token.
            HttpEntity request = createTokenRequest(scope, platformDeployment);
            final String POST_TOKEN_URL = platformDeployment.getoAuth2TokenUrl();
            log.debug("POST_TOKEN_URL -  " + POST_TOKEN_URL);
            reportPostResponse = postEntity(POST_TOKEN_URL, request, platformDeployment, scope);
        } catch (Exception e) {
            log.error("ERROR GETTING THE TOKEN", e);
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the token. Exception");
            log.error(exceptionMsg.toString());
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }

        if (reportPostResponse != null) {
            HttpStatus status = reportPostResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                ltiToken = reportPostResponse.getBody();
            } else {
                String exceptionMsg = "Can't get the token: " + status.getReasonPhrase();
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } else {
            String exceptionMsg = "Problem getting the token";
            log.error(exceptionMsg);
            throw new ConnectionException(exceptionMsg);
        }
        return ltiToken;
    }

    private ResponseEntity<LTIToken> postEntity(String POST_TOKEN_URL, HttpEntity request, PlatformDeployment platformDeployment, String scope) throws GeneralSecurityException, IOException {
        ResponseEntity<LTIToken> reportPostResponse;
        restTemplate = restTemplate == null ? createRestTemplate() : restTemplate;
        try {
            // Add response converter that supports Moodle's response type of text/html
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setSupportedMediaTypes(Arrays.asList(TEXT_HTML));
            restTemplate.getMessageConverters().add(converter);

            reportPostResponse = restTemplate.postForEntity(POST_TOKEN_URL, request, LTIToken.class);
        } catch (Exception ex) {
            log.error("ERROR GETTING THE TOKEN", ex);
            log.error("Can't get the token. Exception. We will try again with a JSON Payload");
            HttpEntity request2 = createTokenRequestJSON(scope, platformDeployment);
            reportPostResponse = restTemplate.
                    postForEntity(POST_TOKEN_URL, request2, LTIToken.class);
        }
        return reportPostResponse;
    }

    // This is specific to request a token.
    private HttpEntity createTokenRequest(String scope, PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        // This is standard too
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        //This is special (see the generateTokenRequestJWT method for more comments)
        map.add("client_assertion", ltijwtService.generateTokenRequestJWT(platformDeployment));
        //We need to pass the scope of the token, meaning, the service we want to allow with this token.
        map.add("scope", scope);
        return new HttpEntity<>(map, headers);
    }

    // This is specific to request a token.
    private HttpEntity createTokenRequestJSON(String scope, PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject parameterJson = new JSONObject();
        // The grant type is client credentials always
        parameterJson.put("grant_type", "client_credentials");
        // This is standard too
        parameterJson.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        //This is special (see the generateTokenRequestJWT method for more comments)
        parameterJson.put("client_assertion", ltijwtService.generateTokenRequestJWT(platformDeployment));
        //We need to pass the scope of the token, meaning, the service we want to allow with this token.
        parameterJson.put("scope", scope);
        return new HttpEntity<>(parameterJson.toString(), headers);
    }

    @Override
    public RestTemplate createRestTemplate() {
        return new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }

    @Override
    public String nextPage(HttpHeaders headers) {
        List<String> links = headers.get("link");
        if (CollectionUtils.isNotEmpty(links)) {
            String link = links.get(0);
            String[] tokens = StringUtils.split(link, ",");
            String url = indexOf(tokens);
            if (StringUtils.isNotEmpty(url)) {
                try {
                    return URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.error("Error decoding the url for the next page", e);
                }
            }
        }
        return null;
    }

    private String indexOf(String[] tokens) {
        for (String token : tokens) {
            if (StringUtils.contains(token, "rel=\"next\"")) {
                return StringUtils.substring(token, token.indexOf("<") + 1, token.indexOf(">"));
            }
        }
        return null;
    }
}