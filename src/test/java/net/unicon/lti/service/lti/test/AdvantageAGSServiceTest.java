package net.unicon.lti.service.lti.test;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.service.lti.impl.AdvantageAGSServiceImpl;
import net.unicon.lti.utils.AGSScope;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdvantageAGSServiceTest {
    private static final String LINEITEMS_URL = "https://lms.com/line_items";

    @InjectMocks
    AdvantageAGSService advantageAGSService = new AdvantageAGSServiceImpl();

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    @Mock
    AdvantageConnectorHelper advantageConnectorHelper;

    @Mock
    RestTemplate restTemplate;

    PlatformDeployment platformDeployment = new PlatformDeployment();

    @BeforeEach
    public void setUp() {
        try {
            MockitoAnnotations.openMocks(this);
            platformDeployment.setIss("https://lms.com");
            platformDeployment.setClientId("sample-client-id");
            platformDeployment.setDeploymentId("sample-deployment-id");
            platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2");
            when(advantageConnectorHelper.createRestTemplate()).thenReturn(restTemplate);
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("sample-access-token");
            when(advantageConnectorHelper.getToken(eq(platformDeployment), eq(AGSScope.AGS_LINEITEMS_SCOPE.getScope()))).thenReturn(ltiAdvantageToken);
            when(advantageConnectorHelper.createTokenizedRequestEntity(eq(ltiAdvantageToken), eq(TextConstants.ALL_LINEITEMS_TYPE))).thenReturn(new HttpEntity(new HttpHeaders()));
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testPostScoreFromSQS() {
        try {
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-scores-token");
            Score score = new Score();
            HttpEntity<Score> httpEntity = new HttpEntity<>(score);
            when(advantageConnectorHelper.createTokenizedRequestEntity(ltiAdvantageToken, score)).thenReturn(httpEntity);
            ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenReturn(responseEntity);

            ResponseEntity<Void> response = advantageAGSService.postScore(ltiAdvantageToken, "https://lms.com/line_item/456", score);
            verify(advantageConnectorHelper).createRestTemplate();
            verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiAdvantageToken, score);
            verify(restTemplate).exchange(eq("https://lms.com/line_item/456/scores"), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
            assertEquals(response, responseEntity);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testPostScoreFromSQSWithMoodleURLFormat() {
        try {
            LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
            ltiAdvantageToken.setAccess_token("test-scores-token");
            Score score = new Score();
            HttpEntity<Score> httpEntity = new HttpEntity<>(score);
            when(advantageConnectorHelper.createTokenizedRequestEntity(ltiAdvantageToken, score)).thenReturn(httpEntity);
            ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenReturn(responseEntity);

            ResponseEntity<Void> response = advantageAGSService.postScore(ltiAdvantageToken, "https://lms.com/mod/lti/services.php/3/lineitems/6/lineitem?type_id=17", score);
            verify(advantageConnectorHelper).createRestTemplate();
            verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiAdvantageToken, score);
            verify(restTemplate).exchange(eq("https://lms.com/mod/lti/services.php/3/lineitems/6/lineitem/scores?type_id=17"), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
            assertEquals(response, responseEntity);
        } catch (ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testPostScoreFromSQSThrowsException() {
        LTIAdvantageToken ltiAdvantageToken = new LTIAdvantageToken();
        ltiAdvantageToken.setAccess_token("test-scores-token");
        Score score = new Score();
        HttpEntity<Score> httpEntity = new HttpEntity<>(score);
        when(advantageConnectorHelper.createTokenizedRequestEntity(ltiAdvantageToken, score)).thenReturn(httpEntity);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class))).thenThrow(RestClientException.class);

        assertThrows(RestClientException.class, () -> {
            advantageAGSService.postScore(ltiAdvantageToken, "https://lms.com/line_item/456", score);
        });

        verify(advantageConnectorHelper).createRestTemplate();
        verify(advantageConnectorHelper).createTokenizedRequestEntity(ltiAdvantageToken, score);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), eq(httpEntity), eq(Void.class));
        verify(exceptionMessageGenerator, never()).exceptionMessage(any(String.class), any(Exception.class));
    }

    @Test
    public void testGetLineItemsWithoutPlatformDeployment() {
        DataServiceException exception = assertThrows(DataServiceException.class, () -> {
            advantageAGSService.getLineItems(null, LINEITEMS_URL);
        });

        assertEquals("PlatformDeployment must not be null in order to get Line Items", exception.getMessage());
    }

    @Test
    public void testGetLineItemsWithoutLineItemsUrl() {
        DataServiceException exception = assertThrows(DataServiceException.class, () -> {
            advantageAGSService.getLineItems(platformDeployment, null);
        });

        assertEquals("LineitemsUrl must not be null or empty in order to get Line Items", exception.getMessage());
    }

    @Test
    public void testGetLineItemsUnsuccessfulResponse() {
        when(restTemplate.exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ConnectionException.class, () -> {
            advantageAGSService.getLineItems(platformDeployment, LINEITEMS_URL);
        });
    }

    @Test
    public void testGetLineItemsThrowsException() {
        when(restTemplate.exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                .thenThrow(new RestClientException("sample-exception"));

        assertThrows(ConnectionException.class, () -> {
            advantageAGSService.getLineItems(platformDeployment, LINEITEMS_URL);
        });
    }

    @Test
    public void testGetLineItemsOnePage() {
        try {
            LineItem lineItem = new LineItem();
            LineItem[] lineItemsArray = new LineItem[]{lineItem};
            when(restTemplate.exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                    .thenReturn(new ResponseEntity<>(lineItemsArray, HttpStatus.OK));

            LineItems lineItems = advantageAGSService.getLineItems(platformDeployment, LINEITEMS_URL);
            verify(restTemplate).exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class));
            verify(advantageConnectorHelper).nextPage(any());
            assertEquals(lineItem, lineItems.getLineItemList().get(0));
        } catch (ConnectionException | DataServiceException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testGetLineItemsMultiplePages() {
        try {
            String secondPageUrl = LINEITEMS_URL + "?page=2";
            LineItem lineItem = new LineItem();
            LineItem[] lineItemsArray = new LineItem[]{lineItem};
            LineItem lineItem2 = new LineItem();
            LineItem[] lineItemsArray2 = new LineItem[]{lineItem2};
            when(restTemplate.exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                    .thenReturn(new ResponseEntity<>(lineItemsArray, HttpStatus.OK));
            when(advantageConnectorHelper.nextPage(any())).thenReturn(secondPageUrl, null);
            when(restTemplate.exchange(eq(secondPageUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                    .thenReturn(new ResponseEntity<>(lineItemsArray2, HttpStatus.OK));

            LineItems lineItems = advantageAGSService.getLineItems(platformDeployment, LINEITEMS_URL);

            verify(restTemplate).exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class));
            verify(advantageConnectorHelper, times(2)).nextPage(any());
            verify(restTemplate).exchange(eq(secondPageUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class));
            assertEquals(2, lineItems.getLineItemList().size());
            assertTrue(lineItems.getLineItemList().containsAll(List.of(lineItem, lineItem2)));
        } catch (ConnectionException | DataServiceException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testGetLineItemsHttpD2LBug() {
        try {
            String secondPageUrl = "http://lms.com/line_items?page=2";
            LineItem lineItem = new LineItem();
            LineItem[] lineItemsArray = new LineItem[]{lineItem};
            LineItem lineItem2 = new LineItem();
            LineItem[] lineItemsArray2 = new LineItem[]{lineItem2};
            when(restTemplate.exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                    .thenReturn(new ResponseEntity<>(lineItemsArray, HttpStatus.OK));
            when(advantageConnectorHelper.nextPage(any())).thenReturn(secondPageUrl, null);
            when(restTemplate.exchange(eq(LINEITEMS_URL + "?page=2"), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class)))
                    .thenReturn(new ResponseEntity<>(lineItemsArray2, HttpStatus.OK));

            LineItems lineItems = advantageAGSService.getLineItems(platformDeployment, LINEITEMS_URL);

            verify(restTemplate).exchange(eq(LINEITEMS_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class));
            verify(advantageConnectorHelper, times(2)).nextPage(any());
            verify(restTemplate).exchange(eq(LINEITEMS_URL + "?page=2"), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineItem[].class));
            assertEquals(2, lineItems.getLineItemList().size());
            assertTrue(lineItems.getLineItemList().containsAll(List.of(lineItem, lineItem2)));
        } catch (ConnectionException | DataServiceException e) {
            fail("Exception should not be thrown.");
        }
    }
}
