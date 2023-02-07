package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiMembershipEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.utils.lti.LTI3Request;
import org.springframework.transaction.annotation.Transactional;

public interface LTIDataService {
    AllRepositories getRepos();

    @Transactional
        //We check if we already have the information about this link in the database.
    boolean loadLTIDataFromDB(LTI3Request lti, String link);

    @Transactional
        // We update the information for the context, user, membership, link (if received), etc...  with new information on the LTI Request.
    int upsertLTIDataInDB(LTI3Request lti, PlatformDeployment platformDeployment, String link) throws DataServiceException;

    LtiUserEntity findByUserKeyAndPlatformDeployment(String userKey, PlatformDeployment platformDeployment);

    LtiUserEntity saveLtiUserEntity(LtiUserEntity ltiUserEntity);

    LtiMembershipEntity findByUserAndContext(LtiUserEntity ltiUserEntity, LtiContextEntity ltiContextEntity);

    LtiMembershipEntity saveLtiMembershipEntity(LtiMembershipEntity ltiMembershipEntity);

    String getLocalUrl();

    void setLocalUrl(String localUrl);

    String getDomainUrl();

    void setDomainUrl(String domainUrl);

    String getOwnPrivateKey();

    void setOwnPrivateKey(String ownPrivateKey);

    String getOwnPublicKey();

    void setOwnPublicKey(String ownPublicKey);

    boolean getDemoMode();

    void setDemoMode(boolean demoMode);

    boolean getDeepLinkingEnabled();

    void setDeepLinkingEnabled(boolean deepLinkingEnabled);

    boolean getEnableMockValkyrie();

    void setEnableMockValkyrie(boolean enableMockValkyrie);
}
