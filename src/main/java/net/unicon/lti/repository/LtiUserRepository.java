package net.unicon.lti.repository;

import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface LtiUserRepository extends JpaRepository<LtiUserEntity, Long> {

    LtiUserEntity findByUserKeyAndPlatformDeployment(String linkKey, PlatformDeployment platformDeployment);

    LtiUserEntity findByUserKeyAndPlatformDeployment_KeyId(String userKey, long keyId);

    LtiUserEntity findByUserIdAndPlatformDeployment_KeyId(long userId, long keyId);

    LtiUserEntity findByUserId(long userId);

    LtiUserEntity findByUserKey(String userKey);


}
