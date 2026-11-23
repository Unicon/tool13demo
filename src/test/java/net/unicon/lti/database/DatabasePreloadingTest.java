package net.unicon.lti.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.LtiUserRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DatabasePreloadingTest {

    @Mock private LtiUserRepository ltiUserRepository;
    @Mock private PlatformDeploymentRepository platformDeploymentRepository;
    @Mock private LtiContextRepository ltiContextRepository;
    @Mock private PlatformDeploymentResourceService platformDeploymentResourceService;
    @Mock private LtiUserEntityResourceService ltiUserEntityResourceService;
    @Mock private PlatformDeploymentResourceServiceTest platformDeploymentResourceServiceTest;
    @Mock private LtiUserEntityResourceServiceTest ltiUserEntityResourceServiceTest;
    @Mock private LtiContextEntityResourceServiceTest ltiContextEntityResourceServiceTest;

    private PlatformDeployment deployment;
    private LtiUserEntity user;
    private LtiContextEntity context;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        deployment = new PlatformDeployment();
        deployment.setIss("https://platform.example.com");
        user = new LtiUserEntity("user-1", null, null);
        context = new LtiContextEntity("context-1", deployment, "title", null);
        when(platformDeploymentResourceService.getResources(PlatformDeployment.class)).thenReturn(Set.of(deployment));
        when(ltiUserEntityResourceService.getResources(LtiUserEntity.class)).thenReturn(Set.of(user));
        when(platformDeploymentResourceServiceTest.getResources(PlatformDeployment.class)).thenReturn(Set.of(deployment));
        when(ltiUserEntityResourceServiceTest.getResources(LtiUserEntity.class)).thenReturn(Set.of(user));
        when(ltiContextEntityResourceServiceTest.getResources(LtiContextEntity.class)).thenReturn(Set.of(context));
    }

    private DatabasePreload preload() {
        return new DatabasePreload(null, ltiUserRepository, platformDeploymentRepository, platformDeploymentResourceService, ltiUserEntityResourceService);
    }

    private DatabasePreloadTest preloadTest() {
        AllRepositories repos = new AllRepositories(null, ltiContextRepository, null, null, null, null, ltiUserRepository, platformDeploymentRepository, null, null);
        return new DatabasePreloadTest(null, repos, platformDeploymentResourceServiceTest, ltiUserEntityResourceServiceTest, ltiContextEntityResourceServiceTest);
    }

    @Test
    public void preloadSeedsAnEmptyDatabase() throws Exception {
        when(platformDeploymentRepository.count()).thenReturn(0L);
        preload().init();
        verify(platformDeploymentRepository).save(deployment);
        verify(ltiUserRepository).save(user);
    }

    @Test
    public void preloadSavesSeedRowsAsNew() throws Exception {
        // the seed files carry "version": 0, and the user file a fixed userId; saved as they are,
        // Spring Data would merge them, which Hibernate 7 rejects for rows that don't exist
        deployment.setVersion(0);
        user.setVersion(0);
        user.setUserId(1);
        when(platformDeploymentRepository.count()).thenReturn(0L);

        preload().init();

        verify(platformDeploymentRepository).save(deployment);
        verify(ltiUserRepository).save(user);
        assertNull(deployment.getVersion());
        assertNull(user.getVersion());
        assertEquals(0L, user.getUserId());
    }

    @Test
    public void preloadLeavesAPopulatedDatabaseAlone() throws Exception {
        when(platformDeploymentRepository.count()).thenReturn(3L);
        preload().init();
        verify(platformDeploymentRepository, never()).save(any());
        verifyNoInteractions(ltiUserRepository, platformDeploymentResourceService, ltiUserEntityResourceService);
    }

    @Test
    public void testPreloadSeedsAnEmptyDatabase() throws Exception {
        when(platformDeploymentRepository.count()).thenReturn(0L);
        preloadTest().initTest();
        verify(platformDeploymentRepository).save(deployment);
        verify(ltiContextRepository).save(context);
        verify(ltiUserRepository).save(user);
    }

    @Test
    public void testPreloadSavesSeedRowsAsNew() throws Exception {
        deployment.setVersion(0);
        context.setContextId(5);
        context.setVersion(0);
        user.setUserId(1);
        user.setVersion(0);
        when(platformDeploymentRepository.count()).thenReturn(0L);

        preloadTest().initTest();

        assertNull(deployment.getVersion());
        assertEquals(0L, context.getContextId());
        assertNull(context.getVersion());
        assertEquals(0L, user.getUserId());
        assertNull(user.getVersion());
    }

    @Test
    public void testPreloadLeavesAPopulatedDatabaseAlone() throws Exception {
        when(platformDeploymentRepository.count()).thenReturn(1L);
        preloadTest().initTest();
        verify(platformDeploymentRepository, never()).save(any());
        verifyNoInteractions(ltiUserRepository, ltiContextRepository, platformDeploymentResourceServiceTest,
                ltiUserEntityResourceServiceTest, ltiContextEntityResourceServiceTest);
    }
}
