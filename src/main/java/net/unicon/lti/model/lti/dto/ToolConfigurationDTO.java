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

import java.util.List;
import java.util.Map;

public class ToolConfigurationDTO {

    private String domain;
    private List<String> secondary_domains;
    private String deployment_id;
    private String target_link_uri;
    private Map<String, String> custom_parameters;
    private String description;
    private List<ToolMessagesSupportedDTO> messages_supported;
    private List<String> claims;


    public ToolConfigurationDTO() {//Empty on purpose
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<String> getSecondary_domains() {
        return secondary_domains;
    }

    public String getDeployment_id() {
        return deployment_id;
    }

    public void setDeployment_id(String deployment_id) {
        this.deployment_id = deployment_id;
    }

    public void setSecondary_domains(List<String> secondary_domains) {
        this.secondary_domains = secondary_domains;
    }

    public String getTarget_link_uri() {
        return target_link_uri;
    }

    public void setTarget_link_uri(String target_link_uri) {
        this.target_link_uri = target_link_uri;
    }

    public Map<String, String> getCustom_parameters() {
        return custom_parameters;
    }

    public void setCustom_parameters(Map<String, String> custom_parameters) {
        this.custom_parameters = custom_parameters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ToolMessagesSupportedDTO> getMessages_supported() {
        return messages_supported;
    }

    public void setMessages_supported(List<ToolMessagesSupportedDTO> messages_supported) {
        this.messages_supported = messages_supported;
    }

    public List<String> getClaims() {
        return claims;
    }

    public void setClaims(List<String> claims) {
        this.claims = claims;
    }
}
