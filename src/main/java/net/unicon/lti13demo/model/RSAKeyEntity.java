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
package net.unicon.lti13demo.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "rsa_key")
public class RSAKeyEntity extends BaseEntity {
    @EmbeddedId
    private RSAKeyId kid;
    @Basic
    @Column(name = "public_key", length = 4096)
    private String publicKey;
    @Basic
    @Column(name = "private_key", length = 4096)
    private String privateKey;


    protected RSAKeyEntity() {
    }

    /**
     * @param kid  the key id
     * @param publicKey  the plain text public key
     * @param privateKey the plain text private key
     */
    public RSAKeyEntity(String kid, Boolean tool, String publicKey, String privateKey) {
        this.kid = new RSAKeyId(kid,tool);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public RSAKeyId getKid() {
        return kid;
    }

    public void setKid(RSAKeyId kid) {
        this.kid = kid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RSAKeyEntity that = (RSAKeyEntity) o;

        if (kid != that.kid) return false;
        if (publicKey != null ? !publicKey.equals(that.publicKey) : that.publicKey != null) return false;
        return privateKey != null ? privateKey.equals(that.privateKey) : that.privateKey == null;
    }

    @Override
    public int hashCode() {
        int result =  kid != null ? kid.hashCode() : 0;
        result = 31 * result + (publicKey != null ? publicKey.hashCode() : 0);
        result = 31 * result + (privateKey != null ? privateKey.hashCode() : 0);
        return result;
    }

}
