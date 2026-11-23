package net.unicon.lti.repository;

import net.unicon.lti.model.PlatformDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface PlatformDeploymentRepository extends JpaRepository<PlatformDeployment, Long> {

    List<PlatformDeployment> findByIss(String iss);

    List<PlatformDeployment> findByClientId(String clientId);

    List<PlatformDeployment> findByDeploymentId(String deploymentId);

    List<PlatformDeployment> findByIssAndClientId(String iss, String clientId);

    List<PlatformDeployment> findByIssAndDeploymentId(String iss, String deploymentId);

    List<PlatformDeployment> findByIssAndClientIdAndDeploymentId(String iss, String clientId, String deploymentId);
}
