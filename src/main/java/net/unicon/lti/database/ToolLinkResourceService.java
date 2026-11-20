/**
 * Copyright 2026 Unicon (R)
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
package net.unicon.lti.database;

import net.unicon.lti.model.ToolLink;
import net.unicon.lti.service.common.ResourceService;
import org.springframework.stereotype.Component;

/**
 * Sample content items (the deep-linking "Content Menu"), one JSON file per item.
 */
@Component
public class ToolLinkResourceService implements ResourceService<ToolLink> {

    static final String TOOL_LINK_RESOURCE = "classpath:data/tool_link";

    @Override
    public String getDirectoryPath() {
        return TOOL_LINK_RESOURCE;
    }

    @Override
    public void setDefaults() { //Empty on purpose

    }

}
