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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "lti_user")
public class LtiUserEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private long userId;
    @Basic
    @Column(name = "user_key", nullable = false, length = 4096)
    private String userKey;
    @Basic
    @Column(name = "lms_user_id")
    private String lmsUserId;
    @Basic
    @Column(name = "displayname", length = 4096)
    private String displayName;
    /**
     * Actual max for emails is 254 chars
     */
    @Basic
    @Column(name = "email")
    private String email;
    @Basic
    @Column(name = "locale", length = 63)
    private String locale;
    @Basic
    @Column(name = "subscribe")
    private Short subscribe;
    @Lob
    @Column(name = "json")
    private String json;
    @Basic
    @Column(name = "login_at")
    private Timestamp loginAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<LtiResultEntity> results;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id", referencedColumnName = "key_id")
    private PlatformDeployment platformDeployment;

    protected LtiUserEntity() {
    }

    /**
     * @param userKey user identifier
     * @param loginAt date of user login
     */
    public LtiUserEntity(String userKey, Date loginAt, PlatformDeployment platformDeployment1) {
        if (!StringUtils.isNotBlank(userKey)) throw new AssertionError();
        if (loginAt == null) {
            loginAt = new Date();
        }
        if (platformDeployment1 != null) {
            this.platformDeployment = platformDeployment1;
        }
        this.userKey = userKey;
        this.loginAt = new Timestamp(loginAt.getTime());
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLmsUserId() {
        return lmsUserId;
    }

    public void setLmsUserId(String lmsUserId) {
        this.lmsUserId = lmsUserId;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Short getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Short subscribe) {
        this.subscribe = subscribe;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Timestamp getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(Timestamp loginAt) {
        this.loginAt = loginAt;
    }

    public Set<LtiResultEntity> getResults() {
        return results;
    }

    public void setResults(Set<LtiResultEntity> results) {
        this.results = results;
    }

    public PlatformDeployment getPlatformDeployment() {
        return platformDeployment;
    }

    public void setPlatformDeployment(PlatformDeployment platformDeployment) {
        this.platformDeployment = platformDeployment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LtiUserEntity that = (LtiUserEntity) o;

        if (userId != that.userId) return false;
        if (!Objects.equals(email, that.email)) return false;
        return Objects.equals(userKey, that.userKey);
    }

    @Override
    public int hashCode() {
        int result = (int) userId;
        result = 31 * result + (userKey != null ? userKey.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }

}
