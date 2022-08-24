package net.unicon.lti.model.harmony;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HarmonyFetchDeepLinksBody {
    @JsonProperty(value = "root_outcome_guid")
    private String rootOutcomeGuid;

    @JsonProperty(value = "id_token")
    private String idToken;

    @JsonProperty(value = "module_ids")
    private List<String> moduleIds;
}