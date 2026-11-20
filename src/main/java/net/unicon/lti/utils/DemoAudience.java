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

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Who a demo launch is for. Only used when lti13.demoMode is true.
 *
 * TECHNICAL keeps the "Behind the Scenes" pages that pause at each step of the launch.
 * CLIENT runs the same LTI flow (including the cookieless postMessage checks) without pausing,
 * and lands on a page that looks like a content tool rather than a protocol inspector.
 *
 * The choice is made before the OIDC redirect (from the target_link_uri, a configured default,
 * or the chooser page) and carried through the launch inside the signed state JWT, so it needs no cookies.
 */
public final class DemoAudience {

    public static final String CLIENT = "client";
    public static final String TECHNICAL = "technical";

    /** Request parameter (chooser form) and target_link_uri query parameter. */
    public static final String PARAM = "demo_audience";
    /** Claim name inside the state JWT. */
    public static final String STATE_CLAIM = "demoAudience";
    /** Model attribute name used by the Thymeleaf templates. */
    public static final String MODEL_ATTRIBUTE = "demoAudience";
    /** Query parameter on target_link_uri that identifies a tool link (deep-linked content). */
    public static final String LINK_PARAM = "link";

    private DemoAudience() {
    }

    /**
     * Returns CLIENT or TECHNICAL for a recognized value, otherwise null.
     */
    public static String normalize(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim().toLowerCase();
        if (CLIENT.equals(trimmed) || TECHNICAL.equals(trimmed)) {
            return trimmed;
        }
        return null;
    }

    public static boolean isClient(String value) {
        return CLIENT.equals(normalize(value));
    }

    /**
     * Reads the demo_audience query parameter from a target_link_uri, if present.
     */
    public static String fromTargetLinkUri(String targetLinkUri) {
        return normalize(queryParam(targetLinkUri, PARAM));
    }

    /**
     * Reads the tool link id (the "link" query parameter) from a target_link_uri, if present.
     */
    public static String linkFromTargetLinkUri(String targetLinkUri) {
        return StringUtils.trimToNull(queryParam(targetLinkUri, LINK_PARAM));
    }

    private static String queryParam(String uri, String name) {
        if (StringUtils.isBlank(uri)) {
            return null;
        }
        try {
            return UriComponentsBuilder.fromUriString(uri).build().getQueryParams().getFirst(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
