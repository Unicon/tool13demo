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
package net.unicon.lti13demo.security;

import net.unicon.lti13demo.service.LTIDataService;
import net.unicon.lti13demo.service.LTIJWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.PostConstruct;

@Configuration
@EnableWebSecurity
@Import( SecurityAutoConfiguration.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    @Autowired
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public void configureSimpleAuthUsers(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("admin").password("{noop}admin").roles("ADMIN", "USER")
                .and().withUser("user").password("{noop}user").roles("USER");
    }

    @Order(10) // VERY HIGH
    @Configuration
    public static class OICDAuthConfigurationAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // this is open
            http.antMatcher("/oidc/**").authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();
        }
    }

    @Order(15) // VERY HIGH
    @Configuration
    public static class DynamicRegistrationConfigurationAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // this is open
            http.antMatcher("/registration/**").authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();
        }
    }

    @Order(20) // VERY HIGH
    @Configuration
    public static class JWKConfigurationAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // this is open
            http.antMatcher("/jwks/**").authorizeRequests().anyRequest().permitAll().and().csrf().disable().headers().frameOptions().disable();
        }
    }

    @Order(30) // VERY HIGH
    @Configuration
    public static class ConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        @Order(Ordered.HIGHEST_PRECEDENCE + 10)
        @SuppressWarnings("SpringJavaAutowiringInspection")
        public void configureSimpleAuthUsers(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    .withUser("admin").password("{noop}admin").roles("ADMIN", "USER")
                    .and().withUser("user").password("{noop}user").roles("USER");
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
            lti3oAuthProviderProcessingFilter = new LTI3OAuthProviderProcessingFilter(ltiDataService,ltijwtService);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            /**/
            http.requestMatchers().antMatchers("/lti3/**").and()
                    .addFilterBefore(lti3oAuthProviderProcessingFilter, UsernamePasswordAuthenticationFilter.class)
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
