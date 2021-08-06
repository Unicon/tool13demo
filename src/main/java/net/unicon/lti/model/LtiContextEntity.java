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
package net.unicon.lti.model;

import org.apache.commons.lang3.StringUtils;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "lti_context")
public class LtiContextEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "context_id", nullable = false)
    private long contextId;
    @Basic
    @Column(name = "context_key", nullable = false, length = 4096)
    private String contextKey;
    @Basic
    @Column(name = "title", length = 4096)
    private String title;
    @Basic
    @Column(name = "context_memberships_url", length = 4096)
    private String context_memberships_url;
    @Basic
    @Column(name = "lineitems", length = 4096)
    private String lineitems;
    @Lob
    @Column(name = "json")
    private String json;
    @Lob
    @Column(name = "settings")
    private String settings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "key_id", referencedColumnName = "key_id", nullable = false)
    private PlatformDeployment platformDeployment;

    @OneToMany(mappedBy = "context")
    private Set<LtiLinkEntity> links;
    @OneToMany(mappedBy = "context")
    private Set<LtiMembershipEntity> memberships;

    public LtiContextEntity() {
    }

    public LtiContextEntity(String contextKey, PlatformDeployment platformDeployment, String title, String json) {
        if (!StringUtils.isNotBlank(contextKey)) throw new AssertionError();
        if (platformDeployment == null) throw new AssertionError();
        this.contextKey = contextKey;
        this.platformDeployment = platformDeployment;
        this.title = title;
        this.json = json;
    }

    public LtiContextEntity(String contextKey, PlatformDeployment platformDeployment, String title, String context_memberships_url, String lineitems, String json) {
        if (!StringUtils.isNotBlank(contextKey)) throw new AssertionError();
        if (platformDeployment == null) throw new AssertionError();
        this.contextKey = contextKey;
        this.platformDeployment = platformDeployment;
        this.title = title;
        this.context_memberships_url = context_memberships_url;
        this.lineitems = lineitems;
        this.json = json;
    }

    public long getContextId() {
        return contextId;
    }

    public void setContextId(long contextId) {
        this.contextId = contextId;
    }

    public String getContextKey() {
        return contextKey;
    }

    public void setContextKey(String contextKey) {
        this.contextKey = contextKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public PlatformDeployment getPlatformDeployment() {
        return platformDeployment;
    }

    public void setPlatformDeployment(PlatformDeployment platformDeployment) {
        this.platformDeployment = platformDeployment;
    }

    public Set<LtiLinkEntity> getLinks() {
        return links;
    }

    public void setLinks(Set<LtiLinkEntity> links) {
        this.links = links;
    }

    public Set<LtiMembershipEntity> getMemberships() {
        return memberships;
    }

    public void setMemberships(Set<LtiMembershipEntity> memberships) {
        this.memberships = memberships;
    }

    public String getContext_memberships_url() {
        return context_memberships_url;
    }

    public void setContext_memberships_url(String context_memberships_url) {
        this.context_memberships_url = context_memberships_url;
    }

    public String getLineitems() {
        return lineitems;
    }

    public void setLineitems(String lineitems) {
        this.lineitems = lineitems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LtiContextEntity that = (LtiContextEntity) o;

        if (contextId != that.contextId) return false;
        return Objects.equals(contextKey, that.contextKey);
    }

    @Override
    public int hashCode() {
        int result = (int) contextId;
        result = 31 * result + (contextKey != null ? contextKey.hashCode() : 0);
        return result;
    }

}
