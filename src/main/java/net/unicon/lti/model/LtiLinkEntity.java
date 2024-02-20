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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "lti_link")
public class LtiLinkEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id", nullable = false)
    private long linkId;
    @Basic
    @Column(name = "lti_link_id", length = 256)
    private String ltiLinkId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tool_link_id")
    private ToolLink toolLink;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "context_id")
    private LtiContextEntity context;
    @OneToMany(mappedBy = "link", fetch = FetchType.LAZY)
    private Set<LtiResultEntity> results;

    protected LtiLinkEntity() {
    }

    /**
     * @param toolLink the external id for this link
     * @param context the LTI context
     */
    public LtiLinkEntity(LtiContextEntity context, ToolLink toolLink) {
        if (toolLink == null) throw new AssertionError();
        if (context == null) throw new AssertionError();
        this.context = context;
        this.toolLink = toolLink;
    }

    public long getLinkId() {
        return linkId;
    }

    public void setLinkId(long linkId) {
        this.linkId = linkId;
    }

    public ToolLink getToolLink() {
        return toolLink;
    }

    public void setToolLink(ToolLink toolLink) {
        this.toolLink = toolLink;
    }

    public LtiContextEntity getContext() {
        return context;
    }

    public void setContext(LtiContextEntity context) {
        this.context = context;
    }

    public Set<LtiResultEntity> getResults() {
        return results;
    }

    public void setResults(Set<LtiResultEntity> results) {
        this.results = results;
    }

    public String getLtiLinkId() {
        return ltiLinkId;
    }

    public void setLtiLinkId(String ltiLinkId) {
        this.ltiLinkId = ltiLinkId;
    }

    public String createHtmlFromLink() {
        return "Link Requested:\n" +
                "Tool Link Id:" +
                toolLink.getToolLinkId() +
                "\nLink Title:" +
                toolLink.getTitle() +
                "\nDescription:" +
                toolLink.getDescription() +
                "\nLTI Link Id:" +
                this.getLtiLinkId() +
                "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LtiLinkEntity that = (LtiLinkEntity) o;

        if (linkId != that.linkId) return false;
        return (Objects.equals(toolLink, that.toolLink) && Objects.equals(getContext().getContextId(), that.getContext().getContextId()));
    }

    @Override
    public int hashCode() {
        int result = (int) linkId;
        result = 31 * result + (toolLink.getTitle().hashCode()) + getContext().hashCode();
        return result;
    }

}
