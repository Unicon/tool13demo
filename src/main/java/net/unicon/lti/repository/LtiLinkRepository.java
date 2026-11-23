package net.unicon.lti.repository;

import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface LtiLinkRepository extends JpaRepository<LtiLinkEntity, String> {

    List<LtiLinkEntity> findByToolLinkToolLinkId(String toolLinkId);

    List<LtiLinkEntity> findByToolLinkToolLinkIdAndContext(String toolLinkId, LtiContextEntity context);

    List<LtiLinkEntity> findByLtiLinkIdAndToolLinkToolLinkIdAndContext(String ltiLinkId, String toolLinkId, LtiContextEntity context);


}
