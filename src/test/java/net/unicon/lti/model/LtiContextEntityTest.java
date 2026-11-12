package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LtiContextEntityTest {

    private final PlatformDeployment platformDeployment = new PlatformDeployment();

    @Test
    public void testShortConstructor() {
        LtiContextEntity context = new LtiContextEntity("key", platformDeployment, "Title", "{}");

        assertEquals("key", context.getContextKey());
        assertSame(platformDeployment, context.getPlatformDeployment());
        assertEquals("Title", context.getTitle());
        assertEquals("{}", context.getJson());
        assertNull(context.getContext_memberships_url());
        assertNull(context.getLineitems());
    }

    @Test
    public void testFullConstructor() {
        LtiContextEntity context = new LtiContextEntity("key", platformDeployment, "Title", "https://m", "https://li", "{}");

        assertEquals("key", context.getContextKey());
        assertSame(platformDeployment, context.getPlatformDeployment());
        assertEquals("Title", context.getTitle());
        assertEquals("https://m", context.getContext_memberships_url());
        assertEquals("https://li", context.getLineitems());
        assertEquals("{}", context.getJson());
    }

    @Test
    public void testConstructorsRejectMissingArguments() {
        assertThrows(AssertionError.class, () -> new LtiContextEntity("", platformDeployment, "t", "{}"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity("key", null, "t", "{}"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity(null, platformDeployment, "t", "m", "l", "{}"));
        assertThrows(AssertionError.class, () -> new LtiContextEntity("key", null, "t", "m", "l", "{}"));
    }

    @Test
    public void testSetters() {
        LtiContextEntity context = new LtiContextEntity();
        PlatformDeployment other = new PlatformDeployment();
        Set<LtiLinkEntity> links = Set.of();
        Set<LtiMembershipEntity> memberships = Set.of();

        context.setContextId(4L);
        context.setContextKey("k");
        context.setTitle("t");
        context.setJson("j");
        context.setSettings("s");
        context.setPlatformDeployment(other);
        context.setLinks(links);
        context.setMemberships(memberships);
        context.setContext_memberships_url("m");
        context.setLineitems("l");

        assertEquals(4L, context.getContextId());
        assertEquals("k", context.getContextKey());
        assertEquals("t", context.getTitle());
        assertEquals("j", context.getJson());
        assertEquals("s", context.getSettings());
        assertSame(other, context.getPlatformDeployment());
        assertSame(links, context.getLinks());
        assertSame(memberships, context.getMemberships());
        assertEquals("m", context.getContext_memberships_url());
        assertEquals("l", context.getLineitems());
    }

    @Test
    public void testEqualsAndHashCode() {
        LtiContextEntity context = new LtiContextEntity("key", platformDeployment, "A", "{}");
        context.setContextId(1L);
        LtiContextEntity same = new LtiContextEntity("key", new PlatformDeployment(), "B", "[]");
        same.setContextId(1L);
        LtiContextEntity otherId = new LtiContextEntity("key", platformDeployment, "A", "{}");
        otherId.setContextId(2L);
        LtiContextEntity otherKey = new LtiContextEntity("other", platformDeployment, "A", "{}");
        otherKey.setContextId(1L);

        assertEquals(context, context);
        assertEquals(context, same);
        assertEquals(context.hashCode(), same.hashCode());
        assertEquals(31 + "key".hashCode(), context.hashCode());
        assertNotEquals(context, otherId);
        assertNotEquals(context, otherKey);
        assertNotEquals(context, null);
        assertNotEquals(context, "key");
    }

    @Test
    public void testHashCodeWithNullKey() {
        LtiContextEntity context = new LtiContextEntity();
        context.setContextId(3L);
        assertEquals(93, context.hashCode());
    }
}
