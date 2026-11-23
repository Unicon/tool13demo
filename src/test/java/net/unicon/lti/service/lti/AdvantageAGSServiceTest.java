package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Result;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.impl.AdvantageAGSServiceImpl;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AdvantageAGSServiceTest {

    private static final String LINEITEMS_URL = "https://lms.com/api/lti/courses/1/line_items";

    AdvantageAGSService advantageAGSService;

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    AdvantageConnectorHelper advantageConnectorHelper;

    @Mock
    RestTemplate restTemplate;

    private LTIToken token;
    private LTIToken resultsToken;
    private LtiContextEntity context;
    private final HttpEntity<LineItem> request = new HttpEntity<>(new HttpHeaders());

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        advantageAGSService = new AdvantageAGSServiceImpl(advantageConnectorHelper, exceptionMessageGenerator);
        when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any(Exception.class)))
                .thenAnswer(inv -> inv.getArgument(0) + " | " + ((Exception) inv.getArgument(1)).getMessage());
        token = token("lineitem-token");
        resultsToken = token("results-token");
        context = new LtiContextEntity();
        context.setLineitems(LINEITEMS_URL);
    }

    private static LTIToken token(String value) {
        LTIToken t = new LTIToken();
        t.setAccess_token(value);
        return t;
    }

    private static LineItem lineItem(String id) {
        LineItem li = new LineItem();
        li.setId(id);
        return li;
    }

    private static Result result(String userId) {
        Result r = new Result();
        r.setUserId(userId);
        return r;
    }

    private static HttpHeaders linkHeaders(String marker) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("link", marker);
        return headers;
    }

    @Test
    public void testGetTokenUsesLineItemScopeByDefault() throws ConnectionException {
        PlatformDeployment pd = new PlatformDeployment();
        when(advantageConnectorHelper.getToken(pd, "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem")).thenReturn(token);

        assertSame(token, advantageAGSService.getToken("lineitems", pd));
    }

    @Test
    public void testGetTokenUsesResultsScope() throws ConnectionException {
        PlatformDeployment pd = new PlatformDeployment();
        when(advantageConnectorHelper.getToken(pd, "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly")).thenReturn(resultsToken);

        assertSame(resultsToken, advantageAGSService.getToken("results", pd));
    }

    @Test
    public void testGetTokenUsesScoresScope() throws ConnectionException {
        PlatformDeployment pd = new PlatformDeployment();
        LTIToken scores = token("scores");
        when(advantageConnectorHelper.getToken(pd, "https://purl.imsglobal.org/spec/lti-ags/scope/score")).thenReturn(scores);

        assertSame(scores, advantageAGSService.getToken("scores", pd));
    }

    @Test
    public void testGetLineItemsSinglePageWithoutResults() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.ALL_LINEITEMS_TYPE)).thenReturn(request);
        LineItem a = lineItem("a");
        LineItem b = lineItem("b");
        when(restTemplate.exchange(LINEITEMS_URL, HttpMethod.GET, request, LineItem[].class))
                .thenReturn(new ResponseEntity<>(new LineItem[]{a, b}, HttpStatus.OK));

        LineItems lineItems = advantageAGSService.getLineItems(token, context);

        assertEquals(List.of(a, b), lineItems.getLineItemList());
        assertNull(a.getResults());
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Result[].class));
    }

    @Test
    public void testGetLineItemsFollowsPaginationAndLoadsResults() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.ALL_LINEITEMS_TYPE)).thenReturn(request);
        HttpEntity<LineItem> resultsRequest = new HttpEntity<>(new HttpHeaders());
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(resultsRequest);
        LineItem a = lineItem("https://lms.com/li/a");
        LineItem b = lineItem("https://lms.com/li/b");
        HttpHeaders page1Headers = linkHeaders("page1");
        HttpHeaders page2Headers = linkHeaders("page2");
        when(restTemplate.exchange(LINEITEMS_URL, HttpMethod.GET, request, LineItem[].class))
                .thenReturn(new ResponseEntity<>(new LineItem[]{a}, page1Headers, HttpStatus.OK));
        when(restTemplate.exchange("https://lms.com/page2", HttpMethod.GET, request, LineItem[].class))
                .thenReturn(new ResponseEntity<>(new LineItem[]{b}, page2Headers, HttpStatus.OK));
        when(advantageConnectorHelper.nextPage(page1Headers)).thenReturn("https://lms.com/page2");
        Result ra = result("user-a");
        Result rb = result("user-b");
        when(restTemplate.exchange("https://lms.com/li/a/results", HttpMethod.GET, resultsRequest, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[]{ra}, HttpStatus.OK));
        when(restTemplate.exchange("https://lms.com/li/b/results", HttpMethod.GET, resultsRequest, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[]{rb}, HttpStatus.OK));

        LineItems lineItems = advantageAGSService.getLineItems(token, context, true, resultsToken);

        assertEquals(List.of(a, b), lineItems.getLineItemList());
        assertEquals(List.of(ra), a.getResults().getResultList());
        assertEquals(List.of(rb), b.getResults().getResultList());
        verify(advantageConnectorHelper).nextPage(page2Headers);
    }

    @Test
    public void testGetLineItemsNon2xxIsWrappedInConnectionException() {
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.ALL_LINEITEMS_TYPE)).thenReturn(request);
        when(restTemplate.exchange(LINEITEMS_URL, HttpMethod.GET, request, LineItem[].class))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));

        ConnectionException ex = assertThrows(ConnectionException.class, () -> advantageAGSService.getLineItems(token, context));

        assertEquals("Can't get the AGS | Can't get the AGS", ex.getMessage());
    }

    @Test
    public void testGetLineItemsRestExceptionIsWrapped() {
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.ALL_LINEITEMS_TYPE)).thenReturn(request);
        when(restTemplate.exchange(LINEITEMS_URL, HttpMethod.GET, request, LineItem[].class))
                .thenThrow(new RestClientException("boom"));

        ConnectionException ex = assertThrows(ConnectionException.class, () -> advantageAGSService.getLineItems(token, context));

        assertEquals("Can't get the AGS | boom", ex.getMessage());
    }

    @Test
    public void testDeleteLineItemSuccess() throws ConnectionException {
        HttpEntity<?> deleteRequest = new HttpEntity<>(new HttpHeaders());
        when(advantageConnectorHelper.createTokenizedRequestEntity(token)).thenReturn(deleteRequest);
        when(restTemplate.exchange(LINEITEMS_URL + "/42", HttpMethod.DELETE, deleteRequest, String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        assertTrue(advantageAGSService.deleteLineItem(token, context, "42"));
    }

    @Test
    public void testLineItemIdsMustBeOnePathSegment() throws ConnectionException {
        assertEquals("42", AdvantageAGSServiceImpl.lineItemPathSegment("42"));
        assertEquals("a-b_c.d~e", AdvantageAGSServiceImpl.lineItemPathSegment("a-b_c.d~e"));
        for (String bad : new String[]{null, "", ".", "..", "../..", "1/2", "1?x=2", "1#x", "1%2F2", "line item", "1\r\nforged"}) {
            ConnectionException ex = assertThrows(ConnectionException.class, () -> AdvantageAGSServiceImpl.lineItemPathSegment(bad), String.valueOf(bad));
            assertEquals("The line item id isn't valid", ex.getMessage());
        }
    }

    @Test
    public void testLineItemCallsRejectABadIdBeforeCallingThePlatform() {
        LineItem li = new LineItem();
        li.setId("../../users");
        assertThrows(ConnectionException.class, () -> advantageAGSService.deleteLineItem(token, context, "../../users"));
        assertThrows(ConnectionException.class, () -> advantageAGSService.putLineItem(token, context, li));
        assertThrows(ConnectionException.class, () -> advantageAGSService.getLineItem(token, token, context, "../../users"));
        verifyNoInteractions(advantageConnectorHelper);
    }

    @Test
    public void testDeleteLineItemNon2xx() {
        HttpEntity<?> deleteRequest = new HttpEntity<>(new HttpHeaders());
        when(advantageConnectorHelper.createTokenizedRequestEntity(token)).thenReturn(deleteRequest);
        when(restTemplate.exchange(LINEITEMS_URL + "/42", HttpMethod.DELETE, deleteRequest, String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        ConnectionException ex = assertThrows(ConnectionException.class, () -> advantageAGSService.deleteLineItem(token, context, "42"));

        assertEquals("Can't delete the lineitem with id 42 | Can't delete the lineitem with id: 42", ex.getMessage());
    }

    @Test
    public void testPutLineItemSuccess() throws ConnectionException {
        LineItem li = lineItem("7");
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, li, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        LineItem updated = lineItem("7");
        updated.setLabel("updated");
        when(restTemplate.exchange(LINEITEMS_URL + "/7", HttpMethod.PUT, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(updated, HttpStatus.OK));

        assertSame(updated, advantageAGSService.putLineItem(token, context, li));
    }

    @Test
    public void testPutLineItemNon2xx() {
        LineItem li = lineItem("7");
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, li, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        when(restTemplate.exchange(LINEITEMS_URL + "/7", HttpMethod.PUT, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        ConnectionException ex = assertThrows(ConnectionException.class, () -> advantageAGSService.putLineItem(token, context, li));

        assertEquals("Can't put the lineitem 7 | Can't put the lineitem 7", ex.getMessage());
    }

    @Test
    public void testGetLineItemBuildsUrlAndLoadsResults() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        HttpEntity<LineItem> resultsRequest = new HttpEntity<>(new HttpHeaders());
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(resultsRequest);
        LineItem li = lineItem(LINEITEMS_URL + "/9");
        when(restTemplate.exchange(LINEITEMS_URL + "/9", HttpMethod.GET, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(li, HttpStatus.OK));
        Result r = result("u1");
        when(restTemplate.exchange(LINEITEMS_URL + "/9/results", HttpMethod.GET, resultsRequest, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[]{r}, HttpStatus.OK));

        LineItem returned = advantageAGSService.getLineItem(token, resultsToken, context, "9");

        assertSame(li, returned);
        assertEquals(List.of(r), returned.getResults().getResultList());
    }

    @Test
    public void testGetLineItemUsesMoodleUrlFormat() throws ConnectionException {
        context.setLineitems("https://moodle.com/mod/lti/services.php/2/lineitems?type_id=3");
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        HttpEntity<LineItem> resultsRequest = new HttpEntity<>(new HttpHeaders());
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(resultsRequest);
        String moodleLineItemId = "https://moodle.com/mod/lti/services.php/2/lineitems/5/lineitem?type_id=3";
        LineItem li = lineItem(moodleLineItemId);
        when(restTemplate.exchange(moodleLineItemId, HttpMethod.GET, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(li, HttpStatus.OK));
        Result r = result("u1");
        when(restTemplate.exchange("https://moodle.com/mod/lti/services.php/2/lineitems/5/lineitem/results?type_id=3",
                HttpMethod.GET, resultsRequest, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[]{r}, HttpStatus.OK));

        LineItem returned = advantageAGSService.getLineItem(token, resultsToken, context, "5");

        assertEquals(List.of(r), returned.getResults().getResultList());
    }

    @Test
    public void testGetLineItemNon2xx() {
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        when(restTemplate.exchange(LINEITEMS_URL + "/9", HttpMethod.GET, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        ConnectionException ex = assertThrows(ConnectionException.class,
                () -> advantageAGSService.getLineItem(token, resultsToken, context, "9"));

        assertEquals("Can't get the lineitem 9 | Can't get the lineitem 9", ex.getMessage());
    }

    @Test
    public void testPostLineItemSuccess() throws ConnectionException {
        LineItem li = lineItem(null);
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, li, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        LineItem created = lineItem("new");
        when(restTemplate.exchange(LINEITEMS_URL, HttpMethod.POST, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(created, HttpStatus.CREATED));

        LineItems result = advantageAGSService.postLineItem(token, context, li);

        assertEquals(List.of(created), result.getLineItemList());
    }

    @Test
    public void testPostLineItemNon2xx() {
        LineItem li = lineItem(null);
        when(advantageConnectorHelper.createTokenizedRequestEntity(token, li, TextConstants.LINEITEM_TYPE)).thenReturn(request);
        when(restTemplate.exchange(LINEITEMS_URL, HttpMethod.POST, request, LineItem.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

        ConnectionException ex = assertThrows(ConnectionException.class, () -> advantageAGSService.postLineItem(token, context, li));

        assertEquals("Can't post lineitem | Can't post lineitem", ex.getMessage());
    }

    @Test
    public void testGetResultsFollowsPagination() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(request);
        HttpHeaders page1Headers = linkHeaders("page1");
        HttpHeaders page2Headers = linkHeaders("page2");
        Result r1 = result("u1");
        Result r2 = result("u2");
        when(restTemplate.exchange("https://lms.com/li/1/results", HttpMethod.GET, request, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[]{r1}, page1Headers, HttpStatus.OK));
        when(advantageConnectorHelper.nextPage(page1Headers)).thenReturn("https://lms.com/results/page2");
        Results nextPage = new Results();
        nextPage.getResultList().add(r2);
        when(restTemplate.exchange("https://lms.com/results/page2", HttpMethod.GET, request, Results.class))
                .thenReturn(new ResponseEntity<>(nextPage, page2Headers, HttpStatus.OK));

        Results results = advantageAGSService.getResults(resultsToken, context, "https://lms.com/li/1");

        assertEquals(List.of(r1, r2), results.getResultList());
        verify(advantageConnectorHelper).nextPage(page2Headers);
    }

    @Test
    public void testGetResultsUsesMoodleUrlFormat() throws ConnectionException {
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(request);
        when(restTemplate.exchange("https://moodle.com/li/1/lineitem/results?type_id=3", HttpMethod.GET, request, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[0], HttpStatus.OK));

        Results results = advantageAGSService.getResults(resultsToken, context, "https://moodle.com/li/1/lineitem?type_id=3");

        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testGetResultsNon2xx() {
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(request);
        when(restTemplate.exchange("https://lms.com/li/1/results", HttpMethod.GET, request, Result[].class))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        ConnectionException ex = assertThrows(ConnectionException.class,
                () -> advantageAGSService.getResults(resultsToken, context, "https://lms.com/li/1"));

        assertEquals("Can't get the AGS | Can't get the AGS", ex.getMessage());
    }

    @Test
    public void testPostScoreWithResultsReturnsRefreshedResults() throws ConnectionException {
        LTIToken scoresToken = token("scores-token");
        Score score = new Score();
        HttpEntity<Score> scoreRequest = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(scoresToken, score)).thenReturn(scoreRequest);
        when(restTemplate.exchange("https://lms.com/li/1/scores", HttpMethod.POST, scoreRequest, Void.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(request);
        Result r = result("u1");
        when(restTemplate.exchange("https://lms.com/li/1/results", HttpMethod.GET, request, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[]{r}, HttpStatus.OK));

        Results results = advantageAGSService.postScore(scoresToken, resultsToken, context, "https://lms.com/li/1", score);

        assertEquals(List.of(r), results.getResultList());
    }

    @Test
    public void testPostScoreWithResultsUsesMoodleUrlFormat() throws ConnectionException {
        LTIToken scoresToken = token("scores-token");
        Score score = new Score();
        HttpEntity<Score> scoreRequest = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(scoresToken, score)).thenReturn(scoreRequest);
        when(restTemplate.exchange("https://moodle.com/li/1/lineitem/scores?type_id=3", HttpMethod.POST, scoreRequest, Void.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(advantageConnectorHelper.createTokenizedRequestEntity(resultsToken, TextConstants.RESULTS_TYPE)).thenReturn(request);
        when(restTemplate.exchange("https://moodle.com/li/1/lineitem/results?type_id=3", HttpMethod.GET, request, Result[].class))
                .thenReturn(new ResponseEntity<>(new Result[0], HttpStatus.OK));

        Results results = advantageAGSService.postScore(scoresToken, resultsToken, context, "https://moodle.com/li/1/lineitem?type_id=3", score);

        assertTrue(results.getResultList().isEmpty());
    }

    @Test
    public void testPostScoreWithResultsNon2xx() {
        LTIToken scoresToken = token("scores-token");
        Score score = new Score();
        HttpEntity<Score> scoreRequest = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(scoresToken, score)).thenReturn(scoreRequest);
        when(restTemplate.exchange("https://lms.com/li/1/scores", HttpMethod.POST, scoreRequest, Void.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.UNPROCESSABLE_CONTENT));

        ConnectionException ex = assertThrows(ConnectionException.class,
                () -> advantageAGSService.postScore(scoresToken, resultsToken, context, "https://lms.com/li/1", score));

        assertEquals("Can't post scores | Can't post scores", ex.getMessage());
    }

    @Test
    public void testPostScore() {
        try {
            LTIToken ltiToken = new LTIToken();
            ltiToken.setAccess_token("test-scores-token");
            Score score = new Score();
            when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
            HttpEntity<Score> httpEntity = new HttpEntity<>(score);
            when(advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score)).thenReturn(httpEntity);
            ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenReturn(responseEntity);

            ResponseEntity<Void> response = advantageAGSService.postScore(ltiToken, "https://lms.com/line_item/456", score);
            verify(advantageConnectorHelper).createRestTemplate();
            verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiToken, score);
            verify(restTemplate).exchange(eq("https://lms.com/line_item/456/scores"), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
            assertEquals(response, responseEntity);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testPostScoreUsesMoodleUrlFormat() throws ConnectionException {
        LTIToken ltiToken = token("test-scores-token");
        Score score = new Score();
        HttpEntity<Score> httpEntity = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score)).thenReturn(httpEntity);
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange("https://moodle.com/li/1/lineitem/scores?type_id=3", HttpMethod.POST, httpEntity, Void.class))
                .thenReturn(responseEntity);

        assertSame(responseEntity, advantageAGSService.postScore(ltiToken, "https://moodle.com/li/1/lineitem?type_id=3", score));
    }

    @Test
    public void testPostScoreThrowsException() {
        LTIToken ltiToken = new LTIToken();
        ltiToken.setAccess_token("test-scores-token");
        Score score = new Score();
        when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
        HttpEntity<Score> httpEntity = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score)).thenReturn(httpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenThrow(RestClientException.class);

        assertThrows(RestClientException.class, () -> {
            advantageAGSService.postScore(ltiToken, "https://lms.com/line_item/456", score);
        });

        verify(advantageConnectorHelper).createRestTemplate();
        verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiToken, score);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
        verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
    }

    @Test
    public void testCleanLineItemNullsBlankFields() {
        LineItem li = new LineItem();
        li.setResourceId(" ");
        li.setResourceLinkId("");
        li.setStartDateTime("  ");
        li.setEndDateTime("");
        li.setTag(" ");

        advantageAGSService.cleanLineItem(li);

        assertNull(li.getResourceId());
        assertNull(li.getResourceLinkId());
        assertNull(li.getStartDateTime());
        assertNull(li.getEndDateTime());
        assertNull(li.getTag());
    }

    @Test
    public void testCleanLineItemKeepsPopulatedFields() {
        LineItem li = new LineItem();
        li.setResourceId("res");
        li.setResourceLinkId("link");
        li.setStartDateTime("2026-01-01T00:00:00Z");
        li.setEndDateTime("2026-02-01T00:00:00Z");
        li.setTag("tag");

        advantageAGSService.cleanLineItem(li);

        assertEquals("res", li.getResourceId());
        assertEquals("link", li.getResourceLinkId());
        assertEquals("2026-01-01T00:00:00Z", li.getStartDateTime());
        assertEquals("2026-02-01T00:00:00Z", li.getEndDateTime());
        assertEquals("tag", li.getTag());
    }
}
