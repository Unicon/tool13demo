package net.unicon.lti.model.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
    @JsonProperty("id")
    private String id;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("resultMaximum")
    private String resultMaximum;
    @JsonProperty("resultScore")
    private String resultScore;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("scoreOf")
    private String scoreOf;

    public Result() { //Empty on purpose
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResultMaximum() {
        return resultMaximum;
    }

    public void setResultMaximum(String resultMaximum) {
        this.resultMaximum = resultMaximum;
    }

    public String getResultScore() {
        return resultScore;
    }

    public void setResultScore(String resultScore) {
        this.resultScore = resultScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getScoreOf() {
        return scoreOf;
    }

    public void setScoreOf(String scoreOf) {
        this.scoreOf = scoreOf;
    }
}
