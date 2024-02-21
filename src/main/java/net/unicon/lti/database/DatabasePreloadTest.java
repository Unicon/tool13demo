package net.unicon.lti.database;

import net.unicon.lti.config.ApplicationConfig;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.LtiContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Set;

@Component
@Profile("test")
// only load this when running unit tests (not for for the application which has the '!testing' profile active)
public class DatabasePreloadTest {

    static final Logger log = LoggerFactory.getLogger(DatabasePreloadTest.class);

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    @SuppressWarnings({"SpringJavaAutowiredMembersInspection", "SpringJavaAutowiringInspection"})
    AllRepositories allRepositories;

    @Autowired
    PlatformDeploymentResourceServiceTest platformDeploymentResources;

    @Autowired
    LtiUserEntityResourceServiceTest ltiUserEntityResourceService;

    @Autowired
    LtiContextEntityResourceServiceTest ltiContextEntityResourceService;


    @PostConstruct
    public void initTest() throws IOException {

        if (allRepositories.platformDeploymentRepository.count() > 0) {
            // done, no preloading
            log.info("INIT - no preload");
        } else {
            buildDataFromFilesTest();
        }
    }

    public void buildDataFromFilesTest() throws IOException {
        Set<PlatformDeployment> deploymentPlatforms = platformDeploymentResources.getResources(PlatformDeployment.class);
        for (PlatformDeployment deploymentPlatform : deploymentPlatforms) {
            log.info("Storing (test): " + deploymentPlatform.getKeyId() + " : " + deploymentPlatform.getIss());
            allRepositories.platformDeploymentRepository.save(deploymentPlatform);
        }

        Set<LtiContextEntity> contextEntities = ltiContextEntityResourceService.getResources(LtiContextEntity.class);
        for (LtiContextEntity ltiContextEntity : contextEntities) {
            log.info("Storing " + ltiContextEntity.getContextId() + " : " + ltiContextEntity.getTitle());
            allRepositories.contexts.save(ltiContextEntity);
        }

        Set<LtiUserEntity> users = ltiUserEntityResourceService.getResources(LtiUserEntity.class);
        for (LtiUserEntity user : users) {
            allRepositories.users.save(user);
        }
    }
}
