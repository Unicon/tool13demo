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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import net.unicon.lti13demo.model.RSAKeyEntity;
import net.unicon.lti13demo.service.LTIDataService;
import net.unicon.lti13demo.service.LTIJWTService;
import net.unicon.lti13demo.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.core.matching.Matcher;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.creator.OidcProfileCreator;
import org.pac4j.oidc.profile.creator.TokenValidator;
import org.pac4j.oidc.redirect.OidcRedirectActionBuilder;
import org.pac4j.springframework.security.profile.SpringSecurityProfileManager;
import org.pac4j.springframework.security.util.SpringSecurityHelper;
import org.pac4j.springframework.security.web.CallbackFilter;
import org.pac4j.springframework.security.web.SecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    // TODO replace this w/ something backed by our `LtiUserRepository`.
    @Bean
    public UserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager(ImmutableList.of(
                User
                        .withUsername("admin")
                        .password("admin")
                        .roles("ADMIN", "USER")
                        .build(),
                User
                        .withUsername("user")
                        .password("user")
                        .roles("USER")
                        .build()

        ));
    }

    @Order(2) // HIGHER YET
    @Configuration
    // JavaConfig take on https://github.com/pac4j/spring-security-pac4j-demo/blob/master/src/main/resources/securityContext.xml
    public static class LTI3OidcAuthConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private LTIDataService ltiDataService;

        @Autowired
        private UserDetailsManager userDetailsManager;

        @Value("https://${server.name}:${server.port}/${server.servlet.context-path:}")
        private String baseUrl;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // this is open
            http.antMatcher("/oauth2/oidc/lti/**")
                    .addFilterBefore(newLti3OidcCallbackFilter(), BasicAuthenticationFilter.class)
                    .addFilterBefore(newLti3OidcSecurityFilter(), BasicAuthenticationFilter.class)
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                    .and()
                        .csrf()
                            .disable()
                        .headers()
                            .frameOptions()
                                .disable();

        }

        // You do not want your Servlet Filters to be Beans, else they'll be added to request handling chains twice,
        // once here (and on the path we actually want) and once by ServletContextInitializingBeans.addAdaptableBeans()
        // (to a root path we don't want). (If you _do_ need these filters to be Beans, but still want control over
        // where they're injected into the chain, define them as FilterRegistrationBean)
        private SecurityFilter newLti3OidcSecurityFilter() {
            SecurityFilter securityFilter = new SecurityFilter(
                    lti3OidcConfig(),
                    "Pac4jOidcClient" // TODO having to know list of client names at app startup is potentially a problem long-term since a real LTI app will grow that list at runtime
            );
            securityFilter.setSecurityLogic(lti3OidcSecurityLogic());
            return securityFilter;
        }

        @Bean
        public SecurityLogic<Object, J2EContext> lti3OidcSecurityLogic() {
            return new DefaultSecurityLogic<Object, J2EContext>() {
                @Override
                protected void saveRequestedUrl(final J2EContext context, final List<Client> currentClients) {
                    String targetLinkUri = context.getRequestParameter("target_link_uri");
                    context.getSessionStore().set(context, Pac4jConstants.REQUESTED_URL, targetLinkUri);
                }
            };
        }

        private CallbackFilter newLti3OidcCallbackFilter() {
            CallbackFilter callbackFilter = new CallbackFilter(lti3OidcConfig()) {
                // Special requestPathMatcher only needed if Client names are in the path rather than as a query param,
                // which we're currently doing to try to rule out problems with query param encoding on the Platform
                // side. Either way, there's a potential problem if the Platform expects return URLs to be known
                // statically per-Tool, no matter how many times that Tool is registered with the platform (which may
                // the case, for example, if Tools are expected to publish static registration metadata as was the case
                // in LTI 1.1)
                private final Matcher requestPathMatcher = new Matcher() {
                    private final AntPathMatcher antMatcher = new AntPathMatcher("/");

                    @Override
                    public boolean matches(WebContext context) {
                        return antMatcher.match("/oauth2/oidc/lti/authorization/**", context.getPath());
                    }
                };


                @Override
                protected boolean mustApply(final J2EContext context) {
                    return requestPathMatcher.matches(context);
                }
            };
            callbackFilter.setSuffix("/oauth2/oidc/lti/authorization");
            return callbackFilter;
        }

        @Bean
        public Config lti3OidcConfig() {
            Config config = new Config(
                    lti3OidcClients()
            );
            config.setProfileManagerFactory(request -> new LTI3Pac4jSpringSecurityProfileManager(request, userDetailsManager));
            return config;
        }

        // TODO to be truly reusable as a lib, this needs to be abstracted further to support
        //  direct integration with any user repo, with SpringSecurity's UserDetailsManager being
        //  just one option, i.e. this would become a concrete impl of a more generic
        //  LTI3Pac4jProfileManager. **Also NB hooks need to be added somewhere for context
        //  initialization, which is somewhat strongly related to other LTI Advantage callbacks,
        //  most notably NRPS, so that's being delayed until we figure out where LTI-A slots in.
        private class LTI3Pac4jSpringSecurityProfileManager extends SpringSecurityProfileManager {

            private final UserDetailsManager userDetailsManager;

            public LTI3Pac4jSpringSecurityProfileManager(WebContext context,
                                                         UserDetailsManager userDetailsManager) {
                super(context);
                this.userDetailsManager = userDetailsManager;
            }

            @Override
            public void save(final boolean saveInSession, final CommonProfile profile, final boolean multiProfile) {
                Optional<UserDetails> existingUserDetails = findUserDetails(profile);
                if (existingUserDetails.isPresent()) {
                    merge(profile, existingUserDetails.get());
                } else {
                    create(profile);
                }

                // Write into the session at the end so we don't try to edit the profile once its already been placed
                // into the session (problematic for distributed session stores).
                // TODO a real-world implementation will likely need more control over the specific authentication token
                //  type placed into the spring security context here -> in the end we probably can't extend
                //  SpringSecurityProfileManager and will have to clone and modify SpringSecurityHelper
                super.save(saveInSession, profile, multiProfile);
            }

            protected String buildUserDetailsKey(CommonProfile profile) {
                // TODO UserDetailsService/Manager only supports lookup by `username`. This is tough since LTI launches
                //   are not going to contain usernames per-se, so we go with username==email, but this is not a good
                //   solution b/c "private" launches w/o email addrs should be considered commonplace. So the only
                //   way a UserDetailsManager-backed impl would actually work would be to either use LTI IDs as
                //   usernames (not plausible for a 'real' Tool) or depend on an extension of that interface that
                //   takes more complex predicates. Maybe check to see what SpringSecurity OIDC mainline is doing here.
                //
                // TODO Also the persistence of grants doesn't make a whole lot of sense here. This is b/c at least a
                //   subset of the LTI roles really only apply to the context being launched. So a real-world
                //   implementation would probably need to:
                //   a) create and store the user in some tenant-specific scope,
                //   b) store some tenant-level roles (LTI "system" and "institution" roles) in that same scope,
                //   c) store context-level roles in some content-specific scope, i.e. class enrollments
                return profile.getEmail();
            }

            protected Optional<UserDetails> findUserDetails(CommonProfile profile) {
                try {
                    String userDetailsKey = buildUserDetailsKey(profile);
                    if (StringUtils.isBlank(userDetailsKey)) {
                        throw new IllegalArgumentException("Blank user lookup key for profile ID ["
                                + profile.getId() + " / " + profile.getTypedId() + "]");
                    }
                    return Optional.ofNullable(userDetailsManager.loadUserByUsername(userDetailsKey));
                } catch (UsernameNotFoundException e) {
                    return Optional.empty();
                }
            }

            protected void merge(CommonProfile into, UserDetails from) {
                // A more complex impl might have additional data stored on the UserDetails that needs to be
                // exposed in the profile, e.g. the locked, expired, etc fields might be of interest. We don't, at
                // least for now, since we don't know where those properties would be enforced.
                return;
            }

            private void create(CommonProfile profile) {
                userDetailsManager.createUser(asUserDetails(profile));
            }

            private UserDetails asUserDetails(CommonProfile profile) {
                return User
                        .withUsername(buildUserDetailsKey(profile))
                        .password(new Identifier().getValue())
                        .authorities(SpringSecurityHelper.buildAuthorities(ImmutableList.of(profile)))
                        .build();
            }
        }

        @Bean
        public Clients lti3OidcClients() {
            // TODO this might need to change to be more dynamic since in the Real World, a new Client could be
            //  added at any time and we don't want to bounce the app to pick up such a change. There's a kinda-sorta
            //  example of what this could look like at https://github.com/jkacer/pac4j-extensions. (Still requires
            //  reload, but at least demos what a re-implementation of Clients entails.
            return new Clients(
                    baseUrl + "oauth2/oidc/lti/authorization",
                    newLti3OidcClient("Pac4jOidcClient", lti3OidcConfiguration())
            );
        }

        private Client newLti3OidcClient(String name, OidcConfiguration config) {
            OidcClient client = new OidcClient<>(config);
            client.setName(name);
            client.setRedirectActionBuilder(new OidcRedirectActionBuilder(client.getConfiguration(), client) {
                @Override
                protected void addStateAndNonceParameters(final WebContext context, final Map<String, String> params) {
                    params.put("login_hint", context.getRequestParameter("login_hint"));
                    params.put("lti_message_hint", context.getRequestParameter("lti_message_hint"));
                    params.put("target_link_uri", context.getRequestParameter("target_link_uri"));
                    super.addStateAndNonceParameters(context, params);
                }
            });
            client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
            client.setProfileCreator(new OidcProfileCreator<OidcProfile>(config) {
                @Override
                protected void internalInit() {
                    tokenValidator = new TokenValidator(configuration) {
                        @Override
                        protected IDTokenValidator createRSATokenValidator(final OidcConfiguration configuration,
                                                                           final JWSAlgorithm jwsAlgorithm, final ClientID clientID) {
                            return new IDTokenValidator(
                                    configuration.findProviderMetadata().getIssuer(),
                                    clientID,
                                    jwsAlgorithm,
                                    findKeySet(configuration.findProviderMetadata().getIssuer(), clientID));
                        }

                        private JWKSet findKeySet(Issuer issuer, ClientID clientId) {
                            // TODO change this to only return keys associated directly with a specific client
                            List<RSAKeyEntity> domainKeys = ltiDataService.getRepos().rsaKeys.findAll();
                            List<JWK> joseKeys = domainKeys.stream()
                                    .map(domainKey -> {
                                        try {
                                            return Pair.of(
                                                    domainKey.getKid(),
                                                    OAuthUtils.loadPublicKey(domainKey.getPublicKey())
                                            );
                                        } catch (Exception e) {
                                            logger.warn("Failed to deserialize public key [" + domainKey.getKid() + "]", e);
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .map(jdkKidAndKey -> new RSAKey.Builder(jdkKidAndKey.getRight())
                                            .keyID(jdkKidAndKey.getLeft().getKid()).build())
                                    .collect(Collectors.toList());
                            return new JWKSet(joseKeys);
                        }
                    };

                    super.internalInit();
                }
            });
            client.setAuthorizationGenerator(lti3OidcAuthorizationGenerator());

            return client;
        }

        @Bean
        public AuthorizationGenerator lti3OidcAuthorizationGenerator() {
            // TODO extract to external class
            return new AuthorizationGenerator() {

                // TODO can be made static once not defined on an inner class
                // TODO https://www.imsglobal.org/spec/lti/v1p3#context-sub-roles, esp TA
                // TODO make use of `LTIStrings`
                private final Map<String,Set<String>> APP_ROLES_BY_LTI_ROLE =
                        ImmutableMap.<String,Set<String>>builder()
                                .put("http://purl.imsglobal.org/vocab/lis/v2/system/person#Administrator", ImmutableSet.of("ROLE_ADMIN"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/system/person#None", ImmutableSet.of("ROLE_USER"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Administrator", ImmutableSet.of("ROLE_ADMIN"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Guest", ImmutableSet.of("ROLE_USER"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/institution/person#None", ImmutableSet.of("ROLE_USER"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Other", ImmutableSet.of("ROLE_USER"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Staff", ImmutableSet.of("ROLE_STAFF"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/institution/person#Student", ImmutableSet.of("ROLE_STUDENT"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/membership#Administrator", ImmutableSet.of("ROLE_ADMIN"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/membership#ContentDeveloper", ImmutableSet.of("ROLE_AUTHOR"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor", ImmutableSet.of("ROLE_INSTRUCTOR"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/membership#Learner", ImmutableSet.of("ROLE_STUDENT"))
                                .put("http://purl.imsglobal.org/vocab/lis/v2/membership#Mentor", ImmutableSet.of("ROLE_OBSERVER"))
                                .build();

                @Override
                public CommonProfile generate(WebContext context, CommonProfile profile) {
                    @SuppressWarnings("unchecked")
                    List<String> ltiRoles = (List<String>)profile.getAttribute("https://purl.imsglobal.org/spec/lti/claim/roles");
                    if (!(CollectionUtils.isEmpty(ltiRoles))) {
                        ltiRoles
                                .stream()
                                .map(ltiRole -> mapLtiRoleToApplicationRole(ltiRole, context, profile))
                                .flatMap(Set::stream)
                                .map(String::toUpperCase)
                                .forEach(profile::addRole);
                        ltiRoles.add("LTI");
                    }
                    return profile;
                }

                // TODO This mapping is naive. At a minimum, real world use would require control over
                //   `APP_ROLES_BY_LTI_ROLE` for reasons listed there. e.g. just b/c you're a system or institution admin
                //   doesn't mean you should automatically become an admin in whatever LTI context you launched into.
                protected Set<String> mapLtiRoleToApplicationRole(String ltiRole, WebContext context, CommonProfile profile) {
                    return APP_ROLES_BY_LTI_ROLE.getOrDefault(ltiRole, ImmutableSet.of());
                }
            };
        }

        // TODO this needs to be per-Client, so will need to change to not be a bean (will probably be encapsulated
        //   behind a custom Clients impl that reads Clients from the db).
        @Bean
        public OidcConfiguration lti3OidcConfiguration() {
            OidcConfiguration oidcConfiguration = new OidcConfiguration();

            oidcConfiguration.setScope(OIDCScopeValue.OPENID.getValue());
//            oidcConfiguration.setClientId("dmccallum-platform-2-client-1"); // TODO clear example of why this needs to be more dynamic
            oidcConfiguration.setClientId("dmccallum-local-platform-2-client-1"); // TODO clear example of why this needs to be more dynamic

            OIDCProviderMetadata oidcProviderMetadata = new OIDCProviderMetadata(
//                    new Issuer("https://dmp2-lti-ri.imsglobal.org"), // TODO clear example of why this needs to be more dynamic
                    new Issuer("http://localhost:3000"), // TODO clear example of why this needs to be more dynamic
                    ImmutableList.of(SubjectType.PUBLIC), // TODO not sure if this is right
                    URI.create("https://oauth2server.imsglobal.org/jwks") // TODO value definitely wrong... don't think IMS RI hosts its keys at a JWKS URL
            );
//            oidcProviderMetadata.setAuthorizationEndpointURI(URI.create("https://lti-ri.imsglobal.org/platforms/110/authorizations/new"));
            oidcProviderMetadata.setAuthorizationEndpointURI(URI.create("http://localhost:3000/platforms/2/authorizations/new")); // TODO source from db
            oidcProviderMetadata.setIDTokenJWSAlgs(ImmutableList.of(JWSAlgorithm.RS256));
            oidcConfiguration.setProviderMetadata(oidcProviderMetadata);

            oidcConfiguration.setUseNonce(true); // TODO IMS-RI seems to always generate its own nonces
//            oidcConfiguration.setPreferredJwsAlgorithm(JWSAlgorithm.RS256); // TODO unsure if this is needed

            oidcConfiguration.setClientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC); // TODO this is not right but is the only way to get OidcAuthenticator to work... we'll need to provide our own impl of that
            oidcConfiguration.setSecret("thisiswrong"); // TODO also not right, same reason as setClientAuthenticationMethod()

            // TODO we'll also need to add in:
            //  - a StateGenerator
            //  - probably some CustomParams
            //  - probably a ProfileCreator
            //  Also don't know how nonce's are being validated and, more generally, how this works in a multi-server deployment

            // Enable Implicit flow
            oidcConfiguration.setResponseType("id_token");
            oidcConfiguration.setResponseMode("form_post");

            return oidcConfiguration;
        }

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
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/console/**");
        }

//        @Override
//        protected void configure(HttpSecurity http) throws Exception {
//            // this ensures security context info (Principal, sec:authorize, etc.) is accessible on all paths
//            http.antMatcher("/**").authorizeRequests().anyRequest().permitAll().and().headers().frameOptions().disable();
//        }
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests()
                        .antMatchers("/").permitAll()
                        .anyRequest().authenticated()
                    .and()
                        .formLogin()
                        .disable()
                    .httpBasic()
                        .disable()
                    .logout()
                        .logoutSuccessUrl("/");
        }
    }

}
