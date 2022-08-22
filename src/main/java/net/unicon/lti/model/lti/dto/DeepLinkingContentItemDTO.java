package net.unicon.lti.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepLinkingContentItemDTO {
    @NotEmpty
    private String type = "ltiResourceLink"; // required

    private String title; // name of the link as it appears in the LMS

    private String url; // url to link

    private LineItemDTO lineItem; // optional
}
