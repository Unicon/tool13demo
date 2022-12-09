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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "alternative_domain")
public class AlternativeDomain extends BaseEntity {

    @Id
    @Column(name = "alt_domain", nullable = false)
    private String altDomain;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "menu_label", length = 512)
    private String menuLabel;

    @Column(name = "local_url", length = 512)
    private String localUrl;

    @Column(name = "domain_url", length = 512)
    private String domainUrl;

    protected AlternativeDomain() {
    }

    public AlternativeDomain(String altDomain, String name, String description, String menuLabel, String localUrl, String domainUrl) {
        if (!StringUtils.isNotBlank(altDomain)) throw new AssertionError();
        if (!StringUtils.isNotBlank(name)) throw new AssertionError();
        this.altDomain = altDomain;
        this.name = name;
        this.description = description;
        this.menuLabel = menuLabel;
        this.localUrl = localUrl;
        this.domainUrl = domainUrl;
    }


    public String getAltDomain() {
        return altDomain;
    }

    public void setAltDomain(String altDomain) {
        this.altDomain = altDomain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMenuLabel() {
        return menuLabel;
    }

    public void setMenuLabel(String menuLabel) {
        this.menuLabel = menuLabel;
    }

    public String getLocalUrl() {
        return localUrl;
    }

    public void setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
    }

    public String getDomainUrl() {
        return domainUrl;
    }

    public void setDomainUrl(String domainUrl) {
        this.domainUrl = domainUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlternativeDomain that = (AlternativeDomain) o;
        return Objects.equals(altDomain, that.altDomain);
    }

    @Override
    public int hashCode() {
        int result = 31 * altDomain.hashCode();
        return result + 31 * name.hashCode();
    }

}
