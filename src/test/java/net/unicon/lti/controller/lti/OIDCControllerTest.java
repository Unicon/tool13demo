package net.unicon.lti.controller.lti;

import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.NonceState;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
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
import java.util.Date;
import java.util.List;

import static net.unicon.lti.utils.LtiStrings.OIDC_CLIENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_DEPLOYMENT_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_FORM_POST;
import static net.unicon.lti.utils.LtiStrings.OIDC_ID_TOKEN;
import static net.unicon.lti.utils.LtiStrings.OIDC_ISS;
import static net.unicon.lti.utils.LtiStrings.OIDC_LOGIN_HINT;
import static net.unicon.lti.utils.LtiStrings.OIDC_LTI_MESSAGE_HINT;
import static net.unicon.lti.utils.LtiStrings.OIDC_NONE;
import static net.unicon.lti.utils.LtiStrings.OIDC_OPEN_ID;
import static net.unicon.lti.utils.LtiStrings.OIDC_TARGET_LINK_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@WebMvcTest(LTI3Controller.class)
public class OIDCControllerTest {
    private static final String SAMPLE_LOGIN_HINT = "sample-login-hint-from-platform";
    private static final String SAMPLE_TARGET_URI = "https://lti-tool.com/lti3";
    private static final String SAMPLE_LTI_MESSAGE_HINT = "sample-message-hint-from-platform";
    private static final String SAMPLE_ISS = "https://platform-lms.com";
    private static final String SAMPLE_CLIENT_ID = "sample-client-id";
    private static final String SAMPLE_DEPLOYMENT_ID = "sample-deployment-id";
    private static final String SAMPLE_DEPLOYMENT_ID2 = "sample-deployment-id-2";
    private static final String SAMPLE_OIDC_ENDPOINT = "https://platform-lms.com/oidc";
    private static final String SAMPLE_ENCODED_REDIRECT_URI = "https%3A%2F%2Flti-tool.com%2Flti3%2F";

    @InjectMocks
    private OIDCController oidcController = new OIDCController();

    @InjectMocks
    private AllRepositories allRepositories;

    @MockBean
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private NonceStateRepository nonceStateRepository;

    @MockBean
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

    @Configuration
    static class ContextConfiguration {

    }

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
            kpg.initialize(1024);
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
        when(req.getParameter(OIDC_ISS)).thenReturn(SAMPLE_ISS);
        when(req.getParameter(OIDC_CLIENT_ID)).thenReturn(SAMPLE_CLIENT_ID);
        when(req.getParameter(OIDC_DEPLOYMENT_ID)).thenReturn(SAMPLE_DEPLOYMENT_ID);
        when(req.getParameter(OIDC_TARGET_LINK_URI)).thenReturn(SAMPLE_TARGET_URI);
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
        /*Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(parameters.get("state").get(0));
        assertNotNull(finalClaims);
        assertEquals(TextConstants.DEFAULT_KID, finalClaims.getHeader().get("kid"));
        assertEquals("JWT", finalClaims.getHeader().get("typ"));
        assertEquals("ltiStarter", finalClaims.getBody().getIssuer());
        assertEquals(clientId, finalClaims.getBody().getAudience());
        assertNotNull(finalClaims.getBody().getExpiration());
        assertNotNull(finalClaims.getBody().getNotBefore());
        assertNotNull(finalClaims.getBody().getIssuedAt());
        assertNotNull(finalClaims.getBody().getId());
        assertEquals(iss, finalClaims.getBody().get("original_iss"));
        assertEquals(SAMPLE_LOGIN_HINT, finalClaims.getBody().get("loginHint"));
        assertEquals(SAMPLE_LTI_MESSAGE_HINT, finalClaims.getBody().get("ltiMessageHint"));
        assertEquals(SAMPLE_TARGET_URI, finalClaims.getBody().get("targetLinkUri"));
        assertEquals("/oidc/login_initiations", finalClaims.getBody().get("controller"));
        assertEquals(clientId, finalClaims.getBody().get("clientId"));
        assertEquals(deploymentId, finalClaims.getBody().get("ltiDeploymentId"));*/
    }
}