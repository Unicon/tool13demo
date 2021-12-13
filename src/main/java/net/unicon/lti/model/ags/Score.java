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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {
    @JsonProperty(value = "userId", required = true)
    private String userId;

    @JsonProperty("scoreMaximum")
    private float scoreMaximum;

    @JsonProperty("scoreGiven")
    private float scoreGiven;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonProperty("comment")
    private String comment;

    @JsonProperty(value = "activityProgress", required = true)
    private String activityProgress;

    @JsonProperty(value = "gradingProgress", required = true)
    private String gradingProgress;

    @JsonProperty(value = "timestamp", required = true)
    private String timestamp;
}
