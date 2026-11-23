package net.unicon.lti.model.lti.dto;

import java.io.Serial;
import java.io.Serializable;

import tools.jackson.databind.annotation.JsonDeserialize;
import net.unicon.lti.utils.lti.MessagesSupportedDeserializer;

import java.util.List;

@JsonDeserialize(using = MessagesSupportedDeserializer.class)
public class MessagesSupportedDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String type;
    List<String> placements;


    public MessagesSupportedDTO() {//Empty on purpose
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getPlacements() {
        return placements;
    }

    public void setPlacements(List<String> placements) {
        this.placements = placements;
    }
}
