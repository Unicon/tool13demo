package net.unicon.lti.model.harmony;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarmonyBookContent {

    private String name;

    @JsonProperty(value = "sub_topics")
    private List<String> subTopics;

    @JsonProperty(value = "module_id")
    private String moduleId;

}
