/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.config;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Allows for easy access to the application configuration,
 * merges config settings from spring and local application config
 */
@Component
public class ApplicationConfig implements ApplicationContextAware {

    static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);

    private static final Object contextLock = new Object();
    private static final Object configLock = new Object();

    private static ApplicationContext context;
    private static ApplicationConfig config;

    @Autowired
    ConfigurableEnvironment env;

    @PostConstruct
    public void init() {
        log.info("INIT");
        env.setActiveProfiles("dev", "testing");
        synchronized (configLock) {
            config = this;
        }
        log.info("Config INIT: profiles active: {0}.", ArrayUtils.toString(env.getActiveProfiles()));
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
