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
package net.unicon.lti13demo.model.dto;

import java.util.List;

public class PlatformConfigurationDTO {

    private String product_family_code;
    private String version;
    private List<MessagesSupportedDTO> messages_supported;
    private List<String> variables;


    public PlatformConfigurationDTO() {//Empty on purpose
    }

    public String getProduct_family_code() {
        return product_family_code;
    }

    public void setProduct_family_code(String product_family_code) {
        this.product_family_code = product_family_code;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<MessagesSupportedDTO> getMessages_supported() {
        return messages_supported;
    }

    public void setMessages_supported(List<MessagesSupportedDTO> messages_supported) {
        this.messages_supported = messages_supported;
    }

    public List<String> getVariables() {
        return variables;
    }

    public void setVariables(List<String> variables) {
        this.variables = variables;
    }
}
