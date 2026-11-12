package net.unicon.lti.service.lti;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.LtiLinkEntity;
import net.unicon.lti.model.LtiMembershipEntity;
import net.unicon.lti.model.LtiUserEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ToolLink;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.ApiOneUseTokenRepository;
import net.unicon.lti.repository.ConfigRepository;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.LtiLinkRepository;
import net.unicon.lti.repository.LtiMembershipRepository;
import net.unicon.lti.repository.LtiResultRepository;
import net.unicon.lti.repository.LtiUserRepository;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.repository.ToolLinkRepository;
import net.unicon.lti.service.lti.impl.LTIDataServiceImpl;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objenesis.ObjenesisStd;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LTIDataServiceImplTest {

    private static final String LINK = "tool-link-1";
    private static final String LTI_LINK_ID = "resource-link-1";
    private static final String CONTEXT_KEY = "context-1";
    private static final String SUB = "user-1";

    @Mock private ConfigRepository configs;
    @Mock private LtiContextRepository contexts;
    @Mock private LtiLinkRepository links;
    @Mock private ToolLinkRepository toolLinks;
    @Mock private LtiMembershipRepository members;
    @Mock private LtiResultRepository results;
    @Mock private LtiUserRepository users;
    @Mock private PlatformDeploymentRepository platformDeploymentRepository;
    @Mock private ApiOneUseTokenRepository apiOneUseTokenRepository;
    @Mock private NonceStateRepository nonceStateRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    private AllRepositories repos;
    private LTIDataServiceImpl service;
    private PlatformDeployment platformDeployment;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repos = new AllRepositories(configs, contexts, links, toolLinks, members, results, users,
                platformDeploymentRepository, apiOneUseTokenRepository, nonceStateRepository);
        repos.entityManager = entityManager;
        service = new LTIDataServiceImpl(repos);
        platformDeployment = new PlatformDeployment();
        platformDeployment.setKeyId(1L);
        platformDeployment.setDeploymentId("deployment-1");
        when(entityManager.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(contexts.save(any(LtiContextEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(links.save(any(LtiLinkEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.save(any(LtiUserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(members.save(any(LtiMembershipEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(links.findByToolLinkToolLinkIdAndContext(anyString(), any())).thenReturn(new ArrayList<>());
        when(toolLinks.findById(anyString())).thenReturn(Optional.empty());
    }

    private static LTI3Request newLti() {
        LTI3Request lti = new ObjenesisStd().newInstance(LTI3Request.class);
        Map<String, Object> resourceLink = new HashMap<>();
        resourceLink.put("id", LTI_LINK_ID);
        lti.setLtiResourceLink(resourceLink);
        lti.setLtiLinkId(LTI_LINK_ID);
        lti.setLtiDeploymentId("deployment-1");
        lti.setAud("client-1");
        lti.setIss("https://platform.example.com");
        lti.setSub(SUB);
        lti.setLtiContextId(CONTEXT_KEY);
        lti.setLtiContextTitle("Course title");
        lti.setLtiNamesRoleServiceContextMembershipsUrl("https://platform.example.com/memberships");
        lti.setLtiEndpointLineItems("https://platform.example.com/lineitems");
        lti.setLtiName("Jane Doe");
        lti.setLtiEmail("jane@example.com");
        lti.setLtiRoles(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR));
        lti.setUserRoleNumber(1);
        lti.setLtiCustom(new HashMap<>());
        lti.setExp(new Date());
        lti.setIat(new Date());
        lti.setLtiMessageType(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        lti.setLtiTargetLinkUrl("https://tool.example.com/lti3?link=" + LINK);
        return lti;
    }

    private static LtiContextEntity context(String title) {
        LtiContextEntity context = new LtiContextEntity(CONTEXT_KEY, new PlatformDeployment(), title, null);
        context.setContextId(10L);
        return context;
    }

    private static LtiUserEntity user(String name, String email, String lmsUserId) {
        LtiUserEntity user = new LtiUserEntity(SUB, null, null);
        user.setDisplayName(name);
        user.setEmail(email);
        user.setLmsUserId(lmsUserId);
        return user;
    }

    private static LtiLinkEntity link(String toolLinkId, String ltiLinkId) {
        LtiLinkEntity link = new LtiLinkEntity(context("link context"), new ToolLink(toolLinkId, "title", "desc", false, 1f));
        link.setLtiLinkId(ltiLinkId);
        return link;
    }

    // loadLTIDataFromDB

    @Test
    public void loadSkipsWithoutDeploymentId() {
        LTI3Request lti = newLti();
        lti.setLoaded(true);
        lti.setLtiDeploymentId(null);
        service.loadLTIDataFromDB(lti, LINK);
        assertFalse(lti.isLoaded());
        verifyNoInteractions(entityManager);
    }

    @Test
    public void loadSkipsWithoutAudience() {
        LTI3Request lti = newLti();
        lti.setAud(null);
        service.loadLTIDataFromDB(lti, LINK);
        assertFalse(lti.isLoaded());
        verifyNoInteractions(entityManager);
    }

    @Test
    public void loadRequiresRepos() {
        LTIDataServiceImpl noRepos = new LTIDataServiceImpl(null);
        assertThrows(AssertionError.class, () -> noRepos.loadLTIDataFromDB(newLti(), LINK));
    }

    @Test
    public void loadWithNullResultsLeavesRequestUnloaded() {
        when(query.getResultList()).thenReturn(null);
        LTI3Request lti = newLti();
        service.loadLTIDataFromDB(lti, LINK);
        assertFalse(lti.isLoaded());
        verify(query).setMaxResults(1);
        verify(query).setParameter("clientId", "client-1");
        verify(query).setParameter("deploymentId", "deployment-1");
        verify(query).setParameter("context", CONTEXT_KEY);
        verify(query).setParameter("link", LINK);
        verify(query).setParameter("ltiLinkId", LTI_LINK_ID);
        verify(query).setParameter("user", SUB);
        verify(query).setParameter("iss", "https://platform.example.com");
    }

    @Test
    public void loadWithEmptyResultsLeavesRequestUnloaded() {
        when(query.getResultList()).thenReturn(new ArrayList<>());
        LTI3Request lti = newLti();
        service.loadLTIDataFromDB(lti, LINK);
        assertFalse(lti.isLoaded());
    }

    @Test
    public void loadDerivesLinkFromTargetLinkUrlAndPopulatesEverything() {
        PlatformDeployment found = new PlatformDeployment();
        found.setDeploymentId("stale");
        LtiContextEntity context = context("Course title");
        LtiLinkEntity link = link(LINK, LTI_LINK_ID);
        LtiUserEntity user = user("Jane Doe", "jane@example.com", null);
        LtiMembershipEntity membership = new LtiMembershipEntity(context, user, 1);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{found, context, link, membership, user});
        when(query.getResultList()).thenReturn(rows);

        LTI3Request lti = newLti();
        service.loadLTIDataFromDB(lti, null);

        verify(query).setParameter("link", LINK);
        assertTrue(lti.isLoaded());
        assertSame(found, lti.getKey());
        assertEquals("deployment-1", found.getDeploymentId());
        assertSame(context, lti.getContext());
        assertSame(link, lti.getLink());
        assertSame(membership, lti.getMembership());
        assertSame(user, lti.getUser());
    }

    @Test
    public void loadWithEmptyRowStillMarksLoaded() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[0]);
        when(query.getResultList()).thenReturn(rows);
        LTI3Request lti = newLti();
        service.loadLTIDataFromDB(lti, LINK);
        assertTrue(lti.isLoaded());
        assertNull(lti.getKey());
        assertNull(lti.getContext());
        assertNull(lti.getLink());
        assertNull(lti.getMembership());
        assertNull(lti.getUser());
    }

    // upsertLTIDataInDB guards

    @Test
    public void upsertRequiresRepos() {
        LTIDataServiceImpl noRepos = new LTIDataServiceImpl(null);
        DataServiceException ex = assertThrows(DataServiceException.class, () -> noRepos.upsertLTIDataInDB(newLti(), platformDeployment, LINK));
        assertEquals("access to the repos is required", ex.getMessage());
    }

    @Test
    public void upsertRequiresPlatformDeployment() {
        DataServiceException ex = assertThrows(DataServiceException.class, () -> service.upsertLTIDataInDB(newLti(), null, LINK));
        assertEquals("Key data must not be null to update dara", ex.getMessage());
    }

    // upsertLTIDataInDB inserts

    @Test
    public void upsertInsertsContextLinkUserAndMembershipForANewLaunch() throws DataServiceException {
        ToolLink toolLink = new ToolLink(LINK, "title", "desc", false, 1f);
        when(toolLinks.findById(LINK)).thenReturn(Optional.of(toolLink));
        // one stored link with another tool link, one with the same tool link but another resource link id
        when(links.findByToolLinkToolLinkIdAndContext(eq(LINK), any())).thenReturn(List.of(link("other", LTI_LINK_ID), link(LINK, "other")));
        LTI3Request lti = newLti();
        lti.getLtiCustom().put("canvas_user_id", 386);

        int changes = service.upsertLTIDataInDB(lti, platformDeployment, null);

        assertEquals(4, changes);
        assertEquals(4, lti.getLoadingUpdates());
        assertTrue(lti.isUpdated());
        assertSame(platformDeployment, lti.getKey());
        verify(entityManager).merge(platformDeployment);

        ArgumentCaptor<LtiContextEntity> contextCaptor = ArgumentCaptor.forClass(LtiContextEntity.class);
        verify(contexts).save(contextCaptor.capture());
        assertEquals(CONTEXT_KEY, contextCaptor.getValue().getContextKey());
        assertEquals("Course title", contextCaptor.getValue().getTitle());
        assertEquals("https://platform.example.com/memberships", contextCaptor.getValue().getContext_memberships_url());
        assertEquals("https://platform.example.com/lineitems", contextCaptor.getValue().getLineitems());
        assertSame(platformDeployment, contextCaptor.getValue().getPlatformDeployment());

        ArgumentCaptor<LtiLinkEntity> linkCaptor = ArgumentCaptor.forClass(LtiLinkEntity.class);
        verify(links).save(linkCaptor.capture());
        assertSame(toolLink, linkCaptor.getValue().getToolLink());
        assertEquals(LTI_LINK_ID, linkCaptor.getValue().getLtiLinkId());
        assertSame(linkCaptor.getValue(), lti.getLink());

        ArgumentCaptor<LtiUserEntity> userCaptor = ArgumentCaptor.forClass(LtiUserEntity.class);
        verify(users).save(userCaptor.capture());
        assertEquals(SUB, userCaptor.getValue().getUserKey());
        assertEquals("Jane Doe", userCaptor.getValue().getDisplayName());
        assertEquals("jane@example.com", userCaptor.getValue().getEmail());
        assertEquals("386", userCaptor.getValue().getLmsUserId());

        ArgumentCaptor<LtiMembershipEntity> memberCaptor = ArgumentCaptor.forClass(LtiMembershipEntity.class);
        verify(members).save(memberCaptor.capture());
        assertEquals(1, memberCaptor.getValue().getRole());
        assertSame(lti.getUser(), memberCaptor.getValue().getUser());
        assertSame(lti.getContext(), memberCaptor.getValue().getContext());
    }

    @Test
    public void upsertSkipsUnknownToolLinkAndNewUserWithoutCanvasId() throws DataServiceException {
        LTI3Request lti = newLti();
        lti.setKey(platformDeployment);

        int changes = service.upsertLTIDataInDB(lti, new PlatformDeployment(), LINK);

        assertEquals(3, changes);
        assertSame(platformDeployment, lti.getKey());
        assertNull(lti.getLink());
        verify(links, never()).save(any());
        ArgumentCaptor<LtiUserEntity> userCaptor = ArgumentCaptor.forClass(LtiUserEntity.class);
        verify(users).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getLmsUserId());
    }

    @Test
    public void upsertSkipsTheLinkWhenTheLaunchNamesNone() throws DataServiceException {
        // a course navigation launch: the target link URI has no link parameter
        LTI3Request lti = newLti();
        lti.setKey(platformDeployment);
        lti.setLtiTargetLinkUrl("https://tool.example.com/lti3");

        service.upsertLTIDataInDB(lti, new PlatformDeployment(), null);

        assertNull(lti.getLink());
        verify(toolLinks, never()).findById(anyString());
        verify(links, never()).findByToolLinkToolLinkIdAndContext(anyString(), any());
        verify(links, never()).save(any());
    }

    @Test
    public void upsertDoesNotDuplicateAnExistingLink() throws DataServiceException {
        when(toolLinks.findById(LINK)).thenReturn(Optional.of(new ToolLink(LINK, "title", "desc", false, 1f)));
        when(links.findByToolLinkToolLinkIdAndContext(eq(LINK), any())).thenReturn(List.of(link(LINK, LTI_LINK_ID)));
        LTI3Request lti = newLti();

        int changes = service.upsertLTIDataInDB(lti, platformDeployment, LINK);

        assertEquals(3, changes);
        verify(links, never()).save(any());
        assertNull(lti.getLink());
    }

    // upsertLTIDataInDB reconnects existing rows from the repositories

    @Test
    public void upsertReconnectsExistingRowsFoundInTheRepositories() throws DataServiceException {
        LtiContextEntity storedContext = context("Old title");
        storedContext.setContextKey("stored-context");
        when(contexts.findByContextKeyAndPlatformDeployment(CONTEXT_KEY, platformDeployment)).thenReturn(storedContext);
        LtiUserEntity storedUser = user("Stored Name", "stored@example.com", "386");
        storedUser.setUserKey("stored-user");
        when(users.findByUserKeyAndPlatformDeployment(SUB, platformDeployment)).thenReturn(storedUser);
        LtiMembershipEntity storedMembership = new LtiMembershipEntity(storedContext, storedUser, 1);
        when(members.findByUserAndContext(storedUser, storedContext)).thenReturn(storedMembership);
        LTI3Request lti = newLti();
        lti.setLink(link(LINK, LTI_LINK_ID));

        int changes = service.upsertLTIDataInDB(lti, platformDeployment, LINK);

        // stored values win over the request for the user, so nothing else changes
        assertEquals(0, changes);
        assertSame(storedContext, lti.getContext());
        assertEquals("Course title", storedContext.getTitle());
        assertEquals("https://platform.example.com/memberships", storedContext.getContext_memberships_url());
        assertEquals("https://platform.example.com/lineitems", storedContext.getLineitems());
        assertEquals("stored-context", lti.getLtiContextId());
        assertSame(storedUser, lti.getUser());
        assertEquals("stored-user", lti.getSub());
        assertEquals("Stored Name", lti.getLtiName());
        assertEquals("stored@example.com", lti.getLtiEmail());
        assertSame(storedMembership, lti.getMembership());
        verify(entityManager).merge(storedContext);
        verify(entityManager).merge(storedUser);
        verify(entityManager).merge(storedMembership);
        verify(contexts, never()).save(any());
        verify(users, never()).save(any());
        verify(members, never()).save(any());
        verify(links, never()).findByToolLinkToolLinkIdAndContext(anyString(), any());
    }

    // upsertLTIDataInDB with entities already on the request

    @Test
    public void upsertMergesEntitiesAlreadyOnTheRequestAndAppliesChanges() throws DataServiceException {
        LTI3Request lti = newLti();
        LtiContextEntity context = context("Old title");
        lti.setContext(context);
        lti.setLink(link(LINK, LTI_LINK_ID));
        LtiUserEntity user = user("Jane Doe", "jane@example.com", "1");
        lti.setUser(user);
        LtiMembershipEntity membership = new LtiMembershipEntity(context, user, 0);
        lti.setMembership(membership);
        lti.getLtiCustom().put("canvas_user_id", 2);

        int changes = service.upsertLTIDataInDB(lti, platformDeployment, LINK);

        // canvas id changed on the user, and the membership role changed from 0 to 1
        assertEquals(2, changes);
        assertEquals("Course title", context.getTitle());
        assertEquals(CONTEXT_KEY, lti.getLtiContextId());
        assertEquals(SUB, lti.getSub());
        assertEquals("2", user.getLmsUserId());
        assertEquals(1, membership.getRole());
        verify(users).save(user);
        verify(members).save(membership);
        verify(contexts, never()).save(any());
        verify(entityManager).merge(context);
        verify(entityManager).merge(user);
        verify(entityManager).merge(membership);
        verify(links, never()).findByToolLinkToolLinkIdAndContext(anyString(), any());
    }

    @Test
    public void upsertSavesContextTitleWhenMergeReturnsAStaleCopy() throws DataServiceException {
        LTI3Request lti = newLti();
        LtiContextEntity context = context("Old title");
        LtiContextEntity managed = context("Managed title");
        when(entityManager.merge(context)).thenReturn(managed);
        lti.setContext(context);
        lti.setLink(link(LINK, LTI_LINK_ID));

        int changes = service.upsertLTIDataInDB(lti, platformDeployment, LINK);

        // inserts: user and membership; updates: context title
        assertEquals(3, changes);
        assertEquals("Course title", managed.getTitle());
        verify(contexts).save(managed);
    }

    @Test
    public void upsertUpdatesUserNameAndEmailWhenTheyChange() throws DataServiceException {
        LTI3Request lti = newLti();
        lti.setContext(context("Course title"));
        LtiUserEntity user = user("Old Name", "old@example.com", null);
        LtiUserEntity managed = user("Managed Name", "managed@example.com", null);
        when(entityManager.merge(user)).thenReturn(managed);
        lti.setUser(user);
        lti.setLink(link(LINK, LTI_LINK_ID));
        // merging copies the managed name and email onto the request, so set them back afterwards
        when(members.save(any(LtiMembershipEntity.class))).thenAnswer(invocation -> {
            lti.setLtiName("New Name");
            lti.setLtiEmail("new@example.com");
            return invocation.getArgument(0);
        });

        int changes = service.upsertLTIDataInDB(lti, platformDeployment, LINK);

        assertEquals(2, changes);
        assertEquals("New Name", managed.getDisplayName());
        assertEquals("new@example.com", managed.getEmail());
        verify(users).save(managed);
    }

    @Test
    public void upsertIgnoresNullCanvasIdAndUnchangedValues() throws DataServiceException {
        LTI3Request lti = newLti();
        lti.setContext(context("Course title"));
        LtiUserEntity user = user("Jane Doe", "jane@example.com", "386");
        lti.setUser(user);
        lti.setMembership(new LtiMembershipEntity(lti.getContext(), user, 1));
        lti.setLink(link(LINK, LTI_LINK_ID));
        lti.getLtiCustom().put("canvas_user_id", null);

        assertEquals(0, service.upsertLTIDataInDB(lti, platformDeployment, LINK));
        verify(users, never()).save(any());
        verify(members, never()).save(any());
        verify(contexts, never()).save(any());
    }

    @Test
    public void upsertIgnoresUnchangedCanvasId() throws DataServiceException {
        LTI3Request lti = newLti();
        lti.setContext(context("Course title"));
        LtiUserEntity user = user(null, null, "386");
        lti.setUser(user);
        lti.setMembership(new LtiMembershipEntity(lti.getContext(), user, 1));
        lti.setLink(link(LINK, LTI_LINK_ID));
        lti.setLtiContextTitle(null);
        lti.getLtiCustom().put("canvas_user_id", 386);

        assertEquals(0, service.upsertLTIDataInDB(lti, platformDeployment, LINK));
        verify(users, never()).save(any());
    }

    @Test
    public void upsertWithoutDeploymentSubOrRolesTouchesNothingAndReportsIncomplete() {
        LTI3Request lti = newLti();
        lti.setLtiDeploymentId(null);
        lti.setSub(null);
        lti.setLtiRoles(null);
        lti.setLtiLinkId(null);
        lti.getLtiCustom().put("canvas_user_id", 386);

        DataServiceException ex = assertThrows(DataServiceException.class, () -> service.upsertLTIDataInDB(lti, platformDeployment, LINK));

        assertTrue(ex.getMessage().startsWith("LTI object is incomplete: "), ex.getMessage());
        assertTrue(ex.getMessage().contains("Lti Deployment Id is empty"));
        assertTrue(ex.getMessage().contains("User (sub) is empty"));
        assertTrue(ex.getMessage().contains("Lti Roles is empty"));
        assertNull(lti.getContext());
        assertNull(lti.getUser());
        assertNull(lti.getMembership());
        verifyNoInteractions(contexts, users, members, links, toolLinks);
    }

    @Test
    public void upsertWithContextButNoUserSkipsMembership() {
        LTI3Request lti = newLti();
        lti.setSub(null);
        lti.setLtiRoles(null);
        lti.setLtiLinkId(null);

        assertThrows(DataServiceException.class, () -> service.upsertLTIDataInDB(lti, platformDeployment, LINK));
        assertTrue(lti.getContext() != null);
        assertNull(lti.getMembership());
        verifyNoInteractions(members);
    }

    @Test
    public void upsertCompletesADeepLinkingRequest() throws DataServiceException {
        LTI3Request lti = newLti();
        lti.setLtiMessageType(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
        lti.setLtiLinkId(null);
        lti.setDeepLinkingSettings(Map.of("deep_link_return_url", "https://platform.example.com/return"));
        lti.setDeepLinkReturnUrl("https://platform.example.com/return");
        lti.setDeepLinkAcceptTypes(List.of("ltiResourceLink"));
        lti.setDeepLinkAcceptPresentationDocumentTargets(List.of("iframe"));

        assertEquals(3, service.upsertLTIDataInDB(lti, platformDeployment, null));
        assertTrue(lti.isUpdated());
    }

    @Test
    public void upsertRejectsAnIncompleteDeepLinkingRequest() {
        LTI3Request lti = newLti();
        lti.setLtiMessageType(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
        lti.setLtiLinkId(null);

        DataServiceException ex = assertThrows(DataServiceException.class, () -> service.upsertLTIDataInDB(lti, platformDeployment, LINK));
        assertTrue(ex.getMessage().contains("DeepLinkingSettings is empty or invalid"), ex.getMessage());
        assertFalse(lti.isUpdated());
    }

    // simple delegates and properties

    @Test
    public void delegatesToTheRepositories() {
        LtiUserEntity user = user("a", "b", null);
        LtiContextEntity context = context("t");
        LtiMembershipEntity membership = new LtiMembershipEntity(context, user, 0);
        when(users.findByUserKeyAndPlatformDeployment(SUB, platformDeployment)).thenReturn(user);
        when(members.findByUserAndContext(user, context)).thenReturn(membership);

        assertSame(repos, service.getRepos());
        assertSame(user, service.findByUserKeyAndPlatformDeployment(SUB, platformDeployment));
        assertSame(user, service.saveLtiUserEntity(user));
        assertSame(membership, service.findByUserAndContext(user, context));
        assertSame(membership, service.saveLtiMembershipEntity(membership));
        verify(users).save(user);
        verify(members).save(membership);
    }

    @Test
    public void exposesConfiguredProperties() {
        ReflectionTestUtils.setField(service, "localUrl", "https://tool.example.com");
        ReflectionTestUtils.setField(service, "ownPrivateKey", "private");
        ReflectionTestUtils.setField(service, "ownPublicKey", "public");
        ReflectionTestUtils.setField(service, "demoMode", true);
        assertEquals("https://tool.example.com", service.getLocalUrl());
        assertEquals("private", service.getOwnPrivateKey());
        assertEquals("public", service.getOwnPublicKey());
        assertTrue(service.getDemoMode());

        service.setLocalUrl("https://other.example.com");
        service.setOwnPrivateKey("private2");
        service.setOwnPublicKey("public2");
        service.setDemoMode(false);
        assertEquals("https://other.example.com", service.getLocalUrl());
        assertEquals("private2", service.getOwnPrivateKey());
        assertEquals("public2", service.getOwnPublicKey());
        assertFalse(service.getDemoMode());
    }
}
