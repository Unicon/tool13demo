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
