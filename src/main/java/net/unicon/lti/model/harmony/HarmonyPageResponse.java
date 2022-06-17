package net.unicon.lti.model.harmony;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarmonyPageResponse {

    List<HarmonyCourse> records;
    HarmonyMetadata metadata;

}
