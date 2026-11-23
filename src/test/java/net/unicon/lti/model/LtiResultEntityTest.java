package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LtiResultEntityTest {

    private final LtiUserEntity user = new LtiUserEntity("user", new Date(), null);
    private final LtiLinkEntity link = new LtiLinkEntity(
            new LtiContextEntity("key", new PlatformDeployment(), "Course", "{}"),
            new ToolLink("tl", "Title", "Desc", true, 10f));

    @Test
    public void testConstructorCopiesFields() {
        Date retrievedAt = new Date(123456L);
        LtiResultEntity result = new LtiResultEntity(user, link, retrievedAt, 5f, 10f, "comment", "Completed", "FullyGraded");

        assertSame(user, result.getUser());
        assertSame(link, result.getLink());
        assertEquals(new Timestamp(123456L), result.getTimestamp());
        assertEquals(5f, result.getScoreGiven());
        assertEquals(10f, result.getScoreMaximum());
        assertEquals("comment", result.getComment());
        assertEquals("Completed", result.getActivityProgress());
        assertEquals("FullyGraded", result.getGradingProgress());
    }

    @Test
    public void testConstructorDefaultsTimestampToNow() {
        long before = System.currentTimeMillis();
        LtiResultEntity result = new LtiResultEntity(user, link, null, null, null, null, null, null);

        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().getTime() >= before);
    }

    @Test
    public void testConstructorRejectsMissingArguments() {
        assertThrows(AssertionError.class, () -> new LtiResultEntity(null, link, null, null, null, null, null, null));
        assertThrows(AssertionError.class, () -> new LtiResultEntity(user, null, null, null, null, null, null, null));
    }

    @Test
    public void testSetters() {
        LtiResultEntity result = new LtiResultEntity();
        LtiUserEntity otherUser = new LtiUserEntity("other", null, null);
        Timestamp timestamp = new Timestamp(1L);

        result.setResultId(3L);
        result.setScoreGiven(1f);
        result.setScoreMaximum(2f);
        result.setComment("c");
        result.setActivityProgress("a");
        result.setGradingProgress("g");
        result.setTimestamp(timestamp);
        result.setLink(link);
        result.setUser(otherUser);

        assertEquals(3L, result.getResultId());
        assertEquals(1f, result.getScoreGiven());
        assertEquals(2f, result.getScoreMaximum());
        assertEquals("c", result.getComment());
        assertEquals("a", result.getActivityProgress());
        assertEquals("g", result.getGradingProgress());
        assertSame(timestamp, result.getTimestamp());
        assertSame(link, result.getLink());
        assertSame(otherUser, result.getUser());
    }

    @Test
    public void testEqualsAndHashCode() {
        LtiResultEntity result = new LtiResultEntity(user, link, null, 1f, 2f, null, null, null);
        result.setResultId(1L);
        LtiResultEntity same = new LtiResultEntity();
        same.setResultId(1L);
        LtiResultEntity other = new LtiResultEntity();
        other.setResultId(2L);

        assertEquals(result, result);
        assertEquals(result, same);
        assertEquals(result.hashCode(), same.hashCode());
        assertEquals(Objects.hash(1L), result.hashCode());
        assertNotEquals(result, other);
        assertNotEquals(result, null);
        assertNotEquals(result, "result");
    }
}
