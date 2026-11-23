package net.unicon.lti.database;

import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.service.common.ResourceService;
import org.springframework.stereotype.Component;

@Component
public class PlatformDeploymentResourceServiceTest implements ResourceService<PlatformDeployment> {

    static final String PLATFORM_DEPLOYMENT_RESOURCE = "classpath:test_data/platform_deployment";

    @Override
    public String getDirectoryPath() {
        return PLATFORM_DEPLOYMENT_RESOURCE;
    }

    @Override
    public void setDefaults() { //Empty on purpose

    }

}
