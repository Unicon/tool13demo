package net.unicon.lti.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "iss_configuration")
public class PlatformDeployment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // named explicitly: LtiContextEntity and LtiUserEntity join on referencedColumnName = "key_id",
    // which Hibernate resolves before the naming strategy runs
    @Column(name = "key_id")
    private long keyId;
    @Basic
    @Column(nullable = false)
    private String iss;  //The value we receive in the issuer from the platform. We will use it to know where this come from.
    @Basic
    @Column(nullable = false)
    private String clientId;  //A tool MUST thus allow multiple deployments on a given platform to share the same client_id
    @Basic
    @Column(nullable = false)
    private String oidcEndpoint;  // Where in the platform we need to ask for the oidc authentication.
    @Basic
    private String jwksEndpoint;  // Where in the platform we need to ask for the keys.
    @Basic
    @Column(name = "oAuth2_token_url")
    private String oAuth2TokenUrl;  // Where in the platform we need to ask for the oauth2 tokens
    @Basic
    @Column(name = "oAuth2_token_aud")
    private String oAuth2TokenAud;  // Sometimes, for example D2L, has a different aud for the tokens.
    @Basic
    private String deploymentId;  // Where in the platform we need to ask for the oidc authentication.

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    // Jackson 3 only treats getX/setX with a capital after get/set as accessors, so these names
    // (kept for the JSON the seed files and the /config API use) have to be given explicitly
    @JsonProperty("oAuth2TokenUrl")
    public String getoAuth2TokenUrl() {
        return oAuth2TokenUrl;
    }

    @JsonProperty("oAuth2TokenUrl")
    public void setoAuth2TokenUrl(String oAuth2TokenUrl) {
        this.oAuth2TokenUrl = oAuth2TokenUrl;
    }

    @JsonProperty("oAuth2TokenAud")
    public String getoAuth2TokenAud() {
        return oAuth2TokenAud;
    }

    @JsonProperty("oAuth2TokenAud")
    public void setoAuth2TokenAud(String oAuth2TokenAud) {
        this.oAuth2TokenAud = oAuth2TokenAud;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @JsonIgnore
    public Set<LtiContextEntity> getContexts() {
        return contexts;
    }

    public void setContexts(Set<LtiContextEntity> contexts) {
        this.contexts = contexts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformDeployment that = (PlatformDeployment) o;

        if (keyId != that.keyId) return false;
        if (!Objects.equals(iss, that.iss)) return false;
        if (!Objects.equals(clientId, that.clientId)) return false;
        return Objects.equals(deploymentId, that.deploymentId);
    }

    @Override
    public int hashCode() {
        // the same fields equals() compares
        return Objects.hash(keyId, iss, clientId, deploymentId);
    }


}
