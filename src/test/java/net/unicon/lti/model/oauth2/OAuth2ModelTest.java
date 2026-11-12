package net.unicon.lti.model.oauth2;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.utils.LtiStrings;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OAuth2ModelTest {

    @Test
    public void testUnauthenticatedJwtToken() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("raw.jwt.value");

        assertEquals("raw.jwt.value", token.getToken());
        assertEquals("raw.jwt.value", token.getCredentials());
        assertNull(token.getPrincipal());
        assertNull(token.getClaims());
        assertFalse(token.isAuthenticated());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    public void testAuthenticatedJwtToken() {
        Claims claims = Jwts.claims().subject("user-1").build();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_X"));

        JwtAuthenticationToken token = new JwtAuthenticationToken("raw", "user-1", authorities, claims);

        assertEquals("raw", token.getToken());
        assertEquals("raw", token.getCredentials());
        assertEquals("user-1", token.getPrincipal());
        assertSame(claims, token.getClaims());
        assertTrue(token.isAuthenticated());
        assertEquals(authorities, List.copyOf(token.getAuthorities()));
    }

    @Test
    public void testLtiTokenConstructorAndAccessors() {
        LTIToken token = new LTIToken("access", "Bearer", 3600, "scope-a");
        assertEquals("access", token.getAccess_token());
        assertEquals("Bearer", token.getToken_type());
        assertEquals(3600, token.getExpires_in());
        assertEquals("scope-a", token.getScope());

        LTIToken empty = new LTIToken();
        empty.setAccess_token("a2");
        empty.setToken_type("t2");
        empty.setExpires_in(10);
        empty.setScope("s2");
        assertEquals("a2", empty.getAccess_token());
        assertEquals("t2", empty.getToken_type());
        assertEquals(10, empty.getExpires_in());
        assertEquals("s2", empty.getScope());
    }

    @Test
    public void testRolesConstants() {
        assertEquals(LtiStrings.LTI_ROLE_GENERAL, Roles.GENERAL);
        assertEquals(List.of(Roles.GENERAL), Roles.GENERAL_ROLE_LIST);
        assertEquals(List.of(LtiStrings.LTI_ROLE_LEARNER, LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER), Roles.STUDENT_ROLE_LIST);
        assertEquals(List.of(LtiStrings.LTI_ROLE_INSTRUCTOR, LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR), Roles.INSTRUCTOR_ROLE_LIST);
        assertEquals(List.of(LtiStrings.LTI_ROLE_ADMIN), Roles.ADMIN_ROLE_LIST);
        assertThrows(UnsupportedOperationException.class, () -> Roles.ADMIN_ROLE_LIST.add("x"));
        assertNotNull(new Roles() { });
    }

    @Test
    public void testSecuredInfoAccessors() {
        SecuredInfo info = new SecuredInfo();
        List<String> roles = List.of("r");
        Timestamp due = new Timestamp(1L);
        Timestamp lock = new Timestamp(2L);
        Timestamp unlock = new Timestamp(3L);

        info.setPlatformDeploymentId(1L);
        info.setContextId(2L);
        info.setContextKey("ctx");
        info.setUserId("user");
        info.setRoles(roles);
        info.setCanvasUserId("cu");
        info.setCanvasUserGlobalId("cug");
        info.setCanvasLoginId("login");
        info.setCanvasUserName("name");
        info.setCanvasCourseId("course");
        info.setCanvasAssignmentId("assignment");
        info.setDueAt(due);
        info.setLockAt(lock);
        info.setUnlockAt(unlock);
        info.setNonce("nonce");

        assertEquals(1L, info.getPlatformDeploymentId());
        assertEquals(2L, info.getContextId());
        assertEquals("ctx", info.getContextKey());
        assertEquals("user", info.getUserId());
        assertSame(roles, info.getRoles());
        assertEquals("cu", info.getCanvasUserId());
        assertEquals("cug", info.getCanvasUserGlobalId());
        assertEquals("login", info.getCanvasLoginId());
        assertEquals("name", info.getCanvasUserName());
        assertEquals("course", info.getCanvasCourseId());
        assertEquals("assignment", info.getCanvasAssignmentId());
        assertSame(due, info.getDueAt());
        assertSame(lock, info.getLockAt());
        assertSame(unlock, info.getUnlockAt());
        assertEquals("nonce", info.getNonce());
    }
}
