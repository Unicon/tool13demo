/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.model.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItem {
    @JsonProperty("id")
    private String id;
    @JsonProperty("scoreMaximum")
    private String scoreMaximum;
    @JsonProperty("label")
    private String label;
    @JsonProperty("resourceId")
    private String resourceId;
    @JsonProperty("tag")
    private String tag;
    @JsonProperty("resourceLinkId")
    private String resourceLinkId;

    public LineItem() {
    }

    public LineItem(String id, String scoreMaximum, String label, String resourceId, String tag, String resourceLinkId) {
        this.id = id;
        this.scoreMaximum = scoreMaximum;
        this.label = label;
        this.resourceId = resourceId;
        this.tag = tag;
        this.resourceLinkId = resourceLinkId;
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
}
