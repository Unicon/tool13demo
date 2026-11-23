package net.unicon.lti.model.lti.dto;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.util.List;
import java.util.Map;

public class ToolConfigurationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    private String domain;
    private List<String> secondary_domains;
    private String deployment_id;
    private String target_link_uri;
    private Map<String, String> custom_parameters;
    private String description;
    @JsonProperty("messages")
    private List<ToolMessagesSupportedDTO> messages;
    private List<String> claims;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("https://canvas.instructure.com/lti/privacy_level")
    private String canvasPrivacyLevel;


    public ToolConfigurationDTO() {//Empty on purpose
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<String> getSecondary_domains() {
        return secondary_domains;
    }

    public String getDeployment_id() {
        return deployment_id;
    }

    public void setDeployment_id(String deployment_id) {
        this.deployment_id = deployment_id;
    }

    public void setSecondary_domains(List<String> secondary_domains) {
        this.secondary_domains = secondary_domains;
    }

    public String getTarget_link_uri() {
        return target_link_uri;
    }

    public void setTarget_link_uri(String target_link_uri) {
        this.target_link_uri = target_link_uri;
    }

    public Map<String, String> getCustom_parameters() {
        return custom_parameters;
    }

    public void setCustom_parameters(Map<String, String> custom_parameters) {
        this.custom_parameters = custom_parameters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ToolMessagesSupportedDTO> getMessages_supported() {
        return messages;
    }

    public void setMessages_supported(List<ToolMessagesSupportedDTO> messages) {
        this.messages = messages;
    }

    public List<String> getClaims() {
        return claims;
    }

    public void setClaims(List<String> claims) {
        this.claims = claims;
    }

    public String getCanvasPrivacyLevel() { return canvasPrivacyLevel; }

    public void setCanvasPrivacyLevel(String canvasPrivacyLevel) { this.canvasPrivacyLevel = canvasPrivacyLevel; }


    @Override
    public String toString() {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            return ow.writeValueAsString(this);
        } catch (JacksonException e) {
            return "ToolConfigurationDTO{" +
                    "domain='" + domain + '\'' +
                    ", secondary_domains=" + secondary_domains +
                    ", deployment_id='" + deployment_id + '\'' +
                    ", target_link_uri='" + target_link_uri + '\'' +
                    ", custom_parameters=" + custom_parameters +
                    ", description='" + description + '\'' +
                    ", messages=" + messages +
                    ", claims=" + claims +
                    ", canvasPrivacyLevel='" + canvasPrivacyLevel + '\'' +
                    '}';
        }
    }
}