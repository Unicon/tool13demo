package net.unicon.lti.utils.lti;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The demo's content is launched as {target link uri}?link={tool link id}; launches from other
 * placements, such as course navigation, carry no link.
 */
public final class LtiLinkUtils {

    public static final String LINK_PARAMETER = "link";

    private LtiLinkUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @return the tool link id named by the launch's target link URI, or null if it names none
     */
    public static String linkParameter(String targetLinkUri) {
        if (StringUtils.isBlank(targetLinkUri)) {
            return null;
        }

        try {
            String link = UriComponentsBuilder.fromUriString(targetLinkUri).build().getQueryParams().getFirst(LINK_PARAMETER);

            return StringUtils.isBlank(link) ? null : URLDecoder.decode(link, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // not a URI we can read a link from
            return null;
        }
    }

}
