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
public class HarmonyCourse {

    private String root_outcome_guid;
    private String book_title;
    private String release_date;
    private String cover_img_url;
    private String category;
    private String description;
    List<HarmonyBookContent> table_of_contents;

}
