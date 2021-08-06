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
package net.unicon.lti.model.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("scoreMaximum")
    private String scoreMaximum;
    @JsonProperty("scoreGiven")
    private String scoreGiven;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("activityProgress")
    private String activityProgress;
    @JsonProperty("gradingProgress")
    private String gradingProgress;
    @JsonProperty("timestamp")
    private String timestamp;

    public Score() { //Empty on purpose
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScoreMaximum() {
        return scoreMaximum;
    }

    public void setScoreMaximum(String scoreMaximum) {
        this.scoreMaximum = scoreMaximum;
    }

    public String getScoreGiven() {
        return scoreGiven;
    }

    public void setScoreGiven(String scoreGiven) {
        this.scoreGiven = scoreGiven;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getActivityProgress() {
        return activityProgress;
    }

    public void setActivityProgress(String activityProgress) {
        this.activityProgress = activityProgress;
    }

    public String getGradingProgress() {
        return gradingProgress;
    }

    public void setGradingProgress(String gradingProgress) {
        this.gradingProgress = gradingProgress;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
