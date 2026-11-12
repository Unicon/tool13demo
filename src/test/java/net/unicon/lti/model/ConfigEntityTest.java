package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigEntityTest {

    @Test
    public void testConstructorsAndAccessors() {
        ConfigEntity empty = new ConfigEntity();
        assertNull(empty.getName());
        assertNull(empty.getValue());

        ConfigEntity config = new ConfigEntity("name", "value");
        assertEquals("name", config.getName());
        assertEquals("value", config.getValue());

        config.setId(5L);
        config.setName("other");
        config.setValue("v2");
        assertEquals(5L, config.getId());
        assertEquals("other", config.getName());
        assertEquals("v2", config.getValue());
    }

    @Test
    public void testConstructorRejectsNullName() {
        assertThrows(AssertionError.class, () -> new ConfigEntity(null, "value"));
    }

    @Test
    public void testEqualsAndHashCode() {
        ConfigEntity config = new ConfigEntity("name", "value");
        config.setId(1L);
        ConfigEntity same = new ConfigEntity("name", "different value");
        same.setId(1L);
        ConfigEntity otherId = new ConfigEntity("name", "value");
        otherId.setId(2L);
        ConfigEntity otherName = new ConfigEntity("other", "value");
        otherName.setId(1L);

        assertEquals(config, config);
        assertEquals(config, same);
        assertEquals(config.hashCode(), same.hashCode());
        assertEquals(31 + "name".hashCode(), config.hashCode());
        assertNotEquals(config, otherId);
        assertNotEquals(config, otherName);
        assertNotEquals(config, null);
        assertNotEquals(config, "name");
    }
}
