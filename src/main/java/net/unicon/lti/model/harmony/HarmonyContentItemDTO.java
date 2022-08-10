package net.unicon.lti.model.harmony;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import net.unicon.lti.model.lti.dto.DeepLinkingContentItemDTO;
import net.unicon.lti.model.lti.dto.LineItemDTO;

import javax.validation.constraints.NotEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HarmonyContentItemDTO {

    @NotEmpty
    private String title; // required, name of the link as it appears in the LMS

    @NotEmpty
    private String url; // required, url to link

    private Float scoreMaximum; // required if graded, numeric non-null, greater than 0

    private String label; // required if different from title, name of the lineitem as it appears in the grade book

    private String resourceId; // optional, internal identifier

    private String tag; // optional, internal identifier

    public DeepLinkingContentItemDTO toDeepLinkingContentItem() {
        DeepLinkingContentItemDTO deepLinkingContentItem = new DeepLinkingContentItemDTO();
        deepLinkingContentItem.setTitle(this.title);
        deepLinkingContentItem.setUrl(this.url);
        if (this.scoreMaximum != null) {
            LineItemDTO lineItem = new LineItemDTO(this.scoreMaximum, this.label, this.resourceId, this.tag);
            deepLinkingContentItem.setLineItem(lineItem);
        }
        return deepLinkingContentItem;
    }
}
