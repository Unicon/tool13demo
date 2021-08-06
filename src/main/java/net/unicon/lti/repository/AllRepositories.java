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
package net.unicon.lti.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Special service to give access to all the repositories in one place
 * <p/>
 * This is just here to make it a little easier to get access to the full set of repositories instead of always injecting
 * the lot of them (reduces code duplication)
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Component
public class AllRepositories {

    @Autowired
    public ConfigRepository configs;

    @Autowired
    public LtiContextRepository contexts;

    @Autowired
    public LtiLinkRepository links;

    @Autowired
    public LtiMembershipRepository members;

    @Autowired
    public LtiResultRepository results;

    @Autowired
    public LtiUserRepository users;

    @Autowired
    public PlatformDeploymentRepository platformDeploymentRepository;

    @Autowired
    public ApiOneUseTokenRepository apiOneUseTokenRepository;

    @PersistenceContext
    public EntityManager entityManager;

    /**
     * Do NOT construct this class manually
     */
    protected AllRepositories() {
    }

}
