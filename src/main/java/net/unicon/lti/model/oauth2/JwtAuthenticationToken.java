package net.unicon.lti.model.oauth2;

import org.springframework.security.core.authority.AuthorityUtils;
import io.jsonwebtoken.Claims;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private String token;
    private String principal;
    private Claims claims;

    public JwtAuthenticationToken(String token) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.token = token;
        setAuthenticated(false);
    }

    public JwtAuthenticationToken(
            String token,
            String principal,
            Collection<? extends GrantedAuthority> authorities,
            Claims claims) {
        super(authorities);
        this.token = token;
        this.principal = principal;
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.token;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public String getToken() {
        return token;
    }

    public Claims getClaims() {
        return claims;
    }
}

