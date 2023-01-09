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
package net.unicon.lti.model.lti.dto;

import javax.servlet.http.HttpServletRequest;

import static net.unicon.lti.utils.LtiStrings.OIDC_CLIENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_DEPLOYMENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_ISS;
import static net.unicon.lti.utils.LtiStrings.OIDC_LOGIN_HINT;
import static net.unicon.lti.utils.LtiStrings.OIDC_LTI_MESSAGE_HINT;
import static net.unicon.lti.utils.LtiStrings.OIDC_TARGET_LINK_URI;

public class LoginInitiationDTO {

    private String iss;
    private String loginHint;
    private String targetLinkUri;
    private String ltiMessageHint;
    private String clientId;
    private String deploymentId;


    public LoginInitiationDTO() {//Empty on purpose
    }

    public LoginInitiationDTO(String iss, String loginHint, String targetLinkUri, String ltiMessageHint, String clientId, String deploymentId) {
        this.iss = iss;
        this.loginHint = loginHint;
        this.targetLinkUri = targetLinkUri;
        this.ltiMessageHint = ltiMessageHint;
        this.clientId = clientId;
        this.deploymentId = deploymentId;
    }

    public LoginInitiationDTO(HttpServletRequest req) {
        this(req.getParameter(OIDC_ISS),
                req.getParameter(OIDC_LOGIN_HINT),
                req.getParameter(OIDC_TARGET_LINK_URI),
                req.getParameter(OIDC_LTI_MESSAGE_HINT),
                req.getParameter(OIDC_CLIENT_ID),
                req.getParameter(OIDC_DEPLOYMENT_ID)
        );
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public String toString() {
        return "LoginInitiationDTO{" +
                "iss='" + iss + '\'' +
                ", loginHint='" + loginHint + '\'' +
                ", targetLinkUri='" + targetLinkUri + '\'' +
                ", ltiMessageHint='" + ltiMessageHint + '\'' +
                ", clientId='" + clientId + '\'' +
                ", deploymentId='" + deploymentId + '\'' +
                '}';
    }
}
