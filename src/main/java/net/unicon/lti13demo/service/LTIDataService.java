/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.service;

import net.unicon.lti13demo.model.LtiContextEntity;
import net.unicon.lti13demo.model.LtiLinkEntity;
import net.unicon.lti13demo.model.LtiMembershipEntity;
import net.unicon.lti13demo.model.LtiUserEntity;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.repository.AllRepositories;
import net.unicon.lti13demo.utils.lti.LTI3Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Query;
import java.util.List;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Component
public class LTIDataService {

    static final Logger log = LoggerFactory.getLogger(LTIDataService.class);

    @Autowired
    AllRepositories repos;

    /**
     * Allows convenient access to the DAO repositories which manage the stored LTI data
     * @return the repositories access service
     */
    public AllRepositories getRepos() {
        return repos;
    }

    @Transactional
    public boolean loadLTIDataFromDB(LTI3Request lti) {
        assert repos != null;
        lti.setLoaded(false);
        if (lti.getLtiDeploymentId() == null || lti.getAud() == null) {
            // don't even attempt this without the deployment Id, audience (client_id) or issuer, it's pointless
            log.info("LTIload: No key to load lti.results for");
            return false;
        }

        StringBuilder sbDeployment = new StringBuilder();
        StringBuilder sbNoDeployment = new StringBuilder();
        sbDeployment.append("SELECT k, c, l, m, u");
        sbNoDeployment.append("SELECT k, c, l, m, u");

        sbDeployment.append(" FROM PlatformDeployment k " +
                "LEFT JOIN k.contexts c ON c.contextKey = :context " + // LtiContextEntity
                "LEFT JOIN c.links l ON l.linkKey = :link " + // LtiLinkEntity
                "LEFT JOIN c.memberships m " + // LtiMembershipEntity
                "LEFT JOIN m.user u ON u.userKey = :user "
        );

        sbNoDeployment.append(" FROM PlatformDeployment k " +
                "LEFT JOIN k.contexts c ON c.contextKey = :context " + // LtiContextEntity
                "LEFT JOIN c.links l ON l.linkKey = :link " + // LtiLinkEntity
                "LEFT JOIN c.memberships m " + // LtiMembershipEntity
                "LEFT JOIN m.user u ON u.userKey = :user "
        );

        sbDeployment.append(" WHERE k.clientId = :clientId AND k.deploymentId = :deploymentId AND k.iss = :iss AND (m IS NULL OR (m.context = c AND m.user = u))");
        sbNoDeployment.append(" WHERE k.clientId = :clientId AND k.iss = :iss AND (m IS NULL OR (m.context = c AND m.user = u))");

        String sqlDeployment = sbDeployment.toString();
        String sqlNoDeployment = sbDeployment.toString();
        Query qDeployment = repos.entityManager.createQuery(sqlDeployment);
        qDeployment.setMaxResults(1);
        qDeployment.setParameter("clientId", lti.getAud());
        qDeployment.setParameter("deploymentId", lti.getLtiDeploymentId());
        qDeployment.setParameter("context", lti.getLtiContextId());
        qDeployment.setParameter("link",lti.getLtiLinkId());
        qDeployment.setParameter("user", lti.getSub());
        qDeployment.setParameter("iss", lti.getIss());

        Query qNoDeployment = repos.entityManager.createQuery(sqlNoDeployment);
        qNoDeployment.setMaxResults(1);
        qNoDeployment.setParameter("clientId", lti.getAud());
        qNoDeployment.setParameter("context", lti.getLtiContextId());
        qNoDeployment.setParameter("link",lti.getLtiLinkId());
        qNoDeployment.setParameter("user", lti.getSub());
        qNoDeployment.setParameter("iss", lti.getIss());


        @SuppressWarnings("unchecked")
        List<Object[]> rows = qDeployment.getResultList();
        if (rows == null || rows.isEmpty()) {
            log.info("LTIload: No lti.results found for client_id: " + lti.getAud() + " and  deployment_id:" + lti.getLtiDeploymentId()
                    + ". Let's try only with the clientId");
            rows = qNoDeployment.getResultList();
        }
        if (rows == null || rows.isEmpty()) {
            log.info("LTIload: No lti.results found for client_id: " + lti.getAud());
        } else {
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

            // check if the loading lti.resulted in a complete set of LTI data
            lti.checkCompleteLTIRequest();
            lti.setLoaded(true);
            log.info("LTIload: loaded data for clientid= " + lti.getAud() + " deploymentid=" + lti.getLtiDeploymentId()
                    + " and context=" + lti.getLtiContextId() + ", complete=" + lti.isComplete());
        }
        return lti.isLoaded();
    }

    @Transactional
    public int updateLTIDataInDB(LTI3Request lti) {
        assert repos != null : "access to the repos is required";
        assert lti.isLoaded() : "Data must be loaded before it can be updated";

        assert lti.getKey() != null : "Key data must not be null to update data";
        repos.entityManager.merge(lti.getKey()); // reconnect the key object for this transaction

        int inserts = 0;
        int updates = 0;
        if (lti.getContext() == null && lti.getLtiDeploymentId() != null) {
            LtiContextEntity newContext = new LtiContextEntity(lti.getLtiContextId(), lti.getKey(), lti.getLtiContextTitle(), null);
            lti.setContext(repos.contexts.save(newContext));
            inserts++;
            log.info("LTIupdate: Inserted context id=" + lti.getLtiContextId());
        } else if (lti.getContext() != null) {
            repos.entityManager.merge(lti.getContext()); // reconnect object for this transaction
            lti.setLtiContextId(lti.getContext().getContextKey());
            log.info("LTIupdate: Reconnected existing context id=" + lti.getLtiContextId());
        }

        if (lti.getLink() == null && lti.getLtiLinkId() != null) {
            LtiLinkEntity newLink = new LtiLinkEntity(lti.getLtiLinkId(), lti.getContext(), lti.getLtiLinkTitle(),null);
            lti.setLink(repos.links.save(newLink));
            inserts++;
            log.info("LTIupdate: Inserted link id=" + lti.getLtiLinkId());
        } else if (lti.getLink() != null) {
            repos.entityManager.merge(lti.getLink()); // reconnect object for this transaction
            lti.setLtiLinkId(lti.getLink().getLinkKey());
            log.info("LTIupdate: Reconnected existing link id=" + lti.getLtiLinkId());
        }

        if (lti.getUser() == null && lti.getSub() != null) {
            LtiUserEntity newUser = new LtiUserEntity(lti.getSub(), null);
            newUser.setDisplayName(lti.getLtiName());
            newUser.setEmail(lti.getLtiEmail());
            lti.setUser(repos.users.save(newUser));
            inserts++;
            log.info("LTIupdate: Inserted user id=" + lti.getSub());
        } else if (lti.getUser() != null) {
            repos.entityManager.merge(lti.getUser()); // reconnect object for this transaction
            lti.setSub(lti.getUser().getUserKey());
            lti.setLtiName(lti.getUser().getDisplayName());
            lti.setLtiEmail(lti.getUser().getEmail());
            log.info("LTIupdate: Reconnected existing user id=" + lti.getSub());
        }



        if (lti.getMembership() == null && lti.getContext() != null && lti.getUser() != null) {
            int roleNum = lti.makeUserRoleNum(lti.getLtiRoles()); // NOTE: do not use userRoleNumber here, it may have been overridden
            LtiMembershipEntity newMember = new LtiMembershipEntity(lti.getContext(), lti.getUser(), roleNum);
            lti.setMembership(repos.members.save(newMember));
            inserts++;
            log.info("LTIupdate: Inserted membership id=" + newMember.getMembershipId() + ", role=" + newMember.getRole() + ", user="
                    + lti.getSub() + ", context=" + lti.getLtiContextId());
        } else if (lti.getMembership() != null) {
            repos.entityManager.merge(lti.getMembership()); // reconnect object for this transaction
            lti.setSub(lti.getUser().getUserKey());
            lti.setLtiContextId(lti.getContext().getContextKey());
            log.info("LTIupdate: Reconnected existing membership id=" + lti.getMembership().getMembershipId());
        }

        // Next we handle updates to context_title, link_title, user_displayname, user_email, or role
        LtiContextEntity context = lti.getContext();
        if (lti.getLtiContextTitle() != null && context != null && !lti.getLtiContextTitle().equals(lti.getContext().getTitle())) {

            context.setTitle(lti.getLtiContextTitle());
            lti.setContext(repos.contexts.save(context));
            updates++;
            log.info("LTIupdate: Updated context (id=" + lti.getContext().getContextId() + ") title=" + lti.getLtiContextTitle());
        }
        LtiLinkEntity link = lti.getLink();
        if (lti.getLtiLinkTitle() != null && link != null && !lti.getLtiLinkTitle().equals(link.getTitle())) {
            link.setTitle(lti.getLtiLinkTitle());
            lti.setLink(repos.links.save(link));
            updates++;
            log.info("LTIupdate: Updated link (id=" + lti.getLink().getLinkKey() + ") title=" + lti.getLtiLinkTitle());
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
        if (userChanged) {
            lti.setUser(repos.users.save(user));
            updates++;
            log.info("LTIupdate: Updated lti.user (id=" + lti.getUser().getUserKey() + ") name=" + lti.getLtiName() + ", email=" + lti.getLtiEmail());
        }

        LtiMembershipEntity membership = lti.getMembership();
        if (lti.getLtiRoles() != null && lti.getUserRoleNumber() != membership.getRole()) {
            membership.setRole(lti.getUserRoleNumber());
            lti.setMembership(repos.members.save(membership));
            updates++;
            log.info("LTIupdate: Updated membership (id=" + lti.getMembership().getMembershipId() + ", user=" + lti.getSub() + ", context="
                    + lti.getLtiContextId() + ") roles=" + lti.getLtiRoles() + ", role=" + lti.getUserRoleNumber());
        }

        // need to recheck and see if we are complete now
        lti.checkCompleteLTIRequest(true);

        lti.setLoadingUpdates(inserts + updates);
        lti.setUpdated(true);
        log.info("LTIupdate: changes=" + lti.getLoadingUpdates() + ", inserts=" + inserts + ", updates=" + updates);
        return lti.getLoadingUpdates();
    }

}
