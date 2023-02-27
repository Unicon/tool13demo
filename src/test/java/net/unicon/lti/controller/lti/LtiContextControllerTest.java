package net.unicon.lti.controller.lti;

import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import net.unicon.lti.model.harmony.HarmonyFetchDeepLinksBody;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.harmony.HarmonyService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.lti.DeepLinkUtils;
import net.unicon.lti.utils.lti.LTI3Request;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LtiContextControllerTest {
    private static final String SAMPLE_DL_ID_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImI0MWMzYTdhLWJmN2ItNDczYy04MDRjLTJkZjRlMjZkMzBkZCIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE2NTg0MTk5MTMsImV4cCI6MTY1ODQyMTcxMywiaXNzIjoiaHR0cHM6Ly91bmljb24uZDJsLXBhcnRuZXJzLmJyaWdodHNwYWNlLmNvbSIsImF1ZCI6IjY4ZmY2OWNjLTdiYjYtNDZjNC1iODk5LTMxYWYyMTM3MTNlMSIsImlhdCI6MTY1ODQxOTkxMywic3ViIjoiMDg5M2NjMDktZDAyMy00YjJlLTkzYjQtZDQ3Yzk1ODNiYWY1XzE5MjEiLCJnaXZlbl9uYW1lIjoiVW5pY29uIiwiZmFtaWx5X25hbWUiOiJBZG1pbjIiLCJuYW1lIjoiVW5pY29uIEFkbWluMiIsImVtYWlsIjoiVW5pY29uLkEyQGV4YW1wbGUuY29tIiwibm9uY2UiOiIzYjljMDkyNjNkNGI5ZGFhZGEzYzY1NjM0YjIwNzk1MmUxN2I2Y2Q4M2IzNzBmYTFhZDEyMGFkMTE2NGEzZDQ3IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbWVzc2FnZV90eXBlIjoiTHRpRGVlcExpbmtpbmdSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vZGVwbG95bWVudF9pZCI6ImI5N2YwODE1LWQ4NzQtNDRjMy04MmQyLTA5YmViMGQ2NmMwNSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vMDFjNi0xNzQtMjYtMTM0LTcxLm5ncm9rLmlvL2x0aTMvIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZXMiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNNZW1iZXIiLCJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9tZW1iZXJzaGlwI0FkbWluaXN0cmF0b3IiXSwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vY29udGV4dCI6eyJpZCI6IjY4MzYiLCJsYWJlbCI6ImFsZy1tZ3dvemR6IiwidGl0bGUiOiJBbGdlYnJhIChtZ3dvemR6KSIsInR5cGUiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvY291cnNlI0NvdXJzZU9mZmVyaW5nIl19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9saXMiOnsiY291cnNlX29mZmVyaW5nX3NvdXJjZWRpZCI6InVuaWNvbi5kMmwtcGFydG5lcnMuYnJpZ2h0c3BhY2UuY29tOmFsZy1tZ3dvemR6IiwiY291cnNlX3NlY3Rpb25fc291cmNlZGlkIjoidW5pY29uLmQybC1wYXJ0bmVycy5icmlnaHRzcGFjZS5jb206YWxnLW1nd296ZHoifSwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGF1bmNoX3ByZXNlbnRhdGlvbiI6eyJsb2NhbGUiOiJlbi11cyJ9LCJodHRwOi8vd3d3LmJyaWdodHNwYWNlLmNvbSI6eyJ0ZW5hbnRfaWQiOiI0MDI1NmY3Ny04MDJjLTQxMWEtOTJkYy1lNzdiN2IwY2I3YWUiLCJvcmdfZGVmaW5lZF9pZCI6IlVuaWNvbi5BMiIsInVzZXJfaWQiOjE5MjEsInVzZXJuYW1lIjoiVW5pY29uLkEyIiwiQ29udGV4dC5pZC5oaXN0b3J5IjoiIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9jbGFpbS9lbmRwb2ludCI6eyJzY29wZSI6WyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvbGluZWl0ZW0iLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvbGluZWl0ZW0ucmVhZG9ubHkiLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvcmVzdWx0LnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Njb3JlIl0sImxpbmVpdGVtcyI6Imh0dHBzOi8vdW5pY29uLmQybC1wYXJ0bmVycy5icmlnaHRzcGFjZS5jb20vZDJsL2FwaS9sdGkvYWdzLzIuMC9kZXBsb3ltZW50L2I5N2YwODE1LWQ4NzQtNDRjMy04MmQyLTA5YmViMGQ2NmMwNS9vcmd1bml0LzY4MzYvbGluZWl0ZW1zIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWRsL2NsYWltL2RlZXBfbGlua2luZ19zZXR0aW5ncyI6eyJhY2NlcHRfdHlwZXMiOlsibGluayIsImZpbGUiLCJsdGlSZXNvdXJjZUxpbmsiLCJpbWFnZSJdLCJhY2NlcHRfbWVkaWFfdHlwZXMiOiIqLyoiLCJhY2NlcHRfcHJlc2VudGF0aW9uX2RvY3VtZW50X3RhcmdldHMiOlsiaWZyYW1lIiwid2luZG93Il0sImFjY2VwdF9tdWx0aXBsZSI6dHJ1ZSwiYXV0b19jcmVhdGUiOmZhbHNlLCJkZWVwX2xpbmtfcmV0dXJuX3VybCI6Imh0dHBzOi8vdW5pY29uLmQybC1wYXJ0bmVycy5icmlnaHRzcGFjZS5jb20vZDJsL2x0aS9kbC9jb250ZW50L29yZ1VuaXRJZC82ODM2L2xpbmtJZC8xNzUwL3BhcmVudE1vZHVsZUlkLzU0MDIvTW1WTmF2SUFnUmg5S291d0EwbzF4WldTOGJMUGRyMTh4dVdpM1hWdDZmUSUyNTdlIiwiZGF0YSI6Ik1tVk5hdklBZ1JoOUtvdXdBMG8xeFpXUzhiTFBkcjE4eHVXaTNYVnQ2ZlF-In0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3Rvb2xfcGxhdGZvcm0iOnsiZ3VpZCI6IjQwMjU2Zjc3LTgwMmMtNDExYS05MmRjLWU3N2I3YjBjYjdhZSIsInByb2R1Y3RfZmFtaWx5X2NvZGUiOiJkZXNpcmUybGVhcm4ifX0.miv__rGCWOoU_dUU_ht0XbSGLIyyEDT_XMUyK1geBXrjr9EgkRCc3ExmEefxxpjKDW0ilQcVWydibIjdFxIcqRNVoaKDxGHfn87C2i273VCRo6Y0nMJtaiRwxxVkLrwnTi7dUZTWF3rHYb_M__XwhPpxk1vd7tZ7WmoeIcqMBn4tyPpbG4vlfyzJkigKuPnHe6pFEbYTvyYqMmv-tzERwJ8OdHtmbiOGORnPW7Bw_PjpS8oG-7x416tFvdsebINjqwtApFGTXFaDs9UFso1f7Z1fCpoQ1UYRKqbw_iS5Lr8cLqmsCjkzwI7drEDT86ajPJ9volFMSJCr-9idoe92Ug";
    private static final String SAMPLE_ROOT_OUTCOME_GUID = "root";
    private static final String ROOT_OUTCOME_GUID = "root_outcome_guid";
    private static final String ID_TOKEN = "id_token";
    private static final String SAMPLE_CONTEXT_ID = "sample-context-id";
    private static final String SAMPLE_ISSUER = "https://lms.com";
    private static final String SAMPLE_CLIENT_ID = "sample-client-id";
    private static final String SAMPLE_DEPLOYMENT_ID = "sample-deployment-id";

    private PlatformDeployment platformDeployment = new PlatformDeployment();
    private LtiContextEntity ltiContextEntity = new LtiContextEntity();

    @InjectMocks
    private LtiContextController ltiContextController = new LtiContextController();

    @Mock
    PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    LtiContextRepository ltiContextRepository;

    @Mock
    LTIDataService ltiDataService;

    @Mock
    HarmonyService harmonyService;

    @Mock
    LTI3Request lti3Request;

    private MockedStatic<LTI3Request> lti3RequestMockedStatic;

    private MockedStatic<DeepLinkUtils> deepLinkUtilsMockedStatic;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        deepLinkUtilsMockedStatic = Mockito.mockStatic(DeepLinkUtils.class);
        lti3RequestMockedStatic = Mockito.mockStatic(LTI3Request.class);
    }

    @AfterEach
    public void close() {
        deepLinkUtilsMockedStatic.close();
        lti3RequestMockedStatic.close();
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContextWithoutRootOutcomeGuid() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(null, SAMPLE_DL_ID_TOKEN, null);

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertNull(ltiContextEntity.getRootOutcomeGuid());
        assertNull(ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository, never()).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody());
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContextWithoutIdToken() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, null, null);

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertNull(ltiContextEntity.getRootOutcomeGuid());
        assertNull(ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository, never()).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody());
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContextWithoutLtiContext() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
        lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
        when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
        when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
        when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
        when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(List.of(platformDeployment));
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(null);

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertNull(ltiContextEntity.getRootOutcomeGuid());
        assertNull(ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository, never()).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Could not find LMS course context", response.getBody());
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContextWithoutDeepLinkingContentItemsFromHarmony() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
        lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
        when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
        when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
        when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
        when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(List.of(platformDeployment));
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
        when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(new ArrayList<>());

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertNull(ltiContextEntity.getRootOutcomeGuid());
        assertNull(ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository, never()).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Error communicating with Harmony", response.getBody());
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContextWithNullDeepLinkingContentItemsFromHarmony() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
        lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
        when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
        when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
        when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
        when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(List.of(platformDeployment));
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
        when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(null);

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertNull(ltiContextEntity.getRootOutcomeGuid());
        assertNull(ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository, never()).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Error communicating with Harmony", response.getBody());
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContextDeepLinkingResponseGenerationThrowsException() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
        lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
        when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
        when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
        when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
        when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(List.of(platformDeployment));
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
        List<HarmonyContentItemDTO> contentItems = generateDeepLinkingContentItemsList();
        when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(contentItems);
        deepLinkUtilsMockedStatic.when(() -> DeepLinkUtils.generateDeepLinkingResponseJWT(eq(ltiDataService), eq(lti3Request), anyList())).thenThrow(GeneralSecurityException.class);

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertNull(ltiContextEntity.getRootOutcomeGuid());
        assertNull(ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository, never()).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Exception thrown", response.getBody());
    }

    @Test
    public void testPrepareDeepLinkingResponseForLMSContext() {
        HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
        lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
        when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
        when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
        when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
        when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(List.of(platformDeployment));
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
        List<HarmonyContentItemDTO> contentItems = generateDeepLinkingContentItemsList();
        when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(contentItems);
        deepLinkUtilsMockedStatic.when(() -> DeepLinkUtils.generateDeepLinkingResponseJWT(eq(ltiDataService), eq(lti3Request), anyList())).thenReturn("deep-linking-response-jwt");
        when(lti3Request.getDeepLinkReturnUrl()).thenReturn("https://lms.com/deep-link-return");

        ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

        assertEquals(SAMPLE_ROOT_OUTCOME_GUID, ltiContextEntity.getRootOutcomeGuid());
        assertEquals(false, ltiContextEntity.getLineitemsSynced());
        verify(ltiContextRepository).save(eq(ltiContextEntity));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> responseBody = (Map) response.getBody();
        assertEquals("deep-linking-response-jwt", responseBody.get("JWT"));
        assertEquals("https://lms.com/deep-link-return", responseBody.get("deep_link_return_url"));
    }

    @Test
    public void testPairBookWithoutRootOutcomeGuid() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(null, SAMPLE_DL_ID_TOKEN, null);

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Invalid request", response.getBody());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPairBookWithoutIdToken() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, null, null);

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Invalid request", response.getBody());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPairBookWithoutLtiContext() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
            lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                    .thenReturn(List.of(platformDeployment));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(null);

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("Could not find LMS course context", response.getBody());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPairBookWithoutDeepLinkingContentItemsFromHarmony() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
            lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                    .thenReturn(List.of(platformDeployment));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
            when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(new ArrayList<>());

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("Error communicating with Harmony", response.getBody());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPairBookWithNullDeepLinkingContentItemsFromHarmony() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
            lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                    .thenReturn(List.of(platformDeployment));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
            when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(null);

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("Error communicating with Harmony", response.getBody());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPairBookDeepLinkingResponseGenerationThrowsException() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
            lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                    .thenReturn(List.of(platformDeployment));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
            List<HarmonyContentItemDTO> contentItems = generateDeepLinkingContentItemsList();
            when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(contentItems);
            deepLinkUtilsMockedStatic.when(() -> DeepLinkUtils.generateDeepLinkingResponseJWT(eq(ltiDataService), eq(lti3Request), anyList())).thenThrow(GeneralSecurityException.class);

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("Exception thrown", response.getBody());
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPairBook() {
        try {
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, null);
            lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                    .thenReturn(List.of(platformDeployment));
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
            List<HarmonyContentItemDTO> contentItems = generateDeepLinkingContentItemsList();
            when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(false), eq(null))).thenReturn(contentItems);
            deepLinkUtilsMockedStatic.when(() -> DeepLinkUtils.generateDeepLinkingResponseJWT(eq(ltiDataService), eq(lti3Request), anyList())).thenReturn("deep-linking-response-jwt");
            when(lti3Request.getDeepLinkReturnUrl()).thenReturn("https://lms.com/deep-link-return");

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, String> responseBody = (Map) response.getBody();
            assertEquals("deep-linking-response-jwt", responseBody.get("JWT"));
            assertEquals("https://lms.com/deep-link-return", responseBody.get("deep_link_return_url"));
        } catch (JSONException e) {
            fail();
        }
    }

    @Test
    public void testPrepareDeepLinkingResponseForReturningUser() {
        try {
            List<String> moduleIds = new ArrayList<>();
            moduleIds.add("module-id-1");
            moduleIds.add("module-id-2");
            moduleIds.add("module-id-3");
            HarmonyFetchDeepLinksBody harmonyFetchDeepLinksBody = new HarmonyFetchDeepLinksBody(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_DL_ID_TOKEN, moduleIds);
            lti3RequestMockedStatic.when(() -> LTI3Request.makeLTI3Request(eq(ltiDataService), eq(true), eq(null), eq(SAMPLE_DL_ID_TOKEN), eq(null))).thenReturn(lti3Request);
            when(lti3Request.getLtiContextId()).thenReturn(SAMPLE_CONTEXT_ID);
            when(lti3Request.getIss()).thenReturn(SAMPLE_ISSUER);
            when(lti3Request.getAud()).thenReturn(SAMPLE_CLIENT_ID);
            when(lti3Request.getLtiDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
            when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISSUER), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID)))
                    .thenReturn(List.of(platformDeployment));
            ltiContextEntity.setRootOutcomeGuid("root-outcome-guid-1");
            when(ltiContextRepository.findByContextKeyAndPlatformDeployment(eq(SAMPLE_CONTEXT_ID), eq(platformDeployment))).thenReturn(ltiContextEntity);
            List<HarmonyContentItemDTO> contentItems = generateDeepLinkingContentItemsList();
            when(harmonyService.fetchDeepLinkingContentItems(eq(SAMPLE_ROOT_OUTCOME_GUID), eq(SAMPLE_DL_ID_TOKEN), eq(true), eq(moduleIds))).thenReturn(contentItems);
            deepLinkUtilsMockedStatic.when(() -> DeepLinkUtils.generateDeepLinkingResponseJWT(eq(ltiDataService), eq(lti3Request), anyList())).thenReturn("deep-linking-response-jwt");
            when(lti3Request.getDeepLinkReturnUrl()).thenReturn("https://lms.com/deep-link-return");

            ResponseEntity<Object> response = ltiContextController.prepareDeepLinkingResponse(harmonyFetchDeepLinksBody, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, String> responseBody = (Map) response.getBody();
            assertEquals("deep-linking-response-jwt", responseBody.get("JWT"));
            assertEquals("https://lms.com/deep-link-return", responseBody.get("deep_link_return_url"));
        } catch (JSONException e) {
            fail();
        }
    }

    private static List<HarmonyContentItemDTO> generateDeepLinkingContentItemsList() {
        HarmonyContentItemDTO contentItem1 = new HarmonyContentItemDTO();
        contentItem1.setTitle("Reading 1");
        contentItem1.setUrl("https://tool.com/reading1");
        HarmonyContentItemDTO contentItem2 = new HarmonyContentItemDTO();
        contentItem2.setTitle("Quiz 1");
        contentItem2.setUrl("https://tool.com/quiz1");
        contentItem2.setScoreMaximum(100f);
        contentItem2.setLabel("Quiz 1 Label");
        contentItem2.setResourceId("q1");
        contentItem2.setTag("quiz");
        return List.of(contentItem1, contentItem2);
    }
}
