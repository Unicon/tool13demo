package net.unicon.lti.service.app.impl;

import lombok.RequiredArgsConstructor;
import net.unicon.lti.model.ApiOneUseToken;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.service.app.APIDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * This manages all the access to data related with the main APP
 */
@Service
@RequiredArgsConstructor
public class APIDataServiceImpl implements APIDataService {

    static final Logger log = LoggerFactory.getLogger(APIDataServiceImpl.class);

    private final AllRepositories repos;

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
