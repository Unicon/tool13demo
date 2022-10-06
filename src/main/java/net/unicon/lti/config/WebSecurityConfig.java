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
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@Import(SecurityAutoConfiguration.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    @Order(10) // VERY HIGH
    @Configuration
    public static class OpenEndpointsConfigurationAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // this is open
            http.requestMatchers()
                .antMatchers("/oidc/**")
                .antMatchers("/registration/**")
                .antMatchers("/jwks/**")
                .antMatchers("/ags/**")
                .antMatchers("/demo/**")
                .antMatchers("/harmony/**")
                .antMatchers("/valkyrie/**")
                .and()
                .authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();
        }
    }

    @Order(30) // VERY HIGH
    @Configuration
    public static class ConfigConfigurationAdapter extends WebSecurityConfigurerAdapter {

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

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/config/**").authorizeRequests().anyRequest().authenticated().and().httpBasic().and().csrf().disable().headers().frameOptions().disable();
        }
    }

    @Configuration
    @Order(40) // HIGH
    public static class LTI3SecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        private LTI3OAuthProviderProcessingFilter lti3oAuthProviderProcessingFilter;
        @Autowired
        LTIDataService ltiDataService;
        @Autowired
        LTIJWTService ltijwtService;

        @PostConstruct
        public void init() {
            lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService, ltijwtService);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            /**/
            http.requestMatchers().antMatchers("/lti3/").antMatchers("/context/**").and()
                    .addFilterBefore(lti3oAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();
        }
    }

    @Configuration
    @Order(50) // HIGH
    public static class APISecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
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
        @Override
        public void configure(AuthenticationManagerBuilder builder) throws Exception {
            builder.authenticationProvider(jwtAuthenticationProvider);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.requestMatchers()
                    .antMatchers("/api/**")
                    .and()
                    .addFilterBefore(new CorsFilter(new CorsConfigurationSourceImpl()), BasicAuthenticationFilter.class)
                    .addFilterBefore(apioAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();
        }
    }


    @Order(80) // LOWEST
    @Configuration
    public static class NoAuthConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
            http.antMatcher("/**").authorizeRequests().anyRequest().permitAll().and().headers().frameOptions().disable();
        }
    }



}
