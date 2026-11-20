/**
 * Copyright 2026 Unicon (R)
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
package net.unicon.lti.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Display helpers for the demo templates, available in Thymeleaf as ${@demoFormatter...}.
 *
 * These only format data that has already been received and validated elsewhere. Nothing here verifies a signature.
 */
@Component("demoFormatter")
public class DemoFormatter {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Pretty-prints an object as JSON. A String that already holds JSON is re-indented; any other String is returned as is.
     */
    public String json(Object value) {
        if (value == null) {
            return "";
        }
        try {
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (!(text.startsWith("{") || text.startsWith("["))) {
                    return text;
                }
                return mapper.writeValueAsString(mapper.readTree(text));
            }
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * Decodes the payload (claims) of a compact JWT and pretty-prints it.
     */
    public String jwtPayload(String jwt) {
        return jwtPart(jwt, 1);
    }

    /**
     * Decodes the header of a compact JWT and pretty-prints it.
     */
    public String jwtHeader(String jwt) {
        return jwtPart(jwt, 0);
    }

    private String jwtPart(String jwt, int index) {
        if (StringUtils.isBlank(jwt)) {
            return "";
        }
        String[] parts = jwt.split("\\.");
        if (parts.length <= index) {
            return "";
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[index]);
            JsonNode node = mapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Shortens an LTI role URI to its readable name, for example
     * "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor" becomes "Instructor".
     */
    public String role(String roleUri) {
        if (roleUri == null) {
            return "";
        }
        int hash = roleUri.lastIndexOf('#');
        if (hash >= 0 && hash < roleUri.length() - 1) {
            return roleUri.substring(hash + 1);
        }
        int slash = roleUri.lastIndexOf('/');
        return slash >= 0 && slash < roleUri.length() - 1 ? roleUri.substring(slash + 1) : roleUri;
    }

    /**
     * Short role names for a list of role URIs, without duplicates, in their original order.
     */
    public List<String> roles(Collection<String> roleUris) {
        List<String> names = new ArrayList<>();
        if (roleUris != null) {
            for (String uri : roleUris) {
                String name = role(uri);
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * Shortens a long value (such as a JWT) for display: the first and last few characters around an ellipsis.
     */
    public String abbreviate(String value, int keep) {
        if (value == null || value.length() <= keep * 2 + 3) {
            return value;
        }
        return value.substring(0, keep) + "…" + value.substring(value.length() - keep);
    }
}
