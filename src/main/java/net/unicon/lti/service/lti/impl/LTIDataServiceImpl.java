/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti.service.lti.impl;

import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.*;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.lti.LTI3Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Query;
import java.util.List;
import java.util.Optional;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class LTIDataServiceImpl implements LTIDataService {

    static final Logger log = LoggerFactory.getLogger(LTIDataServiceImpl.class);

    @Autowired
    AllRepositories repos;

    //This will be used to create the deep links. Needs to be in the application properties.
    @Value("${application.url}")
    private String localUrl;

    @Value("${oidc.privatekey}")
    private String ownPrivateKey;

    @Value("${oidc.publickey}")
    private String ownPublicKey;

    @Value("${lti13.demoMode:false}")
    private boolean demoMode;


    /**
     * Allows convenient access to the DAO repositories which manage the stored LTI data
     * @return the repositories access service
     */
    @Override
    public AllRepositories getRepos() {
        return repos;
    }

    @Override
    @Transactional
    //We check if we already have the information about this link in the database.
    public void loadLTIDataFromDB(LTI3Request lti, String link) {
        assert repos != null;
        lti.setLoaded(false);
        if (lti.getLtiDeploymentId() == null || lti.getAud() == null) {
            // don't even attempt this without the deployment Id, audience (client_id) or issuer, it's pointless
            log.debug("LTIload: No key to load lti.results for");
            return;
        }
        if (link == null) {
            link = lti.getLtiTargetLinkUrl().substring(lti.getLtiTargetLinkUrl().lastIndexOf("?link=") + 6);
        }

        String sqlDeployment = "SELECT k, c, l, m, u" +
                " FROM PlatformDeployment k " +
                "LEFT JOIN k.contexts c ON c.contextKey = :context " + // LtiContextEntity
                "LEFT JOIN c.links l ON l.toolLink.toolLinkId = :link  AND l.ltiLinkId = : ltiLinkId " + // LtiLinkEntity
                "LEFT JOIN c.memberships m " + // LtiMembershipEntity
                "LEFT JOIN m.user u ON u.userKey = :user " +
                " WHERE k.clientId = :clientId AND k.deploymentId = :deploymentId AND k.iss = :iss AND (m IS NULL OR (m.context = c AND m.user = u))";
        Query qDeployment = repos.entityManager.createQuery(sqlDeployment);
        qDeployment.setMaxResults(1);
        qDeployment.setParameter("clientId", lti.getAud());
        qDeployment.setParameter("deploymentId", lti.getLtiDeploymentId());
        qDeployment.setParameter("context", lti.getLtiContextId());
        qDeployment.setParameter("link", link);
        qDeployment.setParameter("ltiLinkId", lti.getLtiLinkId());
        qDeployment.setParameter("user", lti.getSub());
        qDeployment.setParameter("iss", lti.getIss());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = qDeployment.getResultList();
        if (rows == null || rows.isEmpty()) {
            log.debug("LTIload: No lti.results found for client_id: " + lti.getAud() + " and  deployment_id:" + lti.getLtiDeploymentId());
        } else {
            //If there is a result, then we load the data in the LTI request.
            // k, c, l, m, u, s, r
            Object[] row = rows.get(0);
            if (row.length > 0) {
                PlatformDeployment platformDeployment = (PlatformDeployment) row[0];
                platformDeployment.setDeploymentId(lti.getLtiDeploymentId());
                lti.setKey((PlatformDeployment) row[0]);
            }
            if (row.length > 1) lti.setContext((LtiContextEntity) row[1]);
            if (row.length > 2) lti.setLink((LtiLinkEntity) row[2]);
            if (row.length > 3) lti.setMembership((LtiMembershipEntity) row[3]);
            if (row.length > 4) lti.setUser((LtiUserEntity) row[4]);

            lti.setLoaded(true);
            log.debug("LTIload: loaded data for clientid= " + lti.getAud() + " deploymentid=" + lti.getLtiDeploymentId()
                    + " and context=" + lti.getLtiContextId() + ", complete=" + lti.isComplete());
        }
    }

    @Override
    @Transactional
    // We update the information for the context, user, membership, link (if received), etc...  with new information on the LTI Request.
    public int upsertLTIDataInDB(LTI3Request lti, PlatformDeployment platformDeployment, String link) throws DataServiceException {
        if (repos == null) {
            throw new DataServiceException("access to the repos is required");
        }
        if (platformDeployment == null)
            throw new DataServiceException("Key data must not be null to update dara");
        if (lti.getKey() == null) {
            lti.setKey(platformDeployment);
        }
        if (link == null) {
            link = lti.getLtiTargetLinkUrl().substring(lti.getLtiTargetLinkUrl().lastIndexOf("?link=") + 6);
        }
        // For the next elements, we will check if we have it already in the lti object, and if not
        // we check if it exists in the database or not.
        // if exists we get it, if not we create it.
        repos.entityManager.merge(lti.getKey());
        int inserts = 0;
        int updates = 0;
        if (lti.getContext() == null && lti.getLtiDeploymentId() != null) {
            //Context is not in the lti request at this moment. Let's see if it exists:
            LtiContextEntity ltiContextEntity = repos.contexts.findByContextKeyAndPlatformDeployment(lti.getLtiContextId(), platformDeployment);
            if (ltiContextEntity == null) {
                LtiContextEntity newContext = new LtiContextEntity(lti.getLtiContextId(), lti.getKey(), lti.getLtiContextTitle(), lti.getLtiNamesRoleServiceContextMembershipsUrl(), lti.getLtiEndpointLineItems(), null);
                lti.setContext(repos.contexts.save(newContext));
                inserts++;
                log.debug("LTIupdate: Inserted context id=" + lti.getLtiContextId());
            } else {
                //Update values from the request.
                ltiContextEntity.setTitle(lti.getLtiContextTitle());
                ltiContextEntity.setContext_memberships_url(lti.getLtiNamesRoleServiceContextMembershipsUrl());
                ltiContextEntity.setLineitems(lti.getLtiEndpointLineItems());
                lti.setContext(ltiContextEntity);
                repos.entityManager.merge(lti.getContext()); // reconnect object for this transaction
                lti.setLtiContextId(lti.getContext().getContextKey());
                log.debug("LTIupdate: Reconnected existing context id=" + lti.getLtiContextId());
            }
        } else if (lti.getContext() != null) {
            lti.getContext().setTitle(lti.getLtiContextTitle());
            lti.getContext().setContext_memberships_url(lti.getLtiNamesRoleServiceContextMembershipsUrl());
            lti.getContext().setLineitems(lti.getLtiEndpointLineItems());
            lti.setContext(repos.entityManager.merge(lti.getContext())); // reconnect object for this transaction
            lti.setLtiContextId(lti.getContext().getContextKey());
            log.debug("LTIupdate: Reconnected existing context id=" + lti.getLtiContextId());
        }

        //If we are getting a link in the url we do this, if not we skip it.
        if (lti.getLink() == null && lti.getLtiLinkId() != null) {
            //Link is not in the lti request at this moment. Let's see if it exists:
            List<LtiLinkEntity> ltiLinkEntityList = repos.links.findByToolLinkToolLinkIdAndContext(link, lti.getContext());
            boolean existingLink = false;
            for (LtiLinkEntity storedLink:ltiLinkEntityList) {
                if (storedLink.getToolLink().getToolLinkId().equals(link) && storedLink.getLtiLinkId().equals(lti.getLtiLinkId())) {
                    existingLink = true;
                }
            }
            Optional<ToolLink> toolLink = repos.toolLinks.findById(link);
            if (!existingLink) { //Then we need to add it
                if (toolLink.isPresent()) {
                    LtiLinkEntity newLink = new LtiLinkEntity(lti.getContext(), toolLink.get());
                    newLink.setLtiLinkId(lti.getLtiLinkId());
                    lti.setLink(repos.links.save(newLink));
                    inserts++;
                    log.debug("LTIupdate: Inserted link id=" + newLink.getLinkId());
                } else {
                    log.warn("LTIupdate: Link received does not exist=" + link);
                }
            }
        }

        if (lti.getUser() == null && lti.getSub() != null) {
            LtiUserEntity ltiUserEntity = repos.users.findByUserKeyAndPlatformDeployment(lti.getSub(), platformDeployment);

            if (ltiUserEntity == null) {
                LtiUserEntity newUser = new LtiUserEntity(lti.getSub(), null, platformDeployment);
                newUser.setDisplayName(lti.getLtiName());
                newUser.setEmail(lti.getLtiEmail());
                if (lti.getLtiCustom().containsKey("canvas_user_id")) {
                    newUser.setLmsUserId(lti.getLtiCustom().get("canvas_user_id").toString());
                }
                lti.setUser(repos.users.save(newUser));
                inserts++;
                log.debug("LTIupdate: Inserted user id=" + lti.getSub());
            } else {
                lti.setUser(ltiUserEntity);
                repos.entityManager.merge(lti.getUser()); // reconnect object for this transaction
                lti.setSub(lti.getUser().getUserKey());
                lti.setLtiName(lti.getUser().getDisplayName());
                lti.setLtiEmail(lti.getUser().getEmail());
                log.debug("LTIupdate: Reconnected existing user id=" + lti.getSub());
            }
        } else if (lti.getUser() != null) {
            lti.setUser(repos.entityManager.merge(lti.getUser())); // reconnect object for this transaction
            lti.setSub(lti.getUser().getUserKey());
            lti.setLtiName(lti.getUser().getDisplayName());
            lti.setLtiEmail(lti.getUser().getEmail());
            log.debug("LTIupdate: Reconnected existing user id=" + lti.getSub());
        }


        if (lti.getMembership() == null && lti.getContext() != null && lti.getUser() != null) {
            LtiMembershipEntity ltiMembershipEntity = repos.members.findByUserAndContext(lti.getUser(), lti.getContext());
            if (ltiMembershipEntity == null) {
                int roleNum = lti.makeUserRoleNum(lti.getLtiRoles()); // NOTE: do not use userRoleNumber here, it may have been overridden
                LtiMembershipEntity newMember = new LtiMembershipEntity(lti.getContext(), lti.getUser(), roleNum);
                lti.setMembership(repos.members.save(newMember));
                inserts++;
                log.debug("LTIupdate: Inserted membership id=" + newMember.getMembershipId() + ", role=" + newMember.getRole() + ", user="
                        + lti.getSub() + ", context=" + lti.getLtiContextId());
            } else {
                lti.setMembership(ltiMembershipEntity);
                repos.entityManager.merge(lti.getMembership()); // reconnect object for this transaction
                lti.setSub(lti.getUser().getUserKey());
                lti.setLtiContextId(lti.getContext().getContextKey());
                log.debug("LTIupdate: Reconnected existing membership id=" + lti.getMembership().getMembershipId());
            }
        } else if (lti.getMembership() != null) {
            lti.setMembership(repos.entityManager.merge(lti.getMembership())); // reconnect object for this transaction
            lti.setSub(lti.getUser().getUserKey());
            lti.setLtiContextId(lti.getContext().getContextKey());
            log.debug("LTIupdate: Reconnected existing membership id=" + lti.getMembership().getMembershipId());
        }

        // Next we handle updates to context_title, link_title, user_displayname, user_email, or role
        LtiContextEntity context = lti.getContext();
        if (lti.getLtiContextTitle() != null && context != null && !lti.getLtiContextTitle().equals(lti.getContext().getTitle())) {

            context.setTitle(lti.getLtiContextTitle());
            lti.setContext(repos.contexts.save(context));
            updates++;
            log.debug("LTIupdate: Updated context (id=" + lti.getContext().getContextId() + ") title=" + lti.getLtiContextTitle());
        }

        boolean userChanged = false;
        LtiUserEntity user = lti.getUser();
        if (lti.getLtiName() != null && user != null && !lti.getLtiName().equals(user.getDisplayName())) {
            user.setDisplayName(lti.getLtiName());
            userChanged = true;
        }
        if (lti.getLtiEmail() != null && user != null && !lti.getLtiEmail().equals(user.getEmail())) {
            user.setEmail(lti.getLtiEmail());
            userChanged = true;
        }
        if (lti.getLtiCustom().containsKey("canvas_user_id") && lti.getLtiCustom().get("canvas_user_id")!= null && user != null && !lti.getLtiCustom().get("canvas_user_id").toString().equals(user.getLmsUserId())) {
            user.setLmsUserId(lti.getLtiCustom().get("canvas_user_id").toString());
            userChanged = true;
        }

        if (userChanged) {
            lti.setUser(repos.users.save(user));
            updates++;
            log.debug("LTIupdate: Updated lti.user (id=" + lti.getUser().getUserKey() + ") name=" + lti.getLtiName() + ", email=" + lti.getLtiEmail());
        }

        LtiMembershipEntity membership = lti.getMembership();
        if (lti.getLtiRoles() != null && lti.getUserRoleNumber() != membership.getRole()) {
            membership.setRole(lti.getUserRoleNumber());
            lti.setMembership(repos.members.save(membership));
            updates++;
            log.debug("LTIupdate: Updated membership (id=" + lti.getMembership().getMembershipId() + ", user=" + lti.getSub() + ", context="
                    + lti.getLtiContextId() + ") roles=" + lti.getLtiRoles() + ", role=" + lti.getUserRoleNumber());
        }

        // need to recheck and see if we are complete now
        String complete;
        if (lti.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK)) {
            complete = lti.checkCompleteLTIRequest();
        } else {
            complete = lti.checkCompleteDeepLinkingRequest();
        }
        if (!complete.equals("true")) {
            throw new DataServiceException("LTI object is incomplete: " + complete);
        }
        lti.setLoadingUpdates(inserts + updates);
        lti.setUpdated(true);
        log.debug("LTIupdate: changes=" + lti.getLoadingUpdates() + ", inserts=" + inserts + ", updates=" + updates);
        return lti.getLoadingUpdates();
    }

    @Override
    public LtiUserEntity findByUserKeyAndPlatformDeployment(String userKey, PlatformDeployment platformDeployment) {
        return repos.users.findByUserKeyAndPlatformDeployment(userKey,platformDeployment);
    }

    @Override
    public LtiUserEntity saveLtiUserEntity(LtiUserEntity ltiUserEntity) {
        return repos.users.save(ltiUserEntity);
    }

    @Override
    public LtiMembershipEntity findByUserAndContext(LtiUserEntity ltiUserEntity, LtiContextEntity ltiContextEntity) {
        return repos.members.findByUserAndContext(ltiUserEntity,ltiContextEntity);
    }

    @Override
    public LtiMembershipEntity saveLtiMembershipEntity(LtiMembershipEntity ltiMembershipEntity) {
        return repos.members.save(ltiMembershipEntity);
    }

    @Override
    public String getLocalUrl() {
        return localUrl;
    }

    @Override
    public void setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
    }

    @Override
    public String getOwnPrivateKey() {
        return ownPrivateKey;
    }

    @Override
    public void setOwnPrivateKey(String ownPrivateKey) {
        this.ownPrivateKey = ownPrivateKey;
    }

    @Override
    public String getOwnPublicKey() {
        return ownPublicKey;
    }

    @Override
    public void setOwnPublicKey(String ownPublicKey) {
        this.ownPublicKey = ownPublicKey;
    }

    @Override
    public Boolean getDemoMode() {
        return demoMode;
    }

    @Override
    public void setDemoMode(Boolean demoMode) {
        this.demoMode = demoMode;
    }
}
