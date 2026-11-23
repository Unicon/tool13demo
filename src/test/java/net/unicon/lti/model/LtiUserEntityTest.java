package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LtiUserEntityTest {

    private LtiUserEntity user(long id, String key, String email) {
        LtiUserEntity user = new LtiUserEntity(key, new Date(), null);
        user.setUserId(id);
        user.setEmail(email);
        return user;
    }

    @Test
    public void testConstructorCopiesFields() {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        LtiUserEntity user = new LtiUserEntity("user-key", new Date(5000L), platformDeployment);

        assertEquals("user-key", user.getUserKey());
        assertEquals(new Timestamp(5000L), user.getLoginAt());
        assertSame(platformDeployment, user.getPlatformDeployment());
    }

    @Test
    public void testConstructorDefaultsLoginAtAndLeavesDeploymentUnset() {
        long before = System.currentTimeMillis();
        LtiUserEntity user = new LtiUserEntity("user-key", null, null);

        assertNotNull(user.getLoginAt());
        assertTrue(user.getLoginAt().getTime() >= before);
        assertNull(user.getPlatformDeployment());
    }

    @Test
    public void testConstructorRejectsBlankUserKey() {
        assertThrows(AssertionError.class, () -> new LtiUserEntity("", null, null));
        assertThrows(AssertionError.class, () -> new LtiUserEntity(null, null, null));
    }

    @Test
    public void testSetters() {
        LtiUserEntity user = new LtiUserEntity();
        PlatformDeployment platformDeployment = new PlatformDeployment();
        Timestamp loginAt = new Timestamp(1L);
        Set<LtiResultEntity> results = Set.of();

        user.setUserId(1L);
        user.setLmsUserId("lms");
        user.setUserKey("key");
        user.setDisplayName("Name");
        user.setEmail("a@b.c");
        user.setLocale("en");
        user.setSubscribe((short) 1);
        user.setJson("{}");
        user.setLoginAt(loginAt);
        user.setResults(results);
        user.setPlatformDeployment(platformDeployment);

        assertEquals(1L, user.getUserId());
        assertEquals("lms", user.getLmsUserId());
        assertEquals("key", user.getUserKey());
        assertEquals("Name", user.getDisplayName());
        assertEquals("a@b.c", user.getEmail());
        assertEquals("en", user.getLocale());
        assertEquals((short) 1, user.getSubscribe());
        assertEquals("{}", user.getJson());
        assertSame(loginAt, user.getLoginAt());
        assertSame(results, user.getResults());
        assertSame(platformDeployment, user.getPlatformDeployment());
    }

    @Test
    public void testEqualsAndHashCode() {
        LtiUserEntity user = user(1L, "key", "a@b.c");
        LtiUserEntity same = user(1L, "key", "a@b.c");
        same.setDisplayName("ignored");

        assertEquals(user, user);
        assertEquals(user, same);
        assertEquals(user.hashCode(), same.hashCode());
        assertEquals((31 + "key".hashCode()) * 31 + "a@b.c".hashCode(), user.hashCode());
        assertNotEquals(user, user(2L, "key", "a@b.c"));
        assertNotEquals(user, user(1L, "key", "x@y.z"));
        assertNotEquals(user, user(1L, "other", "a@b.c"));
        assertNotEquals(user, null);
        assertNotEquals(user, "user");
    }

    @Test
    public void testHashCodeWithNullFields() {
        LtiUserEntity user = new LtiUserEntity();
        user.setUserId(2L);
        assertEquals(2 * 31 * 31, user.hashCode());
    }
}
