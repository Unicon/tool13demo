package net.unicon.lti.model.harmony;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarmonyMetadata {

    private String page;
    private String per_page;
    private int page_count;
    private int total_count;
    HarmonyMetadataLinks links;

}
