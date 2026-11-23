package net.unicon.lti.model.lti.dto;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

public class ToolConfigurationACKDTO extends ToolRegistrationDTO{

    private String client_id;
    private String registration_client_uri;

    public ToolConfigurationACKDTO() {//Empty on purpose
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    @Override
    public String toString() {
        try {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            return ow.writeValueAsString(this);
        } catch (JacksonException e) {
            return "ToolConfigurationACKDTO{" +
                    "client_id='" + client_id + '\'' +
                    ", registration_client_uri='" + registration_client_uri + '\'' +
                    "} " + super.toString();
        }
    }
}