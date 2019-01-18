/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.repository;

import net.unicon.lti13demo.model.LtiResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiResultRepository extends JpaRepository<LtiResultEntity, Long> {

    /**
     * @param resultId the unique resultId key
     * @return the LtiResultEntity OR null if there is no entity matching this key
     */
    LtiResultEntity findByResultId(Long resultId);
}
