package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseEntityTest {

    @Test
    public void testPreCreateSetsBothTimestampsToNow() {
        BaseEntity entity = new BaseEntity();
        long before = System.currentTimeMillis();

        entity.preCreate();

        long after = System.currentTimeMillis();
        assertNotNull(entity.getCreatedAt());
        assertSame(entity.getCreatedAt(), entity.getUpdatedAt());
        assertTrue(entity.getCreatedAt().getTime() >= before && entity.getCreatedAt().getTime() <= after);
    }

    @Test
    public void testPreUpdateOnlyChangesUpdatedAt() {
        BaseEntity entity = new BaseEntity();
        Timestamp created = new Timestamp(1000L);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(new Timestamp(1000L));
        long before = System.currentTimeMillis();

        entity.preUpdate();

        assertSame(created, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().getTime() >= before);
        assertFalse(entity.getUpdatedAt().equals(created));
    }

    @Test
    public void testAccessors() {
        BaseEntity entity = new BaseEntity();
        assertNull(entity.getVersion());
        Timestamp created = new Timestamp(1L);
        Timestamp updated = new Timestamp(2L);

        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);
        entity.setVersion(3);

        assertSame(created, entity.getCreatedAt());
        assertSame(updated, entity.getUpdatedAt());
        assertEquals(3, entity.getVersion());
    }
}
