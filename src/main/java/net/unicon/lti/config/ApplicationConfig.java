package net.unicon.lti.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Allows for easy access to the application configuration,
 * merges config settings from spring and local application config
 */
@Component
@RequiredArgsConstructor
public class ApplicationConfig implements ApplicationContextAware {

    static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

    private static final Object contextLock = new Object();
    private static final Object configLock = new Object();

    private static ApplicationContext context;
    private static ApplicationConfig config;

    private final ConfigurableEnvironment env;

    @PostConstruct
    public void init() {
        log.info("INIT");
        synchronized (configLock) {
            config = this;
        }
        log.info("Config INIT: profiles active: {}.", ArrayUtils.toString(env.getActiveProfiles()));
    }

    @PreDestroy
    public void shutdown() {
        synchronized (contextLock) {
            context = null;
        }
        synchronized (configLock) {
            config = null;
        }
        log.info("DESTROY");
    }

    // DELEGATED from the spring Environment (easier config access)

    public ConfigurableEnvironment getEnvironment() {
        return env;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        synchronized (contextLock) {
            context = applicationContext;
        }
    }

    /**
     * @return the current service instance the spring application context (only populated after init)
     */
    public static ApplicationContext getContext() {
        return context;
    }

    /**
     * @return the current service instance of the config object (only populated after init)
     */
    public static ApplicationConfig getInstance() {
        return config;
    }

}
