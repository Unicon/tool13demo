package net.unicon.lti.repository;

import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiContextRepository extends JpaRepository<LtiContextEntity, Long> {

    LtiContextEntity findByContextKey(String key);

    LtiContextEntity findByContextKeyAndPlatformDeployment(String contextKey, PlatformDeployment platformDeployment);

    List<LtiContextEntity> findByPlatformDeployment(PlatformDeployment platformDeployment);

    int deleteByContextKeyAndPlatformDeployment(String contextKey, PlatformDeployment platformDeployment);
}
