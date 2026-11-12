package net.unicon.lti.repository;

import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiMembershipEntity;
import net.unicon.lti.model.LtiUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface LtiMembershipRepository extends JpaRepository<LtiMembershipEntity, Long> {

    LtiMembershipEntity findByUserAndContext(LtiUserEntity user, LtiContextEntity context);

}
