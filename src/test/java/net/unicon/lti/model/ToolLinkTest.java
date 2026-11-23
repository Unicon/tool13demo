package net.unicon.lti.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ToolLinkTest {

    @Test
    public void testConstructorAndAccessors() {
        ToolLink toolLink = new ToolLink("id", "Title", "Desc", true, 10f);

        assertEquals("id", toolLink.getToolLinkId());
        assertEquals("Title", toolLink.getTitle());
        assertEquals("Desc", toolLink.getDescription());
        assertEquals(true, toolLink.getIsAssignment());
        assertEquals(10f, toolLink.getMaxGrade());

        toolLink.setTitle("T2");
        toolLink.setDescription("D2");
        toolLink.setIsAssignment(false);
        toolLink.setMaxGrade(5f);

        assertEquals("T2", toolLink.getTitle());
        assertEquals("D2", toolLink.getDescription());
        assertEquals(false, toolLink.getIsAssignment());
        assertEquals(5f, toolLink.getMaxGrade());
        assertNull(new ToolLink().getToolLinkId());
    }

    @Test
    public void testEqualsComparesToolLinkIdByValue() {
        ToolLink toolLink = new ToolLink("id", "Title", "Desc", true, 10f);

        assertEquals(toolLink, toolLink);
        assertEquals(toolLink, new ToolLink("id", "Other", null, null, null));
        assertEquals(toolLink, new ToolLink(new String("id"), "Title", "Desc", true, 10f));
        assertNotEquals(toolLink, new ToolLink("other-id", "Title", "Desc", true, 10f));
        assertNotEquals(toolLink, null);
        assertNotEquals(toolLink, "id");
    }

    @Test
    public void testHashCode() {
        // equal links (same id, any title) have the same hash code
        assertEquals(new ToolLink("id", "Title", "Desc", true, 10f).hashCode(), new ToolLink(new String("id"), "Other", null, null, null).hashCode());
        assertEquals("id".hashCode(), new ToolLink("id", "Title", "Desc", true, 10f).hashCode());
        assertEquals(0, new ToolLink(null, null, null, null, null).hashCode());
    }
}
