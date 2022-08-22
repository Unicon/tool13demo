package net.unicon.lti.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineItemDTO {
    private Float scoreMaximum; // required if graded, numeric non-null, greater than 0

    private String label; // required if different from title, name of the lineitem as it appears in the grade book

    private String resourceId; // optional, internal identifier

    private String tag; // optional, internal identifier
}
