package net.unicon.lti.model.lti.dto;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

public class DeepLinkDtoTest {

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDeepLinkDtoFromRequest() {
        when(request.getParameter("toolLinkId")).thenReturn("tl");
        when(request.getParameter("title")).thenReturn("Title");
        when(request.getParameter("description")).thenReturn("Desc");

        DeepLinkDTO dto = new DeepLinkDTO(request);

        assertEquals("tl", dto.getToolLinkId());
        assertEquals("Title", dto.getTitle());
        assertEquals("Desc", dto.getDescription());
    }

    @Test
    public void testDeepLinkDtoAccessors() {
        DeepLinkDTO dto = new DeepLinkDTO("tl", "Title", "Desc");
        assertEquals("tl", dto.getToolLinkId());
        assertEquals("Title", dto.getTitle());
        assertEquals("Desc", dto.getDescription());

        DeepLinkDTO empty = new DeepLinkDTO();
        assertNull(empty.getToolLinkId());
        empty.setToolLinkId("a");
        empty.setTitle("b");
        empty.setDescription("c");
        assertEquals("a", empty.getToolLinkId());
        assertEquals("b", empty.getTitle());
        assertEquals("c", empty.getDescription());
    }

    @Test
    public void testDeepLinkJwtDto() {
        List<Map<String, Object>> json = List.of(Map.of("type", "ltiResourceLink"));
        DeepLinkJWTDTO dto = new DeepLinkJWTDTO(json, "jwt");
        assertSame(json, dto.getJSONString());
        assertEquals("jwt", dto.getJWTString());

        DeepLinkJWTDTO empty = new DeepLinkJWTDTO();
        assertNull(empty.getJSONString());
        empty.setJSONString(json);
        empty.setJWTString("jwt2");
        assertSame(json, empty.getJSONString());
        assertEquals("jwt2", empty.getJWTString());
    }

    @Test
    public void testDeepLinkRequest() {
        DeepLinkRequest deepLinkRequest = new DeepLinkRequest();
        List<String> ids = List.of("1", "2");
        deepLinkRequest.setSelectedIds(ids);
        deepLinkRequest.setToken("token");
        deepLinkRequest.setId_token("id-token");
        deepLinkRequest.setState_hash("hash");
        deepLinkRequest.setNonce("nonce");

        assertSame(ids, deepLinkRequest.getSelectedIds());
        assertEquals("token", deepLinkRequest.getToken());
        assertEquals("id-token", deepLinkRequest.getId_token());
        assertEquals("hash", deepLinkRequest.getState_hash());
        assertEquals("nonce", deepLinkRequest.getNonce());
    }
}
