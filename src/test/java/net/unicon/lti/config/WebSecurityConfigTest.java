package net.unicon.lti.config;

import jakarta.servlet.Filter;
import net.unicon.lti.security.app.APIOAuthProviderProcessingFilter;
import net.unicon.lti.security.lti.LTI3OAuthProviderProcessingFilter;
import net.unicon.lti.security.lti.LTI3OAuthProviderProcessingFilterStateNonceChecked;
import net.unicon.lti.service.app.APIDataService;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebSecurityConfigTest {

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private LTIJWTService ltijwtService;

    @Mock
    private APIJWTService apiJwtService;

    @Mock
    private APIDataService apiDataService;

    private WebSecurityConfig config;

    private GenericApplicationContext context;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new WebSecurityConfig(ltiDataService, ltijwtService, apiJwtService, apiDataService);
        config.adminUser = "admin";
        config.adminPassword = "admin";
        config.init();
        context = new GenericApplicationContext();
        context.registerBean(PathPatternRequestMatcher.Builder.class, PathPatternRequestMatcher::withDefaults);
        context.refresh();
    }

    @AfterEach
    public void tearDown() {
        context.close();
    }

    private HttpSecurity newHttpSecurity() throws Exception {
        AuthenticationManagerBuilder auth = new AuthenticationManagerBuilder(ObjectPostProcessor.identity());
        config.configureSimpleAuthUsers(auth);
        return new HttpSecurity(ObjectPostProcessor.identity(), auth, Map.of(ApplicationContext.class, context,
                PathPatternRequestMatcher.Builder.class, context.getBean(PathPatternRequestMatcher.Builder.class)));
    }

    private static MockHttpServletRequest requestTo(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    private static Set<Class<?>> filterTypes(SecurityFilterChain chain) {
        return chain.getFilters().stream().map(Object::getClass).collect(Collectors.toSet());
    }

    private static <T extends Filter> T findFilter(SecurityFilterChain chain, Class<T> type) {
        return chain.getFilters().stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    @Test
    public void testExplicitAdminPassword() throws Exception {
        config.adminUser = "boss";
        config.adminPassword = "s3cret";
        AuthenticationManagerBuilder auth = new AuthenticationManagerBuilder(ObjectPostProcessor.identity());

        config.configureSimpleAuthUsers(auth);

        AuthenticationManager manager = auth.build();
        Authentication authentication = manager.authenticate(new UsernamePasswordAuthenticationToken("boss", "s3cret"));
        assertTrue(authentication.isAuthenticated());
        UserDetails user = auth.getDefaultUserDetailsService().loadUserByUsername("boss");
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        assertTrue(encoder.matches("s3cret", user.getPassword()));
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_USER"),
                user.getAuthorities().stream().map(Object::toString).collect(Collectors.toSet()));
    }

    @Test
    public void testDefaultAdminPasswordIsReplacedByARandomOne() throws Exception {
        AuthenticationManagerBuilder auth = new AuthenticationManagerBuilder(ObjectPostProcessor.identity());

        config.configureSimpleAuthUsers(auth);

        AuthenticationManager manager = auth.build();
        assertThrows(BadCredentialsException.class,
                () -> manager.authenticate(new UsernamePasswordAuthenticationToken("admin", "admin")));
        UserDetails user = auth.getDefaultUserDetailsService().loadUserByUsername("admin");
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        assertFalse(encoder.matches("admin", user.getPassword()));
        assertEquals(2, user.getAuthorities().size());
    }

    @Test
    public void testConfigChainUsesBasicAuth() throws Exception {
        SecurityFilterChain chain = config.filterChain2(newHttpSecurity());

        assertTrue(chain.matches(requestTo("/config/1")));
        assertFalse(chain.matches(requestTo("/lti3/")));
        Set<Class<?>> types = filterTypes(chain);
        assertTrue(types.contains(BasicAuthenticationFilter.class));
        assertFalse(types.contains(CsrfFilter.class));
        assertFalse(types.contains(HeaderWriterFilter.class));
    }

    @Test
    public void testStateNonceCheckedChainUsesItsFilter() throws Exception {
        SecurityFilterChain chain = config.filterChain3(newHttpSecurity());

        assertTrue(chain.matches(requestTo("/lti3/stateNonceChecked")));
        assertFalse(chain.matches(requestTo("/lti3/")));
        assertNotNull(findFilter(chain, LTI3OAuthProviderProcessingFilterStateNonceChecked.class));
        assertFalse(filterTypes(chain).contains(CsrfFilter.class));
    }

    @Test
    public void testLti3ChainUsesItsFilter() throws Exception {
        SecurityFilterChain chain = config.filterChain4(newHttpSecurity());

        assertTrue(chain.matches(requestTo("/lti3/anything")));
        assertFalse(chain.matches(requestTo("/api/x")));
        assertNotNull(findFilter(chain, LTI3OAuthProviderProcessingFilter.class));
        assertFalse(filterTypes(chain).contains(CsrfFilter.class));
    }

    @Test
    public void testApiChainUsesCorsAndApiFilter() throws Exception {
        SecurityFilterChain chain = config.filterChain5(newHttpSecurity());

        assertTrue(chain.matches(requestTo("/api/x")));
        assertFalse(chain.matches(requestTo("/config/")));
        List<Filter> filters = chain.getFilters();
        int cors = filters.indexOf(findFilter(chain, CorsFilter.class));
        int api = filters.indexOf(findFilter(chain, APIOAuthProviderProcessingFilter.class));
        assertTrue(cors >= 0);
        assertTrue(api >= 0);
        assertFalse(filterTypes(chain).contains(CsrfFilter.class));
    }

    @Test
    public void testDefaultChainMatchesEverything() throws Exception {
        SecurityFilterChain chain = config.filterChain6(newHttpSecurity());

        assertTrue(chain instanceof DefaultSecurityFilterChain);
        assertTrue(chain.matches(requestTo("/oidc/login_initiations")));
        assertTrue(chain.matches(requestTo("/anything/else")));
        assertFalse(filterTypes(chain).contains(CsrfFilter.class));
        assertFalse(filterTypes(chain).contains(HeaderWriterFilter.class));
    }

    @Test
    public void testInitCreatesTheFiltersOnce() throws Exception {
        Filter first = findFilter(config.filterChain4(newHttpSecurity()), LTI3OAuthProviderProcessingFilter.class);
        Filter second = findFilter(config.filterChain4(newHttpSecurity()), LTI3OAuthProviderProcessingFilter.class);

        assertSame(first, second);
    }
}
