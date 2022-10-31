package net.unicon.lti.model.resourcesearch;

import lombok.Data;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class Property {
    private String name;
    private String value;
}
