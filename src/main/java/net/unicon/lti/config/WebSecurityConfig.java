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
package net.unicon.lti.config;

import net.unicon.lti.security.app.APIOAuthProviderProcessingFilter;
import net.unicon.lti.security.app.JwtAuthenticationProvider;
import net.unicon.lti.security.lti.LTI3OAuthProviderProcessingFilter;
import net.unicon.lti.security.lti.LTI3OAuthProviderProcessingFilterAfter;
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.annotation.PostConstruct;

import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${terracotta.admin.user:admin}")
    String adminUser;

    @Value("${terracotta.admin.password:admin}")
    String adminPassword;

    private LTI3OAuthProviderProcessingFilterAfter lti3oAuthProviderProcessingFilterAfter;
    private LTI3OAuthProviderProcessingFilter lti3oAuthProviderProcessingFilter;
    private APIOAuthProviderProcessingFilter apioAuthProviderProcessingFilter;
    @Autowired
    LTIDataService ltiDataService;
    @Autowired
    LTIJWTService ltijwtService;
    @Autowired
    APIJWTService apiJwtService;
    @Autowired
    APIDataService apiDataService;
    @Autowired
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

    @PostConstruct
    public void init() {
        lti3oAuthProviderProcessingFilterAfter = new LTI3OAuthProviderProcessingFilterAfter(ltiDataService, ltijwtService);
        lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
        apioAuthProviderProcessingFilter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService);
    }

    @Autowired
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public void configureSimpleAuthUsers(AuthenticationManagerBuilder auth) throws Exception {
        PasswordEncoder encoder =
                PasswordEncoderFactories.createDelegatingPasswordEncoder();

        if (!adminPassword.equals("admin")) {
            auth.inMemoryAuthentication()
                    .withUser(adminUser).password(encoder.encode(adminPassword)).roles("ADMIN", "USER");
        } else {
            String adminRandomPwd = UUID.randomUUID().toString();
            log.warn("Admin password not specified, please add one to the application properties file and restart the application." +
                    " Meanwhile, you can use this one (only valid until the next restart): " + adminRandomPwd);
            auth.inMemoryAuthentication()
                    .withUser(adminUser).password(encoder.encode(adminRandomPwd)).roles("ADMIN", "USER");
        }
    }

    @Order(30) // VERY HIGH
    @Bean
    public SecurityFilterChain filterChain2(HttpSecurity http) throws Exception {
        http.securityMatcher("/config/**");
        return http.authorizeHttpRequests(authz -> authz
                        .requestMatchers("/config/**").authenticated()

                )
                .httpBasic(withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(frameOptions -> frameOptions.disable())
                .build();
    }

    @Order(35) // HIGH
    @Bean
    public SecurityFilterChain filterChain3(HttpSecurity http) throws Exception {
        http.securityMatcher("/lti3/after");
        return http.authorizeHttpRequests(authz -> authz
                        .requestMatchers("/lti3/after").permitAll()
                )
                .addFilterAfter(lti3oAuthProviderProcessingFilterAfter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .headers(frameOptions -> frameOptions.disable())
                .build();
    }


    @Order(40) // HIGH
    @Bean
    public SecurityFilterChain filterChain4(HttpSecurity http) throws Exception {
        http.securityMatcher("/lti3/**");
        return http.authorizeHttpRequests(authz -> authz
                        .requestMatchers("/lti3/**").permitAll()
                )
                .addFilterBefore(lti3oAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .headers(frameOptions -> frameOptions.disable())
                .build();
    }


    @Order(70)
    @Bean
    public SecurityFilterChain filterChain5(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**");
        return http.authorizeHttpRequests(authz ->
                        authz
                                .requestMatchers("/api/**").permitAll()
                )
                .addFilterBefore(new CorsFilter(new CorsConfigurationSourceImpl()), BasicAuthenticationFilter.class)
                .addFilterBefore(apioAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .headers(frameOptions -> frameOptions.disable())
                .build();
    }


    @Order(80) // LOWEST
    @Bean
    public SecurityFilterChain filterChain6(HttpSecurity http) throws Exception {
        // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
        return http.authorizeHttpRequests(authz ->
                        authz
                                .requestMatchers("/oidc/**", "/registration/**", "/jwks/**", "/deeplink/**", "/ags/**").permitAll()
                                .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .headers(frameOptions -> frameOptions.disable())
                .build();
    }

}
