package net.unicon.lti.database;

import lombok.RequiredArgsConstructor;
import net.unicon.lti.config.ApplicationConfig;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.LtiUserRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Set;

/**
 * Check if the database has initial data in it,
 * if it is empty on startup then we populate it with some initial data
 */
@Component
@Profile("!test")
// only load this when running the application (not for unit tests which have the 'testing' profile active)
@RequiredArgsConstructor
public class DatabasePreload {

    static final Logger log = LoggerFactory.getLogger(DatabasePreload.class);

    private final ApplicationConfig applicationConfig;

    private final LtiUserRepository ltiUserRepository;
    private final PlatformDeploymentRepository platformDeploymentRepository;

    private final PlatformDeploymentResourceService platformDeploymentResources;

    private final LtiUserEntityResourceService ltiUserEntityResourceService;

    @PostConstruct
    public void init() throws IOException {

        if (platformDeploymentRepository.count() > 0) {
            // done, no preloading
            log.info("INIT - no preload");
        } else {
            buildDataFromFiles();
        }
    }

    public void buildDataFromFiles() throws IOException {
        Set<PlatformDeployment> deploymentPlatforms = platformDeploymentResources.getResources(PlatformDeployment.class);
        for (PlatformDeployment deploymentPlatform : deploymentPlatforms) {
            log.info("Storing: " + deploymentPlatform.getKeyId() + " : " + deploymentPlatform.getIss());
            // a seed row is always new: the version in the file would make Spring Data merge it
            // instead, and Hibernate 7 rejects merging a row that doesn't exist
            deploymentPlatform.setVersion(null);
            platformDeploymentRepository.save(deploymentPlatform);
        }

        Set<LtiUserEntity> users = ltiUserEntityResourceService.getResources(LtiUserEntity.class);
        for (LtiUserEntity user : users) {
            // the same for seed users, whose file also fixes the generated id
            user.setUserId(0);
            user.setVersion(null);
            ltiUserRepository.save(user);
        }
    }
}
