package net.unicon.lti.utils.resourcesearch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.unicon.lti.model.resourcesearch.RsResourceEntity;
import net.unicon.lti.model.resourcesearch.RsSubjectEntity;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.core.serializer.Serializer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.IOException;

@JsonComponent
public class ResourceSearchPageJacksonSerializer extends JsonSerializer<Page> {

    public ResourceSearchPageJacksonSerializer() {
        super();
    }

    @Override
    public void serialize(Page page, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String content = "content";
        if (page.getContent().get(0).getClass() == RsResourceEntity.class) {
            content = "resources";
        } else if (page.getContent().get(0).getClass() == RsSubjectEntity.class) {
            content = "subjects";
        }
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(content, page.getContent());
        jsonGenerator.writeBooleanField("first", page.isFirst());
        jsonGenerator.writeBooleanField("last", page.isLast());
        jsonGenerator.writeNumberField("totalPages", page.getTotalPages());
        jsonGenerator.writeNumberField("totalElements", page.getTotalElements());
        jsonGenerator.writeNumberField("numberOfElements", page.getNumberOfElements());

        jsonGenerator.writeNumberField("size", page.getSize());
        jsonGenerator.writeNumberField("number", page.getNumber());

        Sort sort = page.getSort();

        jsonGenerator.writeArrayFieldStart("sort");

        for (Sort.Order order : sort) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("property", order.getProperty());
            jsonGenerator.writeStringField("direction", order.getDirection().name());
            jsonGenerator.writeBooleanField("ignoreCase", order.isIgnoreCase());
            jsonGenerator.writeStringField("nullHandling", order.getNullHandling().name());
            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
