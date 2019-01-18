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
package net.unicon.lti13demo.model.dto;

import javax.servlet.http.HttpServletRequest;

public class LoginInitiationDTO {

    private String iss;
    private String loginHint;
    private String targetLinkUri;
    private String ltiMessageHint;

    public LoginInitiationDTO() {
    }

    public LoginInitiationDTO(String iss, String loginHint, String targetLinkUri, String ltiMessageHint) {
        this.iss = iss;
        this.loginHint = loginHint;
        this.targetLinkUri = targetLinkUri;
        this.ltiMessageHint = ltiMessageHint;
    }

    public LoginInitiationDTO(HttpServletRequest req) {
        this(req.getParameter("iss"),
                req.getParameter("login_hint"),
                req.getParameter("target_link_uri"),
                req.getParameter("lti_message_hint"));
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getTargetLinkUri() {
        return targetLinkUri;
    }

    public void setTargetLinkUri(String targetLinkUri) {
        this.targetLinkUri = targetLinkUri;
    }

    public String getLtiMessageHint() {
        return ltiMessageHint;
    }

    public void setLtiMessageHint(String ltiMessageHint) {
        this.ltiMessageHint = ltiMessageHint;
    }
}
