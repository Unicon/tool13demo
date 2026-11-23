package net.unicon.lti.repository;

import net.unicon.lti.model.ConfigEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

    /**
     * @param name the config name (e.g. app.config)
     * @return the count of config items with this exact name
     */
    int countByName(String name);

    /**
     * @param name the config name (e.g. app.config)
     * @return the config item (or null if none found)
     */
    @Cacheable(value = "configs", key = "#name")
    ConfigEntity findByName(String name);
}
