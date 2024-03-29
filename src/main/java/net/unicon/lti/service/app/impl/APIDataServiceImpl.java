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
package net.unicon.lti.service.app.impl;

import net.unicon.lti.model.ApiOneUseToken;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.service.app.APIDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * This manages all the access to data related with the main APP
 */
@Service
public class APIDataServiceImpl implements APIDataService {

    static final Logger log = LoggerFactory.getLogger(APIDataServiceImpl.class);

    @Autowired
    AllRepositories repos;

    /**
     * Allows convenient access to the DAO repositories which manage the stored data
     * @return the repositories access service
     */
    @Override
    public AllRepositories getRepos() {
        return repos;
    }

    @Override
    public void addOneUseToken(String token){
        repos.apiOneUseTokenRepository.save(new ApiOneUseToken(token));
    }

    @Override
    public boolean findAndDeleteOneUseToken(String token){
        ApiOneUseToken apiOneUseToken = repos.apiOneUseTokenRepository.findByToken(token);
        if (apiOneUseToken!=null){
            repos.apiOneUseTokenRepository.delete(apiOneUseToken);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void cleanOldTokens() {
        repos.apiOneUseTokenRepository.deleteByCreatedAtBefore(new Date(System.currentTimeMillis()-24*60*60*1000));
    }

}
