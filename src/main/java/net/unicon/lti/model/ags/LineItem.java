package net.unicon.lti.model.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItem {
    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    @JsonProperty("scoreMaximum")
    private String scoreMaximum;
    @JsonProperty("label")
    private String label;
    @JsonProperty("resourceId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String resourceId;
    @JsonProperty("tag")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tag;
    @JsonProperty("resourceLinkId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String resourceLinkId;
    @JsonProperty("startDateTime")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String startDateTime;
    @JsonProperty("endDateTime")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String endDateTime;
    @JsonProperty("results")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Results results;

    public LineItem() { //Empty on purpose
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScoreMaximum() {
        return scoreMaximum;
    }

    public void setScoreMaximum(String scoreMaximum) {
        this.scoreMaximum = scoreMaximum;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getResourceLinkId() {
        return resourceLinkId;
    }

    public void setResourceLinkId(String resourceLinkId) {
        this.resourceLinkId = resourceLinkId;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }
}
