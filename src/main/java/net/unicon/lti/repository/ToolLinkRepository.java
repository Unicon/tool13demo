package net.unicon.lti.repository;

import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiLinkEntity;
import net.unicon.lti.model.ToolLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ToolLinkRepository extends JpaRepository<ToolLink, String> {

}
