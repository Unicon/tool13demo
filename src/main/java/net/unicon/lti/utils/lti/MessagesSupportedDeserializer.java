package net.unicon.lti.utils.lti;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.lti.dto.MessagesSupportedDTO;
import net.unicon.lti.utils.LtiStrings;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MessagesSupportedDeserializer extends StdDeserializer<MessagesSupportedDTO> {

    public MessagesSupportedDeserializer() {
        // Jackson 3 requires the handled type; Jackson 2 accepted null
        this(MessagesSupportedDTO.class);
    }

    protected MessagesSupportedDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MessagesSupportedDTO deserialize(JsonParser jp, DeserializationContext ctxt) {
        JsonNode node = ctxt.readTree(jp);
        MessagesSupportedDTO messagesSupported = new MessagesSupportedDTO();
        if (node.get("type") != null) {
            messagesSupported.setType(node.get("type").asString());
            if (node.get("placements") != null) {
                List<String> placements = new ArrayList<>();
                for (JsonNode jsonNode : node.get("placements")) {
                    placements.add(jsonNode.asString());
                }
                messagesSupported.setPlacements(placements);
            }
        } else if (node.isString() && LtiStrings.MESSAGES_SUPPORTED_TYPES.contains(node.stringValue())) {
            messagesSupported.setType(node.stringValue());
        }
        log.debug("Deserialized Message Type:");
        log.debug(messagesSupported.getType());
        log.debug(messagesSupported.getPlacements() != null ? messagesSupported.getPlacements().toString() : null);

        return messagesSupported;
    }
}