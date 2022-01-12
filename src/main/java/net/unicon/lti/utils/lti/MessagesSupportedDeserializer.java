package net.unicon.lti.utils.lti;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.lti.dto.MessagesSupportedDTO;
import net.unicon.lti.utils.LtiStrings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MessagesSupportedDeserializer extends StdDeserializer<MessagesSupportedDTO> {

    public MessagesSupportedDeserializer() {
        this(null);
    }

    protected MessagesSupportedDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MessagesSupportedDTO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        MessagesSupportedDTO messagesSupported = new MessagesSupportedDTO();
        if (node.get("type") != null) {
            messagesSupported.setType(node.get("type").asText());
            if (node.get("placements") != null) {
                List<String> placements = new ArrayList<>();
                for (JsonNode jsonNode : node.get("placements")) {
                    placements.add(jsonNode.asText());
                }
                messagesSupported.setPlacements(placements);
            }
        } else if (LtiStrings.MESSAGES_SUPPORTED_TYPES.contains(node.asText())) {
            messagesSupported.setType(node.asText());
        }
        log.debug("Deserialized Message Type:");
        log.debug(messagesSupported.getType());
        log.debug(messagesSupported.getPlacements() != null ? messagesSupported.getPlacements().toString() : null);

        return messagesSupported;
    }
}