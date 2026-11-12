package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApiOneUseTokenTest {

    @Test
    public void testConstructorAndAccessors() {
        ApiOneUseToken token = new ApiOneUseToken("abc");
        assertEquals("abc", token.getToken());

        token.setTokenId(7L);
        token.setToken("def");

        assertEquals(7L, token.getTokenId());
        assertEquals("def", token.getToken());
    }

    @Test
    public void testConstructorRejectsBlankToken() {
        assertThrows(AssertionError.class, () -> new ApiOneUseToken(" "));
        assertThrows(AssertionError.class, () -> new ApiOneUseToken(null));
    }

    @Test
    public void testEqualsAndHashCode() {
        ApiOneUseToken token = new ApiOneUseToken("abc");
        ApiOneUseToken same = new ApiOneUseToken("abc");
        same.setTokenId(99L);

        assertEquals(token, token);
        assertEquals(token, same);
        assertEquals(token.hashCode(), same.hashCode());
        assertEquals(31 * "abc".hashCode(), token.hashCode());
        assertNotEquals(token, new ApiOneUseToken("xyz"));
        assertNotEquals(token, null);
        assertNotEquals(token, "abc");
    }

    @Test
    public void testHashCodeWithNullToken() {
        ApiOneUseToken token = new ApiOneUseToken();
        assertNull(token.getToken());
        assertEquals(0, token.hashCode());
        assertEquals(token, new ApiOneUseToken());
    }
}
