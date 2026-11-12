package net.unicon.lti.model.lti.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NonceStateTest {

    @Test
    public void testConstructorAndAccessors() {
        NonceState nonceState = new NonceState("nonce", "hash", "state", "_parent");
        assertEquals("nonce", nonceState.getNonce());
        assertEquals("hash", nonceState.getStateHash());
        assertEquals("state", nonceState.getState());
        assertEquals("_parent", nonceState.getLtiStorageTarget());

        nonceState.setNonce("n2");
        nonceState.setStateHash("h2");
        nonceState.setState("s2");
        nonceState.setLtiStorageTarget(null);
        assertEquals("n2", nonceState.getNonce());
        assertEquals("h2", nonceState.getStateHash());
        assertEquals("s2", nonceState.getState());
        assertNull(nonceState.getLtiStorageTarget());
    }

    @Test
    public void testConstructorRejectsBlankValues() {
        assertThrows(AssertionError.class, () -> new NonceState("", "hash", "state", null));
        assertThrows(AssertionError.class, () -> new NonceState("nonce", " ", "state", null));
        assertThrows(AssertionError.class, () -> new NonceState("nonce", "hash", null, null));
    }

    @Test
    public void testEqualsAndHashCode() {
        NonceState nonceState = new NonceState("nonce", "hash", "state", "a");
        NonceState same = new NonceState("nonce", "hash", "state", "b");

        assertEquals(nonceState, nonceState);
        assertEquals(nonceState, same);
        assertEquals(nonceState.hashCode(), same.hashCode());
        assertEquals(31 * "nonce".hashCode(), nonceState.hashCode());
        assertNotEquals(nonceState, new NonceState("other", "hash", "state", "a"));
        assertNotEquals(nonceState, new NonceState("nonce", "other", "state", "a"));
        assertNotEquals(nonceState, new NonceState("nonce", "hash", "other", "a"));
        assertNotEquals(nonceState, null);
        assertNotEquals(nonceState, "nonce");
        assertEquals(0, new NonceState().hashCode());
    }
}
