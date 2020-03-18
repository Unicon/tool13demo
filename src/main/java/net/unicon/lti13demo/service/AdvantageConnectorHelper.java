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

import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.oauth2.Token;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class AdvantageConnectorHelper {

    @Autowired
    LTIJWTService ltijwtService;

    public HttpEntity createRequestEntity(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        HttpEntity request = new HttpEntity<>(headers);
        return request;
    }

    public HttpEntity createTokenizedRequestEntity(Token token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccess_token());
        HttpEntity request = new HttpEntity<>(headers);
        return request;
    }

    public HttpEntity createTokenRequest(String scope, PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject parameterJson = new JSONObject();
        parameterJson.put("grant_type", "client_credentials");
        parameterJson.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        parameterJson.put("client_assertion", ltijwtService.generateTokenRequestJWT(platformDeployment));
        parameterJson.put("scope", scope);
        HttpEntity request = new HttpEntity<>(parameterJson.toString(), headers);
        return request;
    }

    public RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        return restTemplate;
    }

    public String nextPage(HttpHeaders headers) {
        List<String> links = headers.get("link");
        if (CollectionUtils.isNotEmpty(links)) {
            String link = links.get(0);
            String[] tokens = StringUtils.split(link, ",");
            String url = indexOf(tokens, "rel=\"next\"");
            if (StringUtils.isNotEmpty(url)) {
                try {
                    return URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public String indexOf(String[] tokens, String searchString) {
        for (String token : tokens) {
            if (StringUtils.contains(token, searchString)) {
                return StringUtils.substring(token, token.indexOf("<") + 1, token.indexOf(">"));
            }
        }
        return null;
    }
}