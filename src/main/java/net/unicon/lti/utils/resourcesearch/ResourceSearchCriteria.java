package net.unicon.lti.utils.resourcesearch;

import lombok.Data;

@Data
public class ResourceSearchCriteria {
    private String key;
    private String operation;
    private Object value;
    private Boolean orPredicate;

    public ResourceSearchCriteria(String key, String operation, Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
    }
}
