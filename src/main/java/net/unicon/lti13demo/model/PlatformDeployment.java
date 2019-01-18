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
package net.unicon.lti13demo.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "iss_configuration")
public class PlatformDeployment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "key_id")
    private long keyId;
    @Basic
    @Column(name = "iss", nullable = false, insertable = true, updatable = true)
    private String iss;  //The value we receive in the issuer from the platform. We will use it to know where this come from.
    @Basic
    @Column(name = "client_id", nullable = false, insertable = true, updatable = true)
    private String clientId;  //A tool MUST thus allow multiple deployments on a given platform to share the same client_id
    @Basic
    @Column(name = "oidc_endpoint", nullable = false, insertable = true, updatable = true)
    private String oidcEndpoint;  // Where in the platform we need to ask for the oidc authentication.
    @Basic
    @Column(name = "jwks_endpoint", nullable = true, insertable = true, updatable = true)
    private String jwksEndpoint;  // Where in the platform we need to ask for the keys.
    @Basic
    @Column(name = "oAuth2_token_url", nullable = true, insertable = true, updatable = true)
    private String oAuth2TokenUrl;  // Where in the platform we need to ask for the oauth2 tokens
    @Basic
    @Column(name = "deployment_id", nullable = false, insertable = true, updatable = true)
    private String deploymentId;  // Where in the platform we need to ask for the oidc authentication.
    @Basic
    @Column(name = "toolKid", nullable = true, insertable = true, updatable = true)
    private String toolKid; // The tool key if number.
    @Basic
    @Column(name = "platformKid", nullable = true, insertable = true, updatable = true)
    private String platformKid; // The tool key if number.

    @OneToMany(mappedBy = "platformDeployment", fetch = FetchType.LAZY)
    private Set<LtiContextEntity> contexts;



    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getOidcEndpoint() {
        return oidcEndpoint;
    }

    public void setOidcEndpoint(String oidcEndpoint) {
        this.oidcEndpoint = oidcEndpoint;
    }

    public String getJwksEndpoint() {
        return jwksEndpoint;
    }

    public void setJwksEndpoint(String jwksEndpoint) {
        this.jwksEndpoint = jwksEndpoint;
    }

    public String getoAuth2TokenUrl() {
        return oAuth2TokenUrl;
    }

    public void setoAuth2TokenUrl(String oAuth2TokenUrl) {
        this.oAuth2TokenUrl = oAuth2TokenUrl;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getToolKid() {
        return toolKid;
    }

    public void setToolKid(String toolKid) {
        this.toolKid = toolKid;
    }

    public String getPlatformKid() {
        return platformKid;
    }

    public void setPlatformKid(String platformKid) {
        this.platformKid = platformKid;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformDeployment that = (PlatformDeployment) o;

        if (keyId != that.keyId) return false;
        if (iss != null ? !iss.equals(that.iss) : that.iss != null) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        return deploymentId != null ? deploymentId.equals(that.deploymentId) : that.deploymentId == null;
    }

    @Override
    public int hashCode() {
        int result = (int) keyId;
        result = 31 * result + (iss != null ? iss.hashCode() : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (oidcEndpoint != null ? oidcEndpoint.hashCode() : 0);
        result = 31 * result + (oAuth2TokenUrl != null ? oAuth2TokenUrl.hashCode() : 0);
        result = 31 * result + (deploymentId != null ? deploymentId.hashCode() : 0);
        result = 31 * result + (toolKid != null ? toolKid.hashCode() : 0);
        result = 31 * result + (platformKid != null ? platformKid.hashCode() : 0);
        return result;
    }


}
