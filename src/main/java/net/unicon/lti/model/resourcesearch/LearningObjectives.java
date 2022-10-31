package net.unicon.lti.model.resourcesearch;

import lombok.Data;
import net.unicon.lti.model.resourcesearch.utils.AlignmentTypeEnum;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Data
@Embeddable
public class LearningObjectives {
    @Enumerated(EnumType.STRING)
    private AlignmentTypeEnum alignmentType;
    private String educationalFramework;
    private String targetDescription;
    private String targetName;
    private String targetURL;
    private String caseItemUri;
    private String caseItemGUID;
}
