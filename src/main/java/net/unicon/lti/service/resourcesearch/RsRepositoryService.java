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
package net.unicon.lti.service.resourcesearch;

import net.unicon.lti.model.resourcesearch.RsSubjectEntity;
import net.unicon.lti.repository.RsSubjectRepository;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class RsRepositoryService {

    @Resource
    private RsSubjectRepository rsSubjects;

    private static final Map<Class<?>, Repository<?, ?>> repos = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
        repos.put(RsSubjectEntity.class, rsSubjects);
    }

    public static Map<Class<?>, Repository<?, ?>> getRepos() {
        return repos;
    }
}
