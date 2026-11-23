package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LtiMembershipEntityTest {

    private LtiContextEntity context(long id) {
        LtiContextEntity context = new LtiContextEntity("key", new PlatformDeployment(), "Course", "{}");
        context.setContextId(id);
        return context;
    }

    private LtiUserEntity user(long id) {
        LtiUserEntity user = new LtiUserEntity("user", new Date(), null);
        user.setUserId(id);
        return user;
    }

    private LtiMembershipEntity membership(long membershipId, long contextId, long userId, Integer role) {
        LtiMembershipEntity membership = new LtiMembershipEntity(context(contextId), user(userId), role);
        membership.setMembershipId(membershipId);
        return membership;
    }

    @Test
    public void testConstructorAndAccessors() {
        LtiContextEntity context = context(1L);
        LtiUserEntity user = user(2L);
        LtiMembershipEntity membership = new LtiMembershipEntity(context, user, 1);

        assertSame(context, membership.getContext());
        assertSame(user, membership.getUser());
        assertEquals(1, membership.getRole());
        assertNull(membership.getRoleOverride());

        LtiContextEntity otherContext = context(3L);
        LtiUserEntity otherUser = user(4L);
        membership.setMembershipId(8L);
        membership.setRole(2);
        membership.setRoleOverride(3);
        membership.setContext(otherContext);
        membership.setUser(otherUser);

        assertEquals(8L, membership.getMembershipId());
        assertEquals(2, membership.getRole());
        assertEquals(3, membership.getRoleOverride());
        assertSame(otherContext, membership.getContext());
        assertSame(otherUser, membership.getUser());
    }

    @Test
    public void testConstructorRejectsMissingArguments() {
        assertThrows(AssertionError.class, () -> new LtiMembershipEntity(context(1L), null, 1));
        assertThrows(AssertionError.class, () -> new LtiMembershipEntity(null, user(1L), 1));
    }

    @Test
    public void testEqualsAndHashCode() {
        LtiMembershipEntity membership = membership(1L, 2L, 3L, 4);
        LtiMembershipEntity same = membership(1L, 2L, 3L, 4);

        assertEquals(membership, membership);
        assertEquals(membership, same);
        assertEquals(membership.hashCode(), same.hashCode());
        assertEquals(((1 * 31 + 2) * 31 + 3) * 31 + 4, membership.hashCode());
        assertNotEquals(membership, membership(1L, 9L, 3L, 4));
        assertNotEquals(membership, membership(9L, 2L, 3L, 4));
        assertNotEquals(membership, membership(1L, 2L, 9L, 4));
        assertNotEquals(membership, membership(1L, 2L, 3L, 9));
        assertNotEquals(membership, null);
        assertNotEquals(membership, "membership");
    }

    @Test
    public void testHashCodeWithNullRole() {
        LtiMembershipEntity membership = membership(1L, 2L, 3L, null);
        assertEquals(((1 * 31 + 2) * 31 + 3) * 31, membership.hashCode());
    }

    @Test
    public void testProtectedConstructor() {
        LtiMembershipEntity membership = new LtiMembershipEntity();
        assertNull(membership.getContext());
        assertNull(membership.getUser());
    }
}
