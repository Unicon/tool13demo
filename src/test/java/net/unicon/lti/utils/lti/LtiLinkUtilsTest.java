package net.unicon.lti.utils.lti;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LtiLinkUtilsTest {

    @Test
    public void testReadsTheLinkParameter() {
        assertEquals("1234", LtiLinkUtils.linkParameter("https://bob.anteater-ruler.ts.net/lti3?link=1234"));
    }

    @Test
    public void testReadsTheLinkAmongOtherParameters() {
        assertEquals("1234", LtiLinkUtils.linkParameter("https://tool.example.com/lti3?a=1&link=1234&b=2"));
    }

    @Test
    public void testDecodesTheLink() {
        assertEquals("a b/c", LtiLinkUtils.linkParameter("https://tool.example.com/lti3?link=a%20b%2Fc"));
    }

    @Test
    public void testNoLinkParameter() {
        // the old substring parsing turned this into "://bob.anteater-ruler.ts.net/lti3"
        assertNull(LtiLinkUtils.linkParameter("https://bob.anteater-ruler.ts.net/lti3"));
        assertNull(LtiLinkUtils.linkParameter("https://tool.example.com/lti3?other=1"));
        assertNull(LtiLinkUtils.linkParameter("https://tool.example.com/lti3?link="));
    }

    @Test
    public void testNoTargetLinkUri() {
        assertNull(LtiLinkUtils.linkParameter(null));
        assertNull(LtiLinkUtils.linkParameter(" "));
    }

    @Test
    public void testUnreadableUri() {
        assertNull(LtiLinkUtils.linkParameter("https://tool.example.com/lti3?link=%zz"));
    }

    @Test
    public void testCannotBeInstantiated() throws Exception {
        Constructor<LtiLinkUtils> constructor = LtiLinkUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

}
