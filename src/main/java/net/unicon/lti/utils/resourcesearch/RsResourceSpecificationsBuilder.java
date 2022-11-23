package net.unicon.lti.utils.resourcesearch;

import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Data
public class RsResourceSpecificationsBuilder {
    private final List<ResourceSearchCriteria> params;

    public RsResourceSpecificationsBuilder() {
        params = new ArrayList<>();
    }

    public Specification build() {
        if (params.size() == 0)
            return null;

        Specification result = null;

        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getKey().equalsIgnoreCase("search")) {
                Specification searchSpec = Specification.where(new RsResourceSpecification(new ResourceSearchCriteria("name", params.get(i).getOperation(), params.get(i).getValue())))
                        .or(new RsResourceSpecification(new ResourceSearchCriteria("description", params.get(i).getOperation(), params.get(i).getValue())));
//                        .or(new RsResourceSpecification(new ResourceSearchCriteria("subject", params.get(i).getOperation(), Collections.singleton(params.get(i).getValue()))));
                if (i == 0) {
                    result = searchSpec;
                } else {
                    result = params.get(i).getOrPredicate()
                            ? Specification.where(result).or(searchSpec)
                            : Specification.where(searchSpec);
                }
            } else {
                if (i == 0) {
                    result = new RsResourceSpecification(params.get(0));
                } else {
                    result = params.get(i).getOrPredicate()
                            ? Specification.where(result).or(new RsResourceSpecification(params.get(i)))
                            : Specification.where(result).and(new RsResourceSpecification(params.get(i)));
                }
            }
        }

        return result;
    }
}
