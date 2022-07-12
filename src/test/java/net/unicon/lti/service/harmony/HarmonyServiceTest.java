package net.unicon.lti.service.harmony;

import net.unicon.lti.model.harmony.HarmonyCourse;
import net.unicon.lti.model.harmony.HarmonyMetadata;
import net.unicon.lti.model.harmony.HarmonyMetadataLinks;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HarmonyServiceTest {

    private final String MOCK_SERVER_URL = "http://localhost:1080";

    @InjectMocks
    private HarmonyService harmonyService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testNullCredentials() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", null);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", null);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testEmptyCredentials() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "");
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testNullUrl() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", null);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testNullToken() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "nonnull");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", null);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testEmptyUrl() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testEmptyToken() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "nonnull");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "");
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testNotValidURL() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", "notvalid");
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(eq("notvalid"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testForbiddenRequest() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testBadRequest() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testWrongResponse() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");

        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenThrow(new HttpMessageNotReadableException("JSON not able to be parsed", new MockHttpInputMessage("[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]".getBytes(StandardCharsets.UTF_8))));
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testGoodResponse() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");

        HarmonyCourse harmonyCourse = new HarmonyCourse();
        harmonyCourse.setRoot_outcome_guid("root");
        harmonyCourse.setBook_title("Root");
        HarmonyMetadata harmonyMetadata = new HarmonyMetadata();
        harmonyMetadata.setPage("1");
        harmonyMetadata.setPer_page("10");
        harmonyMetadata.setPage_count(1);
        harmonyMetadata.setTotal_count(1);
        HarmonyMetadataLinks harmonyMetadataLinks = new HarmonyMetadataLinks();
        harmonyMetadataLinks.setFirst("/service_api/course_catalog?page=1");
        harmonyMetadataLinks.setLast("/service_api/course_catalog?page=1");
        harmonyMetadata.setLinks(harmonyMetadataLinks);
        HarmonyPageResponse harmonyPageResponse = new HarmonyPageResponse();
        harmonyPageResponse.setRecords(List.of(harmonyCourse));
        harmonyPageResponse.setMetadata(harmonyMetadata);
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(harmonyPageResponse, HttpStatus.OK);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        HarmonyPageResponse serviceResponse = harmonyService.fetchHarmonyCourses(1);
        assertNotNull(serviceResponse);
        assertEquals(serviceResponse.getRecords().size(), 1);
        assertEquals(serviceResponse.getRecords().get(0).getRoot_outcome_guid(), "root");
        assertEquals(serviceResponse.getRecords().get(0).getBook_title(), "Root");
        assertEquals(serviceResponse.getMetadata().getPage(), "1");
        assertEquals(serviceResponse.getMetadata().getPage_count(), 1);
        assertEquals(serviceResponse.getMetadata().getTotal_count(), 1);
        assertEquals(serviceResponse.getMetadata().getPer_page(), "10");
        assertNotNull(serviceResponse.getMetadata().getLinks().getFirst());
        assertNotNull(serviceResponse.getMetadata().getLinks().getFirst());
    }

    @Test
    public void testRequestedPage() {
        ReflectionTestUtils.setField(harmonyService, "harmonyCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, "harmonyJWT", "nonnull");

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(new HarmonyPageResponse(), HttpStatus.OK);
        // Ensure that the service requests the right page
        // When requesting the default it returns null.
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(null);
        // The typo in the page attribute is introduced on purpose to verify the parameter.
        when(restTemplate.exchange(eq(MOCK_SERVER_URL + "?p_ag_e=3"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        // When requesting the second or the fourth page it returns a response.
        when(restTemplate.exchange(eq(MOCK_SERVER_URL + "?page=2"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL + "?page=4"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        // These pages do not include the response
        assertNull(harmonyService.fetchHarmonyCourses(1));
        assertNull(harmonyService.fetchHarmonyCourses(3));
        // These pages include the response
        assertNotNull(harmonyService.fetchHarmonyCourses(2));
        assertNotNull(harmonyService.fetchHarmonyCourses(4));
    }

}
