package net.unicon.lti.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationConfigTest {

    @Mock
    private ConfigurableEnvironment env;

    private ApplicationConfig config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new ApplicationConfig(env);
    }

    @AfterEach
    public void tearDown() {
        config.shutdown();
    }

    @Test
    public void testInitPublishesTheInstanceAndLeavesTheProfilesAlone() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        config.init();

        verify(env, never()).setActiveProfiles(any(String[].class));
        assertSame(config, ApplicationConfig.getInstance());
        assertSame(env, config.getEnvironment());
    }

    @Test
    public void testApplicationContextIsKeptUntilShutdown() {
        ApplicationContext context = mock(ApplicationContext.class);
        config.init();
        config.setApplicationContext(context);
        assertSame(context, ApplicationConfig.getContext());

        config.shutdown();

        assertNull(ApplicationConfig.getContext());
        assertNull(ApplicationConfig.getInstance());
    }
}
