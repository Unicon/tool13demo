package net.unicon.lti.model.lti.dto;

import java.io.Serial;
import java.io.Serializable;

import java.util.List;
import java.util.Map;

public class ToolMessagesSupportedDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String type;
    String target_link_uri;
    String label;
    String icon_uri;
    private Map<String, String> custom_parameters;
    List<String> placements;

    public ToolMessagesSupportedDTO() {//Empty on purpose
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget_link_uri() {
        return target_link_uri;
    }

    public void setTarget_link_uri(String target_link_uri) {
        this.target_link_uri = target_link_uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon_uri() {
        return icon_uri;
    }

    public void setIcon_uri(String icon_uri) {
        this.icon_uri = icon_uri;
    }

    public Map<String, String> getCustom_parameters() {
        return custom_parameters;
    }

    public void setCustom_parameters(Map<String, String> custom_parameters) {
        this.custom_parameters = custom_parameters;
    }

    public List<String> getPlacements() {
        return placements;
    }

    public void setPlacements(List<String> placements) {
        this.placements = placements;
    }
}
