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
package net.unicon.lti.model.lti.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.unicon.lti.utils.lti.MessagesSupportedDeserializer;

import java.util.List;

@JsonDeserialize(using = MessagesSupportedDeserializer.class)
public class MessagesSupportedDTO {
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
