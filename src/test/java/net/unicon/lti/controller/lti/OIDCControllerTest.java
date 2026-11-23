package net.unicon.lti.controller.lti;

import java.util.ArrayList;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static net.unicon.lti.utils.LtiStrings.OIDC_CLIENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_DEPLOYMENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_FORM_POST;
import static net.unicon.lti.utils.LtiStrings.OIDC_ID_TOKEN;
import static net.unicon.lti.utils.LtiStrings.OIDC_ISS;
import static net.unicon.lti.utils.LtiStrings.OIDC_LOGIN_HINT;
import static net.unicon.lti.utils.LtiStrings.OIDC_LTI_MESSAGE_HINT;
import static net.unicon.lti.utils.LtiStrings.OIDC_LTI_STORAGE_TARGET;
import static net.unicon.lti.utils.LtiStrings.OIDC_NONE;
import static net.unicon.lti.utils.LtiStrings.OIDC_OPEN_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_TARGET_LINK_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OIDCControllerTest {
    private static final String SAMPLE_LOGIN_HINT = "sample-login-hint-from-platform";
    private static final String SAMPLE_TARGET_URI = "https://lti-tool.com/lti3";
    private static final String SAMPLE_LTI_MESSAGE_HINT = "sample-message-hint-from-platform";
    private static final String SAMPLE_ISS = "https://platform-lms.com";
    private static final String SAMPLE_CLIENT_ID = "sample-client-id";
    private static final String SAMPLE_DEPLOYMENT_ID = "sample-deployment-id";
    private static final String SAMPLE_DEPLOYMENT_ID2 = "sample-deployment-id-2";
    private static final String SAMPLE_OIDC_ENDPOINT = "https://platform-lms.com/oidc";
    private static final String SAMPLE_ENCODED_REDIRECT_URI = "https%3A%2F%2Flti-tool.com%2Flti3";

    @InjectMocks
    private OIDCController oidcController;

    @InjectMocks
    private AllRepositories allRepositories;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private NonceStateRepository nonceStateRepository;

    @Mock
    private LTIDataService ltiDataService;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse res;

    @Mock
    private PlatformDeployment platformDeployment1;

    @Mock
    private PlatformDeployment platformDeployment2;

    private MockHttpSession mockHttpSession = new MockHttpSession();

    private List<PlatformDeployment> onePlatformDeployment;
    private List<PlatformDeployment> multiplePlatformDeployments;

    private KeyPair kp;

    private Model model = new ExtendedModelMap();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        onePlatformDeployment = Arrays.asList(platformDeployment1);
        multiplePlatformDeployments = Arrays.asList(platformDeployment1, platformDeployment2);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(ltiDataService.getDemoMode()).thenReturn(false);
        when(req.getSession()).thenReturn(mockHttpSession);
        when(ltiDataService.getLocalUrl()).thenReturn("https://lti-tool.com");
        when(platformDeployment1.getClientId()).thenReturn(SAMPLE_CLIENT_ID);
        when(platformDeployment2.getClientId()).thenReturn(SAMPLE_CLIENT_ID);
        when(platformDeployment1.getDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeployment2.getDeploymentId()).thenReturn(SAMPLE_DEPLOYMENT_ID2);
        when(platformDeployment1.getOidcEndpoint()).thenReturn(SAMPLE_OIDC_ENDPOINT);
        when(platformDeployment2.getOidcEndpoint()).thenReturn(SAMPLE_OIDC_ENDPOINT);
        when(ltiDataService.getRepos()).thenReturn(allRepositories);

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2056);
            kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
        } catch(NoSuchAlgorithmException e) {
            fail();
        }
    }

    @Test
    public void testLoginInitiationWithIssuerAndClientIdAndDeploymentIdOneConfig() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter("cookies")).thenReturn("false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class)))
                .thenReturn(onePlatformDeployment);

        String response = oidcController.loginInitiations(req, model);
        Mockito.verify(req).getSession();
        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);
    }
    @Test
    public void testLoginInitiationWithIssuerAndClientIdAndDeploymentIdOneConfigCookies() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter("cookies")).thenReturn("true");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(null);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class)))
                .thenReturn(onePlatformDeployment);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);
    }

    @Test
    public void testLoginInitiationWithIssuerAndClientIdAndDeploymentIdMultipleConfigs() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(any(String.class), any(String.class), any(String.class)))
                .thenReturn(multiplePlatformDeployments);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);
    }

    @Test
    public void testLoginInitiationWithIssuerAndClientIdOneConfig() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID)))
                .thenReturn(onePlatformDeployment);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);
    }

    @Test
    public void testLoginInitiationWithIssuerAndClientIdMultipleConfigs() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID)))
                .thenReturn(multiplePlatformDeployments);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, null, SAMPLE_ISS);
    }

    @Test
    public void testLoginInitiationWithIssuerOneConfig() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIss(eq(SAMPLE_ISS)))
                .thenReturn(onePlatformDeployment);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);

    }

    @Test
    public void testLoginInitiationWithIssuerMultipleConfigs() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIss(eq(SAMPLE_ISS)))
                .thenReturn(multiplePlatformDeployments);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, null, null, SAMPLE_ISS);

    }

    @Test
    public void testLoginInitiationWithIssuerAndDeploymentIdOneConfig() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(onePlatformDeployment);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);

    }

    @Test
    public void testLoginInitiationWithIssuerAndDeploymentIdMultipleConfigs() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);
        when(platformDeploymentRepository.findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID)))
                .thenReturn(multiplePlatformDeployments);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(SAMPLE_ISS), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository).findByIssAndDeploymentId(eq(SAMPLE_ISS), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(SAMPLE_ISS));
        Mockito.verify(req).getSession();
        Mockito.verify(ltiDataService).getLocalUrl();
        Mockito.verify(ltiDataService).getOwnPrivateKey();

        validateOAuthResponse(response, null, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);

    }

    @Test
    public void testLoginInitiationWithoutConfigIdentifiers() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(null);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(null);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientIdAndDeploymentId(eq(null), eq(null), eq(null));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(null), eq(null));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(null), eq(null));
        Mockito.verify(platformDeploymentRepository).findByIss(eq(null));
        Mockito.verify(req, never()).getSession();
        Mockito.verify(ltiDataService, never()).getLocalUrl();
        Mockito.verify(ltiDataService, never()).getOwnPrivateKey();

        assertEquals(TextConstants.LTI3ERROR, response);
    }

    @Test
    public void testLoginInitiationWithoutIss() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        when(req.getParameter(OIDC_ISS)).thenReturn(null);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(SAMPLE_LTI_MESSAGE_HINT);
        when(req.getParameter(OIDC_LOGIN_HINT)).thenReturn(SAMPLE_LOGIN_HINT);

        String response = oidcController.loginInitiations(req, model);

        Mockito.verify(platformDeploymentRepository).findByIssAndClientIdAndDeploymentId(eq(null), eq(SAMPLE_CLIENT_ID), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndClientId(eq(null), eq(SAMPLE_CLIENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIssAndDeploymentId(eq(null), eq(SAMPLE_DEPLOYMENT_ID));
        Mockito.verify(platformDeploymentRepository, never()).findByIss(eq(null));
        Mockito.verify(req, never()).getSession();
        Mockito.verify(ltiDataService, never()).getLocalUrl();
        Mockito.verify(ltiDataService, never()).getOwnPrivateKey();

        assertEquals(TextConstants.LTI3ERROR, response);
    }

    private void validateOAuthResponse(String response, String clientId, String deploymentId, String iss) {
        UriComponents responseUri = UriComponentsBuilder.fromUriString(response.substring("redirect:".length())).build();
        assertEquals(SAMPLE_OIDC_ENDPOINT, responseUri.getScheme() + "://" + responseUri.getHost() + responseUri.getPath());
        MultiValueMap<String, String> parameters = responseUri.getQueryParams();

        if (clientId != null) {
            assertEquals(clientId, parameters.get("client_id").get(0));
        } else {
            assertNull(parameters.get("client_id"));
        }
        assertEquals(SAMPLE_LOGIN_HINT, parameters.get("login_hint").get(0));
        assertEquals(SAMPLE_LTI_MESSAGE_HINT, parameters.get("lti_message_hint").get(0));
        assertEquals(OIDC_NONE, parameters.get("prompt").get(0));
        assertEquals(SAMPLE_ENCODED_REDIRECT_URI, parameters.get("redirect_uri").get(0));
        assertEquals(OIDC_FORM_POST, parameters.get("response_mode").get(0));
        assertEquals(OIDC_ID_TOKEN, parameters.get("response_type").get(0));
        assertEquals(OIDC_OPEN_ID, parameters.get("scope").get(0));
        assertTrue(parameters.get("nonce").get(0).length() >= 36);
        assertTrue(parameters.get("state").get(0).length() >= 36);
    }


    private void givenOneRegisteredDeployment() {
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(SAMPLE_ISS, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID))
                .thenReturn(onePlatformDeployment);
    }

    @Test
    public void testLoginInitiationInDemoModeWithStorageTarget() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        givenOneRegisteredDeployment();
        when(req.getParameter(OIDC_LTI_STORAGE_TARGET)).thenReturn("_parent");
        when(ltiDataService.getDemoMode()).thenReturn(true);

        String response = oidcController.loginInitiations(req, model);

        assertEquals("oidcRedirect", response);
        assertEquals("_parent", model.getAttribute("lti_storage_target"));
        assertEquals(SAMPLE_OIDC_ENDPOINT, model.getAttribute("oidc_authorization_uri"));
        assertEquals(SAMPLE_CLIENT_ID, model.getAttribute("client_id_received"));
        assertEquals(SAMPLE_DEPLOYMENT_ID, model.getAttribute("deployment_id_received"));
        assertEquals(SAMPLE_CLIENT_ID, model.getAttribute(OIDC_CLIENT_ID));
        assertTrue(model.containsAttribute("initiation_dto"));
        assertTrue(((String) model.getAttribute("oidcEndpointComplete")).startsWith(SAMPLE_OIDC_ENDPOINT + "?client_id="));
        assertEquals(List.of(model.getAttribute("state")), mockHttpSession.getAttribute("lti_state"));
        assertEquals(List.of(model.getAttribute("nonce")), mockHttpSession.getAttribute("lti_nonce"));
    }

    @Test
    public void testForcedCookiesIgnoreTheStorageTarget() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "true");
        givenOneRegisteredDeployment();
        when(req.getParameter(OIDC_LTI_STORAGE_TARGET)).thenReturn("_parent");

        String response = oidcController.loginInitiations(req, model);

        validateOAuthResponse(response, SAMPLE_CLIENT_ID, SAMPLE_DEPLOYMENT_ID, SAMPLE_ISS);
        assertFalse(model.containsAttribute("lti_storage_target"));
        assertFalse(model.containsAttribute("oidc_authorization_uri"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testKnownStatesAndNoncesAreNotAddedTwice() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        givenOneRegisteredDeployment();
        List<String> states = mock(List.class);
        List<String> nonces = mock(List.class);
        when(states.contains(any())).thenReturn(true);
        when(nonces.contains(any())).thenReturn(true);
        mockHttpSession.setAttribute("lti_state", states);
        mockHttpSession.setAttribute("lti_nonce", nonces);

        oidcController.loginInitiations(req, model);

        verify(states, never()).add(any());
        verify(nonces, never()).add(any());
        assertSame(states, mockHttpSession.getAttribute("lti_state"));
        assertSame(nonces, mockHttpSession.getAttribute("lti_nonce"));
    }

    @Test
    public void testExistingSessionListsAreExtended() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        givenOneRegisteredDeployment();
        mockHttpSession.setAttribute("lti_state", new ArrayList<>(List.of("older-state")));
        mockHttpSession.setAttribute("lti_nonce", new ArrayList<>(List.of("older-nonce")));

        oidcController.loginInitiations(req, model);

        assertEquals(2, ((List<?>) mockHttpSession.getAttribute("lti_state")).size());
        assertEquals(2, ((List<?>) mockHttpSession.getAttribute("lti_nonce")).size());
    }

    @Test
    public void testMissingMessageHintIsLeftOutOfTheUrl() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        givenOneRegisteredDeployment();
        when(req.getParameter(OIDC_LTI_MESSAGE_HINT)).thenReturn(null);

        String response = oidcController.loginInitiations(req, model);

        assertTrue(response.startsWith("redirect:" + SAMPLE_OIDC_ENDPOINT + "?client_id="));
        assertFalse(response.contains("lti_message_hint"));
    }

    @Test
    public void testStateGenerationFailureIsAnError() {
        ReflectionTestUtils.setField(oidcController, "forceCookies", "false");
        givenOneRegisteredDeployment();
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not-a-key");

        String response = oidcController.loginInitiations(req, model);

        assertEquals(TextConstants.LTI3ERROR, response);
        assertTrue(((String) model.getAttribute(TextConstants.ERROR)).contains("Exception"));
        Mockito.verify(req, never()).getSession();
    }

}
