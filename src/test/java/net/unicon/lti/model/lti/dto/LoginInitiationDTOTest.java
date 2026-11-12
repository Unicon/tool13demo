package net.unicon.lti.model.lti.dto;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class LoginInitiationDTOTest {

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFromRequestMapsOidcParameters() {
        when(request.getParameter("iss")).thenReturn("https://canvas");
        when(request.getParameter("login_hint")).thenReturn("hint");
        when(request.getParameter("target_link_uri")).thenReturn("https://tool/launch");
        when(request.getParameter("lti_message_hint")).thenReturn("msg");
        when(request.getParameter("client_id")).thenReturn("client");
        when(request.getParameter("deployment_id")).thenReturn("canvas-dep");
        when(request.getParameter("lti_deployment_id")).thenReturn("lti-dep");
        when(request.getParameter("lti_storage_target")).thenReturn("_parent");

        LoginInitiationDTO dto = new LoginInitiationDTO(request);

        assertEquals("https://canvas", dto.getIss());
        assertEquals("hint", dto.getLoginHint());
        assertEquals("https://tool/launch", dto.getTargetLinkUri());
        assertEquals("msg", dto.getLtiMessageHint());
        assertEquals("client", dto.getClientId());
        assertEquals("canvas-dep", dto.getDeploymentId());
        assertEquals("lti-dep", dto.getLtiDeploymentId());
        assertEquals("_parent", dto.getLtiStorageTarget());
    }

    @Test
    public void testSetters() {
        LoginInitiationDTO dto = new LoginInitiationDTO();
        assertNull(dto.getIss());

        dto.setIss("i");
        dto.setLoginHint("l");
        dto.setTargetLinkUri("t");
        dto.setLtiMessageHint("m");
        dto.setClientId("c");
        dto.setDeploymentId("d");
        dto.setLtiDeploymentId("ld");
        dto.setLtiStorageTarget("s");

        assertEquals("i", dto.getIss());
        assertEquals("l", dto.getLoginHint());
        assertEquals("t", dto.getTargetLinkUri());
        assertEquals("m", dto.getLtiMessageHint());
        assertEquals("c", dto.getClientId());
        assertEquals("d", dto.getDeploymentId());
        assertEquals("ld", dto.getLtiDeploymentId());
        assertEquals("s", dto.getLtiStorageTarget());
    }
}
