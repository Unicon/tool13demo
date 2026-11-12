package net.unicon.lti.model;

import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "api_one_use_token")
public class ApiOneUseToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long tokenId;

    @Column(nullable = false, length = 4096)
    private String token;

    protected ApiOneUseToken() {
    }

    /**
     * @param token the one use token
     */
    public ApiOneUseToken(String token) {
        if (!StringUtils.isNotBlank(token)) throw new AssertionError();
        this.token = token;
    }

    public long getTokenId() {
        return tokenId;
    }

    public void setTokenId(long tokenId) {
        this.tokenId = tokenId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiOneUseToken that = (ApiOneUseToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return 31 * (token != null ? token.hashCode() : 0);
    }

}
