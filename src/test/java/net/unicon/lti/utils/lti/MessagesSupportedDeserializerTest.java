package net.unicon.lti.utils.lti;

import net.unicon.lti.model.lti.dto.MessagesSupportedDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MessagesSupportedDeserializerTest {

    // the default mapper uses the @JsonDeserialize annotation on MessagesSupportedDTO
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    public void testNoArgConstructorHandlesMessagesSupportedDTO() {
        assertEquals(MessagesSupportedDTO.class, new MessagesSupportedDeserializer().handledType());
    }

    private MessagesSupportedDTO parse(String json) {
        return jsonMapper.readValue(json, MessagesSupportedDTO.class);
    }

    @Test
    public void testObjectFormWithPlacements() {
        MessagesSupportedDTO dto = parse("{\"type\":\"LtiDeepLinkingRequest\",\"placements\":[\"editor_button\",\"link_selection\"]}");

        assertEquals("LtiDeepLinkingRequest", dto.getType());
        assertEquals(List.of("editor_button", "link_selection"), dto.getPlacements());
    }

    @Test
    public void testObjectFormWithoutPlacements() {
        MessagesSupportedDTO dto = parse("{\"type\":\"LtiResourceLinkRequest\"}");

        assertEquals("LtiResourceLinkRequest", dto.getType());
        assertNull(dto.getPlacements());
    }

    @Test
    public void testStringFormWithKnownType() {
        MessagesSupportedDTO dto = parse("\"LtiResourceLinkRequest\"");

        assertEquals("LtiResourceLinkRequest", dto.getType());
        assertNull(dto.getPlacements());
    }

    @Test
    public void testStringFormWithUnknownTypeLeavesTypeUnset() {
        MessagesSupportedDTO dto = parse("\"SomethingElse\"");

        assertNull(dto.getType());
        assertNull(dto.getPlacements());
    }

    @Test
    public void testObjectWithoutTypeLeavesTypeUnset() {
        MessagesSupportedDTO dto = parse("{\"placements\":[\"x\"]}");

        assertNull(dto.getType());
        assertNull(dto.getPlacements());
    }

    @Test
    public void testMixedList() {
        List<MessagesSupportedDTO> list = jsonMapper.readValue(
                "[\"LtiDeepLinkingRequest\",{\"type\":\"LtiResourceLinkRequest\",\"placements\":[]}]",
                new TypeReference<List<MessagesSupportedDTO>>() { });

        assertEquals("LtiDeepLinkingRequest", list.get(0).getType());
        assertEquals("LtiResourceLinkRequest", list.get(1).getType());
        assertEquals(List.of(), list.get(1).getPlacements());
    }
}
