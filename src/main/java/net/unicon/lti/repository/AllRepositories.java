package net.unicon.lti.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Special service to give access to all the repositories in one place
 * <p/>
 * This is just here to make it a little easier to get access to the full set of repositories instead of always injecting
 * the lot of them (reduces code duplication)
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Component
@RequiredArgsConstructor
public class AllRepositories {

    public final ConfigRepository configs;

    public final LtiContextRepository contexts;

    public final LtiLinkRepository links;

    public final ToolLinkRepository toolLinks;
    public final LtiMembershipRepository members;

    public final LtiResultRepository results;

    public final LtiUserRepository users;

    public final PlatformDeploymentRepository platformDeploymentRepository;

    public final ApiOneUseTokenRepository apiOneUseTokenRepository;

    public final NonceStateRepository nonceStateRepository;

    @PersistenceContext
    public EntityManager entityManager;

}
