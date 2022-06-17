package net.unicon.lti.service.harmony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import net.unicon.lti.model.harmony.HarmonyPageResponse;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {1080, 1081})
@SpringBootTest
public class HarmonyServiceTest {

    private final String MOCK_SERVER_URL = "http://localhost:1080";

    @Autowired
    private HarmonyService harmonyService;

    private final ClientAndServer client;

    public HarmonyServiceTest(ClientAndServer client) {
        this.client = client;
    }

    @Test
    public void testNullCredentials() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", null);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", null);
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testEmptyCredentials() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testNullUrl() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", null);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testNullToken() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "nonnull");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", null);
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testEmptyUrl() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testEmptyToken() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "nonnull");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testNotValidURL() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "notvalid");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testForbiddenRequest() {
        client.reset();
        client
           .when(request().withMethod("GET"))
           .respond(response().withStatusCode(401));
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testBadRequest() {
        client.reset();
        client
            .when(request().withMethod("GET"))
            .respond(response().withStatusCode(400));
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertEquals(harmonyService.fetchHarmonyCourses(), null);
    }

    @Test
    public void testWrongResponse() {
        String jsonResponse = "[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]";
        client.reset();
        client
            .when(request().withMethod("GET"))
            .respond(response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(jsonResponse));
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        HarmonyPageResponse harmonyResponse = harmonyService.fetchHarmonyCourses();  
        assertEquals(harmonyResponse, null);
    }

    @Test
    public void testGoodResponse() {
        String jsonResponse = "{\"records\":[{\"root_outcome_guid\":\"root\",\"book_title\":\"Root\",\"release_date\":null,\"cover_img_url\":null,\"category\":null,\"description\":null,\"table_of_contents\":{}}],\"metadata\":{\"page\":\"1\",\"per_page\":\"10\",\"page_count\":1,\"total_count\":1,\"links\":{\"first\":\"/service_api/course_catalog?page=1\",\"last\":\"/service_api/course_catalog?page=1\"}}}";
        client.reset();
        client
            .when(request().withMethod("GET"))
            .respond(response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(jsonResponse));
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        HarmonyPageResponse harmonyResponse = harmonyService.fetchHarmonyCourses(); 
        assertNotEquals(harmonyResponse, null);
        assertEquals(harmonyResponse.getRecords().size(), 1);
        assertEquals(harmonyResponse.getRecords().get(0).getRoot_outcome_guid(), "root");
        assertEquals(harmonyResponse.getRecords().get(0).getBook_title(), "Root");
        assertEquals(harmonyResponse.getMetadata().getPage(), "1");
        assertEquals(harmonyResponse.getMetadata().getPage_count(), 1);
        assertEquals(harmonyResponse.getMetadata().getTotal_count(), 1);
        assertEquals(harmonyResponse.getMetadata().getPer_page(), "10");
        assertNotEquals(harmonyResponse.getMetadata().getLinks().getFirst(), null);
        assertNotEquals(harmonyResponse.getMetadata().getLinks().getFirst(), null);
    }

}
