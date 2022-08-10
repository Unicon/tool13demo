package net.unicon.lti.service.harmony;

import net.unicon.lti.model.harmony.HarmonyCourse;
import net.unicon.lti.model.harmony.HarmonyMetadata;
import net.unicon.lti.model.harmony.HarmonyMetadataLinks;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HarmonyServiceTest {
    private final String MOCK_SERVER_URL = "http://localhost:1080";
    private final String MOCK_HARMONY_JWT = "mock-harmony-jwt";
    private final String HARMONY_JWT = "harmonyJWT";
    private final String HARMONY_COURSES_API_URL = "harmonyCoursesApiUrl";
    private final String SAMPLE_ROOT_OUTCOME_GUID = "root";
    private final String SAMPLE_PI_GUID = "sample-pi-guid";
    private final String SAMPLE_ID_TOKEN = "sample-id-token";

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
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, null);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, null);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testEmptyCredentials() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, "");
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testNullUrl() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, null);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testNullToken() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "nonnull");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, null);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testEmptyUrl() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testEmptyToken() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "nonnull");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, "");
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testNotValidURL() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "notvalid");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(eq("notvalid"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testForbiddenRequest() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testBadRequest() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testWrongResponse() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenThrow(new HttpMessageNotReadableException("JSON not able to be parsed", new MockHttpInputMessage("[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]".getBytes(StandardCharsets.UTF_8))));
        assertNull(harmonyService.fetchHarmonyCourses(1));
    }

    @Test
    public void testGoodResponse() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

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
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

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

    @Test
    public void testNullCredentialsForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, null);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, null);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testEmptyCredentialsForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, "");
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testNullUrlForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, null);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testNullTokenForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_HARMONY_JWT);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, null);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testEmptyUrlForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testEmptyTokenForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "nonnull");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, "");
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testEmptyRootOutcomeGuidForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchDeepLinkingContentItems("", SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testNullRootOutcomeGuidForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchDeepLinkingContentItems(null, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testEmptyPiGuidForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, "", SAMPLE_ID_TOKEN));
    }

    @Test
    public void testNullPiGuidForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, null, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testNotValidURLForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "notvalid");
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(eq("notvalid"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testBlankIdTokenForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, ""));
    }

    @Test
    public void testNullIdTokenForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, null));
    }

    @Test
    public void testForbiddenRequestForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testBadRequestForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testWrongResponseForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenThrow(new HttpMessageNotReadableException("JSON not able to be parsed", new MockHttpInputMessage("[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]".getBytes(StandardCharsets.UTF_8))));
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN));
    }

    @Test
    public void testGoodResponseForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        ReflectionTestUtils.setField(harmonyService, HARMONY_JWT, MOCK_HARMONY_JWT);

        HarmonyContentItemDTO deepLinkingContentItemDTO1 = new HarmonyContentItemDTO();
        deepLinkingContentItemDTO1.setTitle("Reading 1");
        deepLinkingContentItemDTO1.setUrl("https://tool.com/reading1");
        HarmonyContentItemDTO deepLinkingContentItemDTO2 = new HarmonyContentItemDTO();
        deepLinkingContentItemDTO2.setTitle("Quiz 1");
        deepLinkingContentItemDTO2.setUrl("https://tool.com/quiz1");
        deepLinkingContentItemDTO2.setScoreMaximum(100f);
        deepLinkingContentItemDTO2.setLabel("Quiz 1 Label");
        deepLinkingContentItemDTO2.setResourceId("q1");
        deepLinkingContentItemDTO2.setTag("quiz");

        HarmonyContentItemDTO[] deepLinkingContentItems = new HarmonyContentItemDTO[]{deepLinkingContentItemDTO1, deepLinkingContentItemDTO2};

        ResponseEntity<HarmonyContentItemDTO[]> responseEntity = new ResponseEntity<>(deepLinkingContentItems, HttpStatus.OK);
        String deepLinkingUrl = MOCK_SERVER_URL + "/lti_deep_links?guid=" + SAMPLE_ROOT_OUTCOME_GUID + "&pi_guid=" + SAMPLE_PI_GUID + "&course_paired=false";
        ArgumentCaptor<HttpEntity<String>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(eq(deepLinkingUrl), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(HarmonyContentItemDTO[].class))).thenReturn(responseEntity);

        List<HarmonyContentItemDTO> deepLinkingContentItemsList = harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_PI_GUID, SAMPLE_ID_TOKEN);

        assertNotNull(deepLinkingContentItemsList);
        HttpEntity<String> httpEntity = httpEntityCaptor.getValue();
        assertEquals("Bearer " + MOCK_HARMONY_JWT, Objects.requireNonNull(httpEntity.getHeaders().get("Authorization")).get(0));
        assertEquals("{\"id_token\":\"sample-id-token\"}", httpEntity.getBody());
        assertEquals(2, deepLinkingContentItemsList.size());
        assertTrue(deepLinkingContentItemsList.contains(deepLinkingContentItemDTO1));
        assertTrue(deepLinkingContentItemsList.contains(deepLinkingContentItemDTO2));

    }

}
