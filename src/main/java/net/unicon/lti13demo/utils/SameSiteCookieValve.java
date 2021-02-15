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
/**
 * This file has been copied from the Oxford University repo under the Apache License
 * https://github.com/oxctl/spring-security-lti13-demo
 */
package net.unicon.lti13demo.utils;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.SessionConfig;
import org.apache.catalina.valves.ValveBase;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Collection;

/**
 * This valve adds an additional session cookie for clients that ignore all cookies with
 * SameSite=None (iOS 12).
 * We can't just add another cookie to the request in this valve because the session has already
 * been linked with the request by CoyoteAdaptor earlier in the call chain. I couldn't easily find a
 * way to have something like a valve execute before CoyoteAdaptor so we link the session here
 * ourselves.
 *
 * This class doesn't use a proper parse for the Set-Cookie header as we know the code building
 * the header (Tomcat) and I don't believe the extra complexity is worth it.
 */
public class SameSiteCookieValve extends ValveBase {

    // The suffix we want to append to the normal session cookie name for old clients.
    private final String suffix;

    public SameSiteCookieValve() {
        this("-legacy");
    }

    public SameSiteCookieValve(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String sessionCookieName = SessionConfig.getSessionCookieName(request.getContext());

        // Check for legacy cookie only if we haven't already found a valid session ID.
        if (!request.isRequestedSessionIdFromURL() && !request.isRequestedSessionIdFromCookie()) {
            Cookie[] cookies = request.getCookies();
            // Check if we've got a legacy cookie here.
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ((sessionCookieName+suffix).equals(cookie.getName())) {
                        request.setRequestedSessionId(cookie.getValue());
                        request.setRequestedSessionCookie(true);
                        request.setRequestedSessionURL(false);
                        break;
                    }
                }
            }
        }

        getNext().invoke(request, response);

        // Find all the cookies set on this request and add a duplicate if they are a SameSite one.
        Collection<String> headers = response.getHeaders("Set-Cookie");
        for (String header : headers) {
            // Rather than parsing the cookie we just knock out the bit we don't want.
            String nonSameSiteHeader = header.replaceAll("; SameSite=None", "");
            // We only do this for the main session cookie set by Tomcat
            if (!header.equals(nonSameSiteHeader) && header.contains(sessionCookieName+"=")) {
                nonSameSiteHeader = nonSameSiteHeader.replaceFirst("=", suffix+"=");
                response.addHeader("Set-Cookie", nonSameSiteHeader);
            }
        }

    }
}
