package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LtiLinkEntityTest {

    private static final String TOOL_LINK_ID = "tool-link-1";

    private LtiContextEntity context(long id) {
        LtiContextEntity context = new LtiContextEntity("key", new PlatformDeployment(), "Course", "{}");
        context.setContextId(id);
        return context;
    }

    private ToolLink toolLink() {
        return new ToolLink(TOOL_LINK_ID, "Link Title", "Link description", true, 10f);
    }

    @Test
    public void testConstructorAndAccessors() {
        LtiContextEntity context = context(1L);
        ToolLink toolLink = toolLink();
        LtiLinkEntity link = new LtiLinkEntity(context, toolLink);

        assertSame(context, link.getContext());
        assertSame(toolLink, link.getToolLink());

        LtiContextEntity otherContext = context(2L);
        ToolLink otherToolLink = new ToolLink("other", "t", "d", false, 1f);
        Set<LtiResultEntity> results = Set.of();
        link.setLinkId(9L);
        link.setLtiLinkId("lti-1");
        link.setContext(otherContext);
        link.setToolLink(otherToolLink);
        link.setResults(results);

        assertEquals(9L, link.getLinkId());
        assertEquals("lti-1", link.getLtiLinkId());
        assertSame(otherContext, link.getContext());
        assertSame(otherToolLink, link.getToolLink());
        assertSame(results, link.getResults());
    }

    @Test
    public void testProtectedConstructor() {
        LtiLinkEntity link = new LtiLinkEntity();
        assertNull(link.getContext());
        assertNull(link.getToolLink());
        assertNull(link.getLtiLinkId());
    }

    @Test
    public void testConstructorRejectsMissingArguments() {
        assertThrows(AssertionError.class, () -> new LtiLinkEntity(context(1L), null));
        assertThrows(AssertionError.class, () -> new LtiLinkEntity(null, toolLink()));
    }

    @Test
    public void testCreateHtmlFromLink() {
        LtiLinkEntity link = new LtiLinkEntity(context(1L), toolLink());
        link.setLtiLinkId("lti-1");

        assertEquals("Link Requested:\n"
                + "Tool Link Id:tool-link-1\n"
                + "Link Title:Link Title\n"
                + "Description:Link description\n"
                + "LTI Link Id:lti-1\n", link.createHtmlFromLink());
    }

    @Test
    public void testEqualsAndHashCode() {
        ToolLink toolLink = toolLink();
        LtiLinkEntity link = new LtiLinkEntity(context(1L), toolLink);
        link.setLinkId(5L);
        LtiLinkEntity same = new LtiLinkEntity(context(1L), toolLink);
        same.setLinkId(5L);
        LtiLinkEntity otherId = new LtiLinkEntity(context(1L), toolLink);
        otherId.setLinkId(6L);
        LtiLinkEntity otherToolLink = new LtiLinkEntity(context(1L), new ToolLink("x", "y", "z", false, 1f));
        otherToolLink.setLinkId(5L);
        LtiLinkEntity otherContext = new LtiLinkEntity(context(2L), toolLink);
        otherContext.setLinkId(5L);

        assertEquals(link, link);
        assertEquals(link, same);
        assertEquals(link.hashCode(), same.hashCode());
        assertEquals(31 * 5 + "Link Title".hashCode() + context(1L).hashCode(), link.hashCode());
        assertNotEquals(link, otherId);
        assertNotEquals(link, otherToolLink);
        assertNotEquals(link, otherContext);
        assertNotEquals(link, null);
        assertNotEquals(link, "link");
    }
}
