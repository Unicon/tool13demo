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

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This manages all the interactions with Harmony
 */
@Service
@Slf4j
public class HarmonyService {
    private static final String DEEP_LINKS_PATH = "/lti_deep_links";

    @Value("${harmony.courses.api}")
    private String harmonyCoursesApiUrl;

    @Value("${harmony.courses.jwt}")
    private String harmonyJWT;

    RestTemplate restTemplate;

    public HarmonyPageResponse fetchHarmonyCourses(int page) {

        if (StringUtils.isAnyBlank(harmonyCoursesApiUrl, harmonyJWT)) {
            log.warn("The Harmony Courses API has not been configured, courses will not be fetched.");
            return null;
        }

        try {

            restTemplate = restTemplate == null ? new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())) : restTemplate;

            // Build the URL
            String requestUrl = harmonyCoursesApiUrl;
            // Only append the page parameter when it's needed.
            if (page != 1) {
                URIBuilder builder = new URIBuilder(harmonyCoursesApiUrl);
                // Add the page parameter since this API is paginated.
                builder.setParameter("page", String.valueOf(page));
                requestUrl = builder.build().toString();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(harmonyJWT);
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
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

    public List<HarmonyContentItemDTO> fetchDeepLinkingContentItems(String rootOutcomeGuid, String piGuid) {
        if (StringUtils.isAnyBlank(harmonyCoursesApiUrl, harmonyJWT)) {
            log.warn("The Harmony API has not been configured, deep links will not be fetched.");
            return null;
        }

        if (rootOutcomeGuid == null || rootOutcomeGuid.isEmpty()) {
            log.warn("Cannot fetch deep links without guid (root_outcome_guid)");
            return null;
        }

        if (piGuid == null || piGuid.isEmpty()) {
            log.warn("Cannot fetch deep links without pi_guid (tool_platform.guid from id_token)");
            return null;
        }

        try {
            restTemplate = restTemplate == null ? new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())) : restTemplate;

            // Build the URL
            String requestUrl = harmonyCoursesApiUrl;

            URIBuilder builder = new URIBuilder(harmonyCoursesApiUrl + DEEP_LINKS_PATH);
            builder.addParameter("guid", rootOutcomeGuid);
            builder.addParameter("pi_guid", piGuid);
            requestUrl = builder.build().toString();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(harmonyJWT);
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            ResponseEntity<HarmonyContentItemDTO[]> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, HarmonyContentItemDTO[].class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Got a successful response from the Harmony Deep Links API for guid {} and pi_guid {}:\n {}", rootOutcomeGuid, piGuid, Arrays.toString(response.getBody()));
                return Arrays.asList(Objects.requireNonNull(response.getBody()));
            } else {
                log.error("Harmony API returned {}", response.getStatusCode());
                log.error(String.valueOf(response.getBody()));
                return null;
            }
        } catch (Exception e) {
            System.out.println("Exception thrown");
            log.error("Error requesting deep links with guid {} and pi_guid {}", rootOutcomeGuid, piGuid);
            log.error(e.getMessage());
            return null;
        }
    }

}
