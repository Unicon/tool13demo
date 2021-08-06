package net.unicon.lti.service.app;

import net.unicon.lti.repository.AllRepositories;

public interface APIDataService {
    AllRepositories getRepos();

    void addOneUseToken(String token);

    boolean findAndDeleteOneUseToken(String token);

    void cleanOldTokens();
}
