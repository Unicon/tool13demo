package net.unicon.lti.service.harmony;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.harmony.HarmonyContentItemDTO;
import net.unicon.lti.model.harmony.HarmonyCourse;
import net.unicon.lti.model.harmony.HarmonyFetchDeepLinksBody;
import net.unicon.lti.model.harmony.HarmonyMetadata;
import net.unicon.lti.model.harmony.HarmonyMetadataLinks;
import net.unicon.lti.model.harmony.HarmonyPageResponse;
import net.unicon.lti.utils.RestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarmonyServiceTest {
    private final String MOCK_SERVER_URL = "http://localhost:1080";
    private final String MOCK_SERVER_LINEITEMS_URL = MOCK_SERVER_URL + "/lineitems";
    private final String HARMONY_COURSES_API_URL = "harmonyCoursesApiUrl";
    private final String SAMPLE_ROOT_OUTCOME_GUID = "root";
    private final String SAMPLE_ID_TOKEN = "sample-id-token";

    @InjectMocks
    private HarmonyService harmonyService;

    @Mock
    private RestTemplate restTemplate;

    private MockedStatic<RestUtils> restUtilsMockedStatic;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);
        restUtilsMockedStatic = Mockito.mockStatic(RestUtils.class);
        restUtilsMockedStatic.when(RestUtils::createRestTemplate).thenReturn(restTemplate);
    }

    @AfterEach
    public void close() {
        restUtilsMockedStatic.close();
    }

    @Test
    public void testNullUrl() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, null);
        assertNull(harmonyService.fetchHarmonyCourses(1, null));
    }

    @Test
    public void testEmptyUrl() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "");
        assertNull(harmonyService.fetchHarmonyCourses(1, null));
    }

    @Test
    public void testNotValidURL() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "notvalid");
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(eq("notvalid"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        assertNull(harmonyService.fetchHarmonyCourses(1, null));
    }

    @Test
    public void testForbiddenRequest() {
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchHarmonyCourses(1, null));
    }

    @Test
    public void testBadRequest() {
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchHarmonyCourses(1, null));
    }

    @Test
    public void testWrongResponse() {
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenThrow(new HttpMessageNotReadableException("JSON not able to be parsed", new MockHttpInputMessage("[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]".getBytes(StandardCharsets.UTF_8))));
        assertNull(harmonyService.fetchHarmonyCourses(1, null));
    }

    @Test
    public void testGoodResponse() {
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
        when(restTemplate.getForEntity(eq(MOCK_SERVER_URL), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        HarmonyPageResponse serviceResponse = harmonyService.fetchHarmonyCourses(1, null);
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
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(new HarmonyPageResponse(), HttpStatus.OK);
        // Ensure that the service requests the right page
        // When requesting the default it returns null.
        when(restTemplate.getForEntity(eq(MOCK_SERVER_URL), eq(HarmonyPageResponse.class))).thenReturn(null);
        // The typo in the page attribute is introduced on purpose to verify the parameter.
        when(restTemplate.getForEntity(eq(MOCK_SERVER_URL + "?p_ag_e=3"), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        // When requesting the second or the fourth page it returns a response.
        when(restTemplate.getForEntity(eq(MOCK_SERVER_URL + "?page=2"), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        when(restTemplate.getForEntity(eq(MOCK_SERVER_URL + "?page=4"), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        // These pages do not include the response
        assertNull(harmonyService.fetchHarmonyCourses(1, null));
        assertNull(harmonyService.fetchHarmonyCourses(3, null));
        // These pages include the response
        assertNotNull(harmonyService.fetchHarmonyCourses(2, null));
        assertNotNull(harmonyService.fetchHarmonyCourses(4, null));
    }

    @Test
    public void testFetchCourseByRootOutcomeGuid() {
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
        when(restTemplate.getForEntity(eq(MOCK_SERVER_URL + "?root_outcome_guid=root"), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        HarmonyPageResponse serviceResponse = harmonyService.fetchHarmonyCourses(1, "root");
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
    public void testNullUrlForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, null);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testEmptyUrlForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "");
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testEmptyRootOutcomeGuidForFetchDeepLinkingContentItems() {
        assertNull(harmonyService.fetchDeepLinkingContentItems("", SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testNullRootOutcomeGuidForFetchDeepLinkingContentItems() {
        assertNull(harmonyService.fetchDeepLinkingContentItems(null, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testNotValidURLForFetchDeepLinkingContentItems() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, "notvalid");
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(eq("notvalid"), eq(HttpMethod.GET), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testBlankIdTokenForFetchDeepLinkingContentItems() {
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, "", false, null));
    }

    @Test
    public void testNullIdTokenForFetchDeepLinkingContentItems() {
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, null, false,null));
    }

    @Test
    public void testForbiddenRequestForFetchDeepLinkingContentItems() {
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testBadRequestForFetchDeepLinkingContentItems() {
        ResponseEntity<HarmonyPageResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenReturn(responseEntity);

        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testWrongResponseForFetchDeepLinkingContentItems() {
        when(restTemplate.exchange(eq(MOCK_SERVER_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(HarmonyPageResponse.class))).thenThrow(new HttpMessageNotReadableException("JSON not able to be parsed", new MockHttpInputMessage("[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]".getBytes(StandardCharsets.UTF_8))));
        assertNull(harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null));
    }

    @Test
    public void testGoodResponseForFetchDeepLinkingContentItems() {
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
        String deepLinkingUrl = MOCK_SERVER_URL + "/lti_deep_links?guid=" + SAMPLE_ROOT_OUTCOME_GUID + "&course_paired=false";
        HarmonyFetchDeepLinksBody body = new HarmonyFetchDeepLinksBody(null, SAMPLE_ID_TOKEN, null);
        ArgumentCaptor<HttpEntity<HarmonyFetchDeepLinksBody>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(eq(deepLinkingUrl), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(HarmonyContentItemDTO[].class))).thenReturn(responseEntity);

        List<HarmonyContentItemDTO> deepLinkingContentItemsList = harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, false, null);

        assertNotNull(deepLinkingContentItemsList);
        HttpEntity<HarmonyFetchDeepLinksBody> httpEntity = httpEntityCaptor.getValue();
        assertEquals(body, httpEntity.getBody());
        assertEquals(2, deepLinkingContentItemsList.size());
        assertTrue(deepLinkingContentItemsList.contains(deepLinkingContentItemDTO1));
        assertTrue(deepLinkingContentItemsList.contains(deepLinkingContentItemDTO2));

    }

    @Test
    public void testGoodResponseForFetchDeepLinkingContentItemsForReturningUser() {
        ReflectionTestUtils.setField(harmonyService, HARMONY_COURSES_API_URL, MOCK_SERVER_URL);

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
        String deepLinkingUrl = MOCK_SERVER_URL + "/lti_deep_links?guid=" + SAMPLE_ROOT_OUTCOME_GUID + "&course_paired=true";
        HarmonyFetchDeepLinksBody body = new HarmonyFetchDeepLinksBody(null, SAMPLE_ID_TOKEN, List.of("module-id-1", "module-id-2", "module-id-3"));
        ArgumentCaptor<HttpEntity<HarmonyFetchDeepLinksBody>> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(eq(deepLinkingUrl), eq(HttpMethod.POST), any(), eq(HarmonyContentItemDTO[].class))).thenReturn(responseEntity);

        List<HarmonyContentItemDTO> deepLinkingContentItemsList = harmonyService.fetchDeepLinkingContentItems(SAMPLE_ROOT_OUTCOME_GUID, SAMPLE_ID_TOKEN, true, List.of("module-id-1", "module-id-2", "module-id-3"));

        verify(restTemplate).exchange(eq(deepLinkingUrl), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(HarmonyContentItemDTO[].class));
        assertNotNull(deepLinkingContentItemsList);
        HttpEntity<HarmonyFetchDeepLinksBody> httpEntity = httpEntityCaptor.getValue();
        assertEquals(body, httpEntity.getBody());
        assertEquals(2, deepLinkingContentItemsList.size());
        assertTrue(deepLinkingContentItemsList.contains(deepLinkingContentItemDTO1));
        assertTrue(deepLinkingContentItemsList.contains(deepLinkingContentItemDTO2));

    }

    @Test
    public void testPostLineitemsToHarmonyWithNullLineitems() {
        DataServiceException exception = Assertions.assertThrows(
                DataServiceException.class,
                () -> {
                    harmonyService.postLineitemsToHarmony(null, SAMPLE_ID_TOKEN);
                }
        );

        assertEquals("No lineitems to send to Harmony", exception.getMessage());
    }

    @Test
    public void testPostLineitemsToHarmonyWithEmptyLineitemsList() {
        DataServiceException exception = Assertions.assertThrows(
                DataServiceException.class,
                () -> {
                    harmonyService.postLineitemsToHarmony(new LineItems(), SAMPLE_ID_TOKEN);
                }
        );

        assertEquals("No lineitems to send to Harmony", exception.getMessage());
    }

    @Test
    public void testPostLineitemsToHarmonyWithNullLineitemsList() {
        LineItems lineItems = new LineItems();
        lineItems.setLineItemList(null);
        DataServiceException exception = Assertions.assertThrows(
                DataServiceException.class,
                () -> {
                    harmonyService.postLineitemsToHarmony(lineItems, SAMPLE_ID_TOKEN);
                }
        );

        assertEquals("No lineitems to send to Harmony", exception.getMessage());
    }

    @Test
    public void testPostLineitemsToHarmonyWithNullIdToken() {
        LineItems lineItems = new LineItems();
        lineItems.setLineItemList(List.of(new LineItem()));
        DataServiceException exception = Assertions.assertThrows(
                DataServiceException.class,
                () -> {
                    harmonyService.postLineitemsToHarmony(lineItems, null);
                }
        );

        assertEquals("Must include an id_token when posting lineitems to harmony.", exception.getMessage());
    }

    @Test
    public void testPostLineitemsToHarmonyWithEmptyIdToken() {
        LineItems lineItems = new LineItems();
        lineItems.setLineItemList(List.of(new LineItem()));
        DataServiceException exception = Assertions.assertThrows(
                DataServiceException.class,
                () -> {
                    harmonyService.postLineitemsToHarmony(lineItems, "");
                }
        );

        assertEquals("Must include an id_token when posting lineitems to harmony.", exception.getMessage());
    }

    @Test
    public void testPostLineitemsToHarmony() {
        try {
            LineItems lineItems = new LineItems();
            LineItem lineItem = new LineItem();
            lineItem.setId("https://lms.com/course/lineitem/1");
            lineItem.setScoreMaximum("100");
            lineItem.setLabel("Quiz 1");
            lineItems.setLineItemList(List.of(lineItem));
            ArgumentCaptor<HttpEntity<String>> httpEntityArgumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            when(restTemplate.exchange(eq(MOCK_SERVER_LINEITEMS_URL), eq(HttpMethod.POST), any(), eq(Map.class))).thenReturn(new ResponseEntity<>(Map.of("root_outcome_guid", "test-rog"), HttpStatus.OK));

            ResponseEntity<Map> response = harmonyService.postLineitemsToHarmony(lineItems, SAMPLE_ID_TOKEN);

            verify(restTemplate).exchange(eq(MOCK_SERVER_LINEITEMS_URL), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(), eq(Map.class));
            HttpEntity<String> entity = httpEntityArgumentCaptor.getValue();
            assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
            assertEquals("{\"lineitems\":[{\"id\":\"https://lms.com/course/lineitem/1\",\"scoreMaximum\":\"100\",\"label\":\"Quiz 1\"}],\"id_token\":\"sample-id-token\"}", entity.getBody());
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, String> rogMap = response.getBody();
            assertEquals("test-rog", rogMap.get("root_outcome_guid"));

        } catch (DataServiceException | JsonProcessingException e) {
            fail("Exception should not be thrown.");
        }
    }

}
