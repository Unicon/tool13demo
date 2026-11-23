package net.unicon.lti.database;


import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
public class LtiUserEntityResourceService implements ResourceService<LtiUserEntity> {

    static final String USERS_RESOURCE = "classpath:data/users";

    @Override
    public String getDirectoryPath() {
        return USERS_RESOURCE;
    }

    @Override
    public void setDefaults() {//Empty on purpose
    }

}
