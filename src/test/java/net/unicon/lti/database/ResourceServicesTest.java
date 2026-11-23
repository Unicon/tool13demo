package net.unicon.lti.database;

import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.unicon.lti.model.LtiUserEntity;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceServicesTest {

    @Test
    public void platformDeploymentResourcesPointAtTheSeedFolder() {
        PlatformDeploymentResourceService service = new PlatformDeploymentResourceService();
        assertEquals("classpath:data/platform_deployment", service.getDirectoryPath());
        assertDoesNotThrow(service::setDefaults);
    }

    @Test
    public void userResourcesLoadTheSeedFiles() throws Exception {
        LtiUserEntityResourceService service = new LtiUserEntityResourceService();
        assertEquals("classpath:data/users", service.getDirectoryPath());
        assertDoesNotThrow(service::setDefaults);

        Set<LtiUserEntity> users = service.getResources(LtiUserEntity.class);

        assertEquals(1, users.size());
        LtiUserEntity user = users.iterator().next();
        assertEquals("admin", user.getUserKey());
        assertEquals("Admin", user.getDisplayName());
    }

    @Test
    public void testMissingSeedFolderLoadsNothing() throws Exception {
        // there are no test_data folders on the classpath
        assertTrue(new PlatformDeploymentResourceServiceTest().getResources(PlatformDeployment.class).isEmpty());
        assertTrue(new LtiUserEntityResourceServiceTest().getResources(LtiUserEntity.class).isEmpty());
        assertTrue(new LtiContextEntityResourceServiceTest().getResources(LtiContextEntity.class).isEmpty());
    }

    @Test
    public void testResourceServicesPointAtTheTestDataFolders() {
        PlatformDeploymentResourceServiceTest deployments = new PlatformDeploymentResourceServiceTest();
        LtiUserEntityResourceServiceTest users = new LtiUserEntityResourceServiceTest();
        LtiContextEntityResourceServiceTest contexts = new LtiContextEntityResourceServiceTest();
        assertEquals("classpath:test_data/platform_deployment", deployments.getDirectoryPath());
        assertEquals("classpath:test_data/users", users.getDirectoryPath());
        assertEquals("classpath:test_data/lti_context_entities", contexts.getDirectoryPath());
        assertDoesNotThrow(deployments::setDefaults);
        assertDoesNotThrow(users::setDefaults);
        assertDoesNotThrow(contexts::setDefaults);
    }
}
