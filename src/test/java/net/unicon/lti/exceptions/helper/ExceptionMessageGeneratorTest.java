package net.unicon.lti.exceptions.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionMessageGeneratorTest {

    private final ExceptionMessageGenerator generator = new ExceptionMessageGenerator();

    @Test
    public void testWithoutException() {
        assertEquals("custom\n", generator.exceptionMessage("custom", null));
    }

    @Test
    public void testWithExceptionWithoutCause() {
        assertEquals("custom\nException : boom", generator.exceptionMessage("custom", new RuntimeException("boom")));
    }

    @Test
    public void testWithExceptionAndCause() {
        Exception e = new RuntimeException("boom", new IllegalStateException("root"));
        assertEquals("custom\nException : boom\nCause :root", generator.exceptionMessage("custom", e));
    }
}
