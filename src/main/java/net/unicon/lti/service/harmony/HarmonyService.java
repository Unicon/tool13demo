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
package net.unicon.lti.service.harmony;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import net.unicon.lti.model.harmony.HarmonyFetchDeepLinksBody;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import net.unicon.lti.utils.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This manages all the interactions with Harmony
 */
@Service
@Slf4j
public class HarmonyService {
    private static final String DEEP_LINKS_PATH = "/lti_deep_links";
    private static final String LINEITEMS_PATH = "/lineitems";

    @Value("${harmony.courses.api}")
    private String harmonyCoursesApiUrl;

    @Value("${harmony.courses.jwt}")
    private String harmonyJWT;

    public HarmonyPageResponse fetchHarmonyCourses(Integer page, String rootOutcomeGuid) {

        if (StringUtils.isAnyBlank(harmonyCoursesApiUrl, harmonyJWT)) {
            log.warn("The Harmony Courses API has not been configured, courses will not be fetched.");
            return null;
        }

        try {

            RestTemplate restTemplate = RestUtils.createRestTemplate();

            // Build the URL
            URIBuilder builder = new URIBuilder(harmonyCoursesApiUrl);
            if (StringUtils.isNotEmpty(rootOutcomeGuid)) {
                builder.addParameter("root_outcome_guid", rootOutcomeGuid);
            } else if (page != null && page > 1) {
                // Add the page parameter since this API is paginated.
                builder.setParameter("page", String.valueOf(page));
            }

            String requestUrl = builder.build().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(harmonyJWT);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<HarmonyPageResponse> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, HarmonyPageResponse.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Harmony Courses API returned {}", response.getStatusCode());
                log.error(String.valueOf(response.getBody()));
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public List<HarmonyContentItemDTO> fetchDeepLinkingContentItems(String rootOutcomeGuid, String idToken, boolean coursePaired, List moduleIds) {
        if (StringUtils.isAnyBlank(harmonyCoursesApiUrl, harmonyJWT)) {
            log.error("The Harmony API has not been configured, deep links will not be fetched.");
            return null;
        }

        if (rootOutcomeGuid == null || rootOutcomeGuid.isEmpty()) {
            log.error("Cannot fetch deep links without guid (root_outcome_guid)");
            return null;
        }

        if (idToken == null || idToken.isEmpty()) {
            log.error("Cannot fetch deep links without id_token");
            return null;
        }

        String requestUrl = "";
        try {
            RestTemplate restTemplate = RestUtils.createRestTemplate();

            URIBuilder builder = new URIBuilder(harmonyCoursesApiUrl + DEEP_LINKS_PATH);
            builder.addParameter("guid", rootOutcomeGuid);
            builder.addParameter("course_paired", String.valueOf(coursePaired));
            requestUrl = builder.build().toString();
            log.debug("About to request deep links from: {}", requestUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(harmonyJWT);
            HarmonyFetchDeepLinksBody body = new HarmonyFetchDeepLinksBody(null, idToken, moduleIds);
            HttpEntity<HarmonyFetchDeepLinksBody> entity = new HttpEntity<>(body, headers);

            ResponseEntity<HarmonyContentItemDTO[]> response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, HarmonyContentItemDTO[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Arrays.asList(Objects.requireNonNull(response.getBody()));
            } else {
                log.error("Error requesting deep links from {}", requestUrl);
                log.error("Harmony API returned {}", response.getStatusCode());
                log.error(String.valueOf(response.getBody()));
                return null;
            }
        } catch (Exception e) {
            log.error("Error requesting deep links from {}", requestUrl);
            log.error(e.getMessage());
            return null;
        }
    }

    public ResponseEntity<String> postLineitemsToHarmony(LineItems lineItems, String idToken) throws JsonProcessingException, DataServiceException {
        if (lineItems == null || lineItems.getLineItemList() == null || lineItems.getLineItemList().isEmpty()) {
            throw new DataServiceException("No lineitems to send to Harmony");
        }
        if (idToken == null || idToken.isEmpty()) {
            throw new DataServiceException("Must include an id_token when posting lineitems to harmony.");
        }

        RestTemplate restTemplate = RestUtils.createRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(harmonyJWT);
        Map<String, Object> body = new HashMap<>();
        body.put("id_token", idToken);
        body.put("lineitems", lineItems.getLineItemList());
        HttpEntity<String> entity = new HttpEntity<>(new ObjectMapper().writeValueAsString(body), headers);

        log.debug("Posting lineitems to {}", harmonyCoursesApiUrl + LINEITEMS_PATH);
        return restTemplate.exchange(harmonyCoursesApiUrl + LINEITEMS_PATH, HttpMethod.POST, entity, String.class);
    }

}
