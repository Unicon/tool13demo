package net.unicon.lti.model.lti.dto;

import java.io.Serial;
import java.io.Serializable;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.util.List;

public class PlatformConfigurationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    private String product_family_code;
    private String version;
    private List<MessagesSupportedDTO> messages_supported;
    private List<String> variables;


    public PlatformConfigurationDTO() {//Empty on purpose
    }

    public String getProduct_family_code() {
        return product_family_code;
    }

    public void setProduct_family_code(String product_family_code) {
        this.product_family_code = product_family_code;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<MessagesSupportedDTO> getMessages_supported() {
        return messages_supported;
    }

    public void setMessages_supported(List<MessagesSupportedDTO> messages_supported) {
        this.messages_supported = messages_supported;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }

    @Override
    public String toString() {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            return ow.writeValueAsString(this);
        } catch (JacksonException e) {
            return "PlatformConfigurationDTO could not be serialized";
        }
    }
}
