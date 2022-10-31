package net.unicon.lti.model.resourcesearch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.unicon.lti.model.resourcesearch.utils.TextComplexityNameEnum;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Data
@Embeddable
@NoArgsConstructor
public class TextComplexity {
    @Enumerated(EnumType.STRING)
    private TextComplexityNameEnum textComplexityName;
    private String value;
}
