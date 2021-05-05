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
package net.unicon.lti.security.app;

import com.google.common.collect.ImmutableSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.unicon.lti.model.oauth2.JwtAuthenticationToken;
import net.unicon.lti.service.app.APIJWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    @Autowired private APIJWTService jwtService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtAuthenticationToken jwtAuthentication = (JwtAuthenticationToken) authentication;
        String jwtValue = jwtAuthentication.getToken();
        try {
            Jws<Claims> jwtClaims = jwtService.validateToken(jwtValue);
            return new JwtAuthenticationToken(
                    jwtValue, jwtClaims.getBody().getSubject(), extractGrantedAuthorities(jwtClaims.getBody()), jwtClaims.getBody());
        } catch (JwtException e) {
            throw new BadCredentialsException("Failed to authenticate JWT", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractGrantedAuthorities(Claims jwtClaims) {
        List<String> authorityStrings = jwtClaims.get("roles", List.class);
        if (CollectionUtils.isEmpty(authorityStrings)) {
            return ImmutableSet.of();
        }
        return authorityStrings.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

