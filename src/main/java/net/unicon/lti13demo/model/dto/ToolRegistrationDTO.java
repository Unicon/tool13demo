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
package net.unicon.lti13demo.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class ToolRegistrationDTO {

    private String application_type;
    private List<String> grant_types;
    private List<String> response_types;
    private List<String> redirect_uris;
    private String initiate_login_uri;
    private String client_name;
    private String jwks_uri;
    private String logo_uri;
    private String token_endpoint_auth_method;
    private List<String> contacts;
    private String client_uri;
    private String tos_uri;
    private String policy_uri;
    @JsonProperty("https://purl.imsglobal.org/spec/lti-tool-configuration")
    private ToolConfigurationDTO toolConfiguration;
    private List<String> scope;

    public ToolRegistrationDTO() {
    }

    public ToolRegistrationDTO(String application_type, List<String> grant_types, List<String> response_types, List<String> redirect_uris, String initiate_login_uri, String client_name, String jwks_uri, String logo_uri, String token_endpoint_auth_method, List<String> contacts, String client_uri, String tos_uri, String policy_uri, ToolConfigurationDTO toolConfiguration, List<String> scope) {
        this.application_type = application_type;
        this.grant_types = grant_types;
        this.response_types = response_types;
        this.redirect_uris = redirect_uris;
        this.initiate_login_uri = initiate_login_uri;
        this.client_name = client_name;
        this.jwks_uri = jwks_uri;
        this.logo_uri = logo_uri;
        this.token_endpoint_auth_method = token_endpoint_auth_method;
        this.contacts = contacts;
        this.client_uri = client_uri;
        this.tos_uri = tos_uri;
        this.policy_uri = policy_uri;
        this.toolConfiguration = toolConfiguration;
        this.scope = scope;
    }

    public String getApplication_type() {
        return application_type;
    }

    public void setApplication_type(String application_type) {
        this.application_type = application_type;
    }

    public List<String> getGrant_types() {
        return grant_types;
    }

    public void setGrant_types(List<String> grant_types) {
        this.grant_types = grant_types;
    }

    public List<String> getResponse_types() {
        return response_types;
    }

    public void setResponse_types(List<String> response_types) {
        this.response_types = response_types;
    }

    public List<String> getRedirect_uris() {
        return redirect_uris;
    }

    public void setRedirect_uris(List<String> redirect_uris) {
        this.redirect_uris = redirect_uris;
    }

    public String getInitiate_login_uri() {
        return initiate_login_uri;
    }

    public void setInitiate_login_uri(String initiate_login_uri) {
        this.initiate_login_uri = initiate_login_uri;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getLogo_uri() {
        return logo_uri;
    }

    public void setLogo_uri(String logo_uri) {
        this.logo_uri = logo_uri;
    }

    public String getToken_endpoint_auth_method() {
        return token_endpoint_auth_method;
    }

    public void setToken_endpoint_auth_method(String token_endpoint_auth_method) {
        this.token_endpoint_auth_method = token_endpoint_auth_method;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getClient_uri() {
        return client_uri;
    }

    public void setClient_uri(String client_uri) {
        this.client_uri = client_uri;
    }

    public String getTos_uri() {
        return tos_uri;
    }

    public void setTos_uri(String tos_uri) {
        this.tos_uri = tos_uri;
    }

    public String getPolicy_uri() {
        return policy_uri;
    }

    public void setPolicy_uri(String policy_uri) {
        this.policy_uri = policy_uri;
    }

    public ToolConfigurationDTO getToolConfiguration() {
        return toolConfiguration;
    }

    public void setToolConfiguration(ToolConfigurationDTO toolConfiguration) {
        this.toolConfiguration = toolConfiguration;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }
}
