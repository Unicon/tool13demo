package net.unicon.lti.database;

import lombok.RequiredArgsConstructor;
import net.unicon.lti.config.ApplicationConfig;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.LtiContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Set;

@Component
@Profile("test")
// only load this when running unit tests (not for for the application which has the '!testing' profile active)
@RequiredArgsConstructor
public class DatabasePreloadTest {

    static final Logger log = LoggerFactory.getLogger(DatabasePreloadTest.class);

    private final ApplicationConfig applicationConfig;

    private final AllRepositories allRepositories;

    private final PlatformDeploymentResourceServiceTest platformDeploymentResources;

    private final LtiUserEntityResourceServiceTest ltiUserEntityResourceService;

    private final LtiContextEntityResourceServiceTest ltiContextEntityResourceService;


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
            // seed rows are always new; see DatabasePreload
            deploymentPlatform.setVersion(null);
            allRepositories.platformDeploymentRepository.save(deploymentPlatform);
        }

        Set<LtiContextEntity> contextEntities = ltiContextEntityResourceService.getResources(LtiContextEntity.class);
        for (LtiContextEntity ltiContextEntity : contextEntities) {
            log.info("Storing " + ltiContextEntity.getContextId() + " : " + ltiContextEntity.getTitle());
            ltiContextEntity.setContextId(0);
            ltiContextEntity.setVersion(null);
            allRepositories.contexts.save(ltiContextEntity);
        }

        Set<LtiUserEntity> users = ltiUserEntityResourceService.getResources(LtiUserEntity.class);
        for (LtiUserEntity user : users) {
            user.setUserId(0);
            user.setVersion(null);
            allRepositories.users.save(user);
        }
    }
}
