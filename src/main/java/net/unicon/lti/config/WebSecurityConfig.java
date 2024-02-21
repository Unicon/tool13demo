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
import net.unicon.lti.security.lti.LTI3OAuthProviderProcessingFilterStateNonceChecked;
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Import(SecurityAutoConfiguration.class)
public class WebSecurityConfig {


    @Order(10) // VERY HIGH
    public static class OpenEndpointsConfigurationAdapter {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            // this is open
            return http.authorizeRequests(authz ->
               authz
                  .requestMatchers("/oidc/**", "/registration/**", "/jwks/**", "/deeplink/**", "/ags/**").permitAll()
                  .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable()).build();
        }
    }


    @Order(30) // VERY HIGH
    @Configuration
    public static class ConfigConfigurationAdapter {

        static final Logger log = LoggerFactory.getLogger(ConfigConfigurationAdapter.class);

        @Value("${terracotta.admin.user:admin}")
        String adminUser;

        @Value("${terracotta.admin.password:admin}")
        String adminPassword;


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

        @Bean
        public SecurityFilterChain filterChain2(HttpSecurity http) throws Exception {
            return http.authorizeRequests(authz ->
                    authz
                            .requestMatchers("/config/**").authenticated()
                            .anyRequest().authenticated()
            ).httpBasic(withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(frameOptions -> frameOptions.disable()).build();
        }
    }

    @Configuration
    @Order(35) // HIGH
    public static class LTI3SecurityConfigurerAdapterAfter {
        private LTI3OAuthProviderProcessingFilterAfter lti3oAuthProviderProcessingFilter;
        @Autowired
        LTIDataService ltiDataService;
        @Autowired
        LTIJWTService ltijwtService;

        @PostConstruct
        public void init() {
            lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilterStateNonceChecked(ltiDataService, ltijwtService);
        }

        @Bean
        public SecurityFilterChain filterChain3(HttpSecurity http) throws Exception {
            /**/
            return http.authorizeRequests(authz ->
                    authz
                            .requestMatchers("/lti3/after").permitAll()
                            .anyRequest().permitAll())
                    .addFilterAfter(lti3oAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                    .csrf(csrf -> csrf.disable())
                    .headers(frameOptions -> frameOptions.disable())
                    .build();
        }
    }

    @Configuration
    @Order(40) // HIGH
    public static class LTI3SecurityConfigurerAdapterCheck {
        private LTI3OAuthProviderProcessingFilter lti3oAuthProviderProcessingFilter;
        @Autowired
        LTIDataService ltiDataService;
        @Autowired
        LTIJWTService ltijwtService;

        @PostConstruct
        public void init() {
            lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
        }

        @Bean
        public SecurityFilterChain filterChain4(HttpSecurity http) throws Exception {
            /**/
            return http.authorizeRequests(authz ->
                            authz
                                    .requestMatchers("/lti3/**").permitAll()
                                    .anyRequest().permitAll())
                    .addFilterBefore(lti3oAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                    .csrf(csrf -> csrf.disable())
                    .headers(frameOptions -> frameOptions.disable())
                    .build();
        }
    }

    @Configuration
    @Order(50) // HIGH
    public static class APISecurityConfigurerAdapter {
        private APIOAuthProviderProcessingFilter apioAuthProviderProcessingFilter;
        @Autowired
        APIJWTService apiJwtService;

        @Autowired
        APIDataService apiDataService;

        @Autowired
        private JwtAuthenticationProvider jwtAuthenticationProvider;

        @PostConstruct
        public void init() {
            apioAuthProviderProcessingFilter = new APIOAuthProviderProcessingFilter(apiJwtService, apiDataService);
        }

        //TODO: this is never called. Modify it to allow this authentication provider
        //to work for these 2 matchers in a way that it sets the roles.
        @Bean
        public AuthenticationManager authManager(HttpSecurity http) throws Exception {
            AuthenticationManagerBuilder authenticationManagerBuilder =
                    http.getSharedObject(AuthenticationManagerBuilder.class);
            authenticationManagerBuilder.authenticationProvider(jwtAuthenticationProvider);
            return authenticationManagerBuilder.build();
        }

        @Bean
        public SecurityFilterChain filterChain5(HttpSecurity http) throws Exception {
            return http.authorizeRequests(authz ->
                    authz
                            .requestMatchers("/api/**").permitAll()
                            .anyRequest().permitAll())
                    .addFilterBefore(new CorsFilter(new CorsConfigurationSourceImpl()), BasicAuthenticationFilter.class)
                    .addFilterBefore(apioAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                    .csrf(csrf -> csrf.disable())
                    .headers(frameOptions -> frameOptions.disable())
                    .build();
        }
    }


    @Order(80) // LOWEST
    @Configuration
    public static class NoAuthConfigurationAdapter {

        @Bean
        public SecurityFilterChain filterChain6(HttpSecurity http) throws Exception {
            // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
            return http.authorizeRequests(authz ->
                    authz
                            .requestMatchers("/**").permitAll()
                            .anyRequest().permitAll())
                            .csrf(csrf -> csrf.disable())
                            .headers(frameOptions -> frameOptions.disable())
                            .build();
        }
    }

}
