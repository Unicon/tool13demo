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
package net.unicon.lti13demo.repository;

import net.unicon.lti13demo.model.ConfigEntity;
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
