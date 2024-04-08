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

import net.unicon.lti.model.BaseEntity;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "nonce_state")
public class NonceState extends BaseEntity {

    @Id
    @Column(name = "nonce", nullable = false)
    private String nonce;

    @Column(name = "state_hash", nullable = false)
    private String stateHash;


    @Column(name = "state", nullable = false, length = 4096)
    private String state;

    @Column(name = "lti_storage_target")
    private String ltiStorageTarget;


    protected NonceState() {
    }

    /**
     * @param nonce the nonce
     * @param stateHash the state_hash
     * @param state the state
     */
    public NonceState(String nonce, String stateHash, String state, String ltiStorageTarget) {
        if (StringUtils.isBlank(nonce) || StringUtils.isBlank(stateHash) || StringUtils.isBlank(state)) throw new AssertionError();
        this.nonce = nonce;
        this.stateHash = stateHash;
        this.state = state;
        this.ltiStorageTarget = ltiStorageTarget;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getStateHash() {
        return stateHash;
    }

    public void setStateHash(String stateHash) {
        this.stateHash = stateHash;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLtiStorageTarget() {
        return ltiStorageTarget;
    }

    public void setLtiStorageTarget(String ltiStorageTarget) {
        this.ltiStorageTarget = ltiStorageTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NonceState that = (NonceState) o;
        return Objects.equals(nonce, that.nonce) &&
                Objects.equals(stateHash, that.stateHash) &&
                Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return 31 * (nonce != null ? nonce.hashCode() : 0);
    }

}
