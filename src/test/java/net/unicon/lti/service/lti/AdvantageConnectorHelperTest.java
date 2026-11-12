package net.unicon.lti.service.lti;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import org.springframework.test.util.ReflectionTestUtils;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.impl.AdvantageConnectorHelperImpl;
import net.unicon.lti.utils.AGSScope;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdvantageConnectorHelperTest {

    @InjectMocks
    AdvantageConnectorHelperImpl advantageConnectorHelper;

    @Mock
    LTIJWTService ltijwtService;

    @Mock
    RestTemplate restTemplate;

    @Mock
    ExceptionMessageGenerator exceptionMessageGenerator;

    @Captor
    ArgumentCaptor<JacksonJsonHttpMessageConverter> converterArgumentCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // created lazily rather than injected, so Mockito's constructor injection doesn't set it
        ReflectionTestUtils.setField(advantageConnectorHelper, "restTemplate", restTemplate);
    }

    @Test
    public void testCreateTokenizedRequestEntityForScores() {
        LTIToken ltiToken = new LTIToken();
        ltiToken.setAccess_token("test-token");
        Score score = new Score();
        HttpEntity<Score> httpEntity = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken, score);

        assertEquals(httpEntity.getBody(), score);
        assertEquals(httpEntity.getHeaders().get("Authorization").get(0), TextConstants.BEARER + "test-token");
        assertEquals(httpEntity.getHeaders().get("Content-Type").get(0), "application/vnd.ims.lis.v1.score+json");
    }

    @Test
    public void testCreateTokenizedRequestEntityForScoresThrowsNPE() {
        Score score = new Score();

        assertThrows(NullPointerException.class, () -> {
            advantageConnectorHelper.createTokenizedRequestEntity(null, score);
        });
    }

    @Test
    public void testGetTokenForAGSScores() {
        try {
            PlatformDeployment platformDeployment = new PlatformDeployment();
            platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
            when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("jwt");
            LTIToken ltiToken = new LTIToken();
            ResponseEntity<LTIToken> responseEntity = new ResponseEntity<>(ltiToken, HttpStatus.OK);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(responseEntity);

            LTIToken ltiTokenResponse = advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());

            verify(ltijwtService).generateStateOrClientAssertionJWT(platformDeployment);
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            assertEquals(ltiTokenResponse, ltiToken);
        } catch (Exception e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testGetTokenForAGSScoresInMoodleFormat() throws Exception {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
        when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("jwt");
        LTIToken ltiToken = new LTIToken();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_HTML);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(new ResponseEntity<>(ltiToken, responseHeaders, HttpStatus.OK));

        LTIToken ltiTokenResponse = advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());

        assertEquals(ltiToken, ltiTokenResponse);
        verify(ltijwtService).generateStateOrClientAssertionJWT(platformDeployment);
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
        // the text/html converter is configured once, on the token client, not added on each request
        verify(restTemplate, never()).getMessageConverters();
    }

    @Test
    public void testGetTokenForAGSScoresPostIsUnsuccessful() {
        try {
            ConnectionException exception = assertThrows(ConnectionException.class, () -> {
                PlatformDeployment platformDeployment = new PlatformDeployment();
                platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
                when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("jwt");
                LTIToken ltiToken = new LTIToken();
                ResponseEntity<LTIToken> responseEntity = new ResponseEntity<>(ltiToken, HttpStatus.BAD_REQUEST);
                when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(responseEntity);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });

            verify(ltijwtService).generateStateOrClientAssertionJWT(any(PlatformDeployment.class));
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            assertEquals("Can't get the token: 400", exception.getMessage());
        } catch (GeneralSecurityException | IOException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testGetTokenForAGSScoresOAuthFails() {
        try {
            assertThrows(ConnectionException.class, () -> {
                PlatformDeployment platformDeployment = new PlatformDeployment();
                platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
                when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenThrow(GeneralSecurityException.class);

                advantageConnectorHelper.getToken(platformDeployment, AGSScope.AGS_SCORES_SCOPE.getScope());
            });
            verify(ltijwtService).generateStateOrClientAssertionJWT(any(PlatformDeployment.class));
            verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
            verify(exceptionMessageGenerator).exceptionMessage(eq("Can't get the token. Exception"), any(GeneralSecurityException.class));
        } catch (GeneralSecurityException | IOException e) {
            fail("Should not throw exception.");
        }
    }

    private static LTIToken token(String value) {
        LTIToken t = new LTIToken();
        t.setAccess_token(value);
        return t;
    }

    @Test
    public void testCreateRequestEntityUsesApiKeyAsBearer() {
        HttpEntity<?> entity = advantageConnectorHelper.createRequestEntity("api-key");

        assertEquals(TextConstants.BEARER + "api-key", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertNull(entity.getBody());
    }

    @Test
    public void testCreateTokenizedRequestEntityWithTokenOnly() {
        HttpEntity<?> entity = advantageConnectorHelper.createTokenizedRequestEntity(token("abc"));

        assertEquals(TextConstants.BEARER + "abc", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertNull(entity.getBody());
    }

    @Test
    public void testCreateTokenizedRequestEntityForLineItemWithContentType() {
        LineItem lineItem = new LineItem();
        HttpEntity<LineItem> entity = advantageConnectorHelper.createTokenizedRequestEntity(token("abc"), lineItem, TextConstants.LINEITEM_TYPE);

        assertSame(lineItem, entity.getBody());
        assertEquals(TextConstants.BEARER + "abc", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(TextConstants.LINEITEM_TYPE, entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void testCreateTokenizedRequestEntityForLineItemWithoutContentType() {
        LineItem lineItem = new LineItem();
        HttpEntity<LineItem> entity = advantageConnectorHelper.createTokenizedRequestEntity(token("abc"), lineItem, null);

        assertSame(lineItem, entity.getBody());
        assertFalse(entity.getHeaders().containsHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void testCreateTokenizedRequestEntityWithAcceptType() {
        HttpEntity<LineItem> entity = advantageConnectorHelper.createTokenizedRequestEntity(token("abc"), TextConstants.RESULTS_TYPE);

        assertEquals(TextConstants.BEARER + "abc", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(TextConstants.RESULTS_TYPE, entity.getHeaders().getFirst(HttpHeaders.ACCEPT));
        assertNull(entity.getBody());
    }

    @Test
    public void testCreateTokenizedRequestEntityWithNullAcceptType() {
        HttpEntity<LineItem> entity = advantageConnectorHelper.createTokenizedRequestEntity(token("abc"), (String) null);

        assertFalse(entity.getHeaders().containsHeader(HttpHeaders.ACCEPT));
        assertEquals(TextConstants.BEARER + "abc", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testCreateTokenizedRequestEntityForLineItems() {
        LineItems lineItems = new LineItems();
        HttpEntity<LineItems> entity = advantageConnectorHelper.createTokenizedRequestEntity(token("abc"), lineItems);

        assertSame(lineItems, entity.getBody());
        assertEquals(TextConstants.BEARER + "abc", entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    public void testGetTokenSendsFormEncodedClientCredentials() throws Exception {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
        when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("signed-jwt");
        LTIToken ltiToken = new LTIToken();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class)))
                .thenReturn(new ResponseEntity<>(ltiToken, HttpStatus.OK));

        assertSame(ltiToken, advantageConnectorHelper.getToken(platformDeployment, "the-scope"));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://lms.com/oauth2/token"), captor.capture(), eq(LTIToken.class));
        HttpEntity<?> sent = captor.getValue();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, sent.getHeaders().getContentType());
        MultiValueMap<String, String> form = (MultiValueMap<String, String>) sent.getBody();
        assertEquals("client_credentials", form.getFirst("grant_type"));
        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", form.getFirst("client_assertion_type"));
        assertEquals("signed-jwt", form.getFirst("client_assertion"));
        assertEquals("the-scope", form.getFirst("scope"));
    }

    @Test
    public void testGetTokenRetriesWithJsonPayloadWhenFormPostFails() throws Exception {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
        when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("signed-jwt");
        LTIToken ltiToken = new LTIToken();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class)))
                .thenThrow(new RestClientException("unsupported form"))
                .thenReturn(new ResponseEntity<>(ltiToken, HttpStatus.OK));

        assertSame(ltiToken, advantageConnectorHelper.getToken(platformDeployment, "the-scope"));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).postForEntity(eq("https://lms.com/oauth2/token"), captor.capture(), eq(LTIToken.class));
        HttpEntity<?> retry = captor.getAllValues().get(1);
        assertEquals(MediaType.APPLICATION_JSON, retry.getHeaders().getContentType());
        JSONObject json = new JSONObject((String) retry.getBody());
        assertEquals("client_credentials", json.getString("grant_type"));
        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", json.getString("client_assertion_type"));
        assertEquals("signed-jwt", json.getString("client_assertion"));
        assertEquals("the-scope", json.getString("scope"));
    }

    @Test
    public void testGetTokenCreatesATokenClientThatReadsTextHtml() throws Exception {
        ReflectionTestUtils.setField(advantageConnectorHelper, "restTemplate", null);
        PlatformDeployment platformDeployment = new PlatformDeployment();
        // not an absolute URL, so the request fails before any connection is made
        platformDeployment.setoAuth2TokenUrl("oauth2/token");
        when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("signed-jwt");

        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "the-scope"));
        RestTemplate tokenClient = (RestTemplate) ReflectionTestUtils.getField(advantageConnectorHelper, "restTemplate");
        int converters = tokenClient.getMessageConverters().size();
        assertThrows(ConnectionException.class, () -> advantageConnectorHelper.getToken(platformDeployment, "the-scope"));

        assertNotSame(advantageConnectorHelper.createRestTemplate(), tokenClient);
        assertSame(tokenClient, ReflectionTestUtils.getField(advantageConnectorHelper, "restTemplate"));
        assertInstanceOf(BufferingClientHttpRequestFactory.class, tokenClient.getRequestFactory());
        assertTrue(tokenClient.getMessageConverters().stream()
            .anyMatch(converter -> converter instanceof JacksonJsonHttpMessageConverter json && json.getSupportedMediaTypes().contains(MediaType.TEXT_HTML)));
        // a second request doesn't add another converter
        assertEquals(converters, tokenClient.getMessageConverters().size());
    }

    private PlatformDeployment tokenPlatform(long keyId) throws Exception {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        platformDeployment.setKeyId(keyId);
        platformDeployment.setClientId("client-1");
        platformDeployment.setoAuth2TokenUrl("https://lms.com/oauth2/token");
        when(ltijwtService.generateStateOrClientAssertionJWT(platformDeployment)).thenReturn("jwt");
        return platformDeployment;
    }

    private static LTIToken tokenExpiringIn(int seconds) {
        LTIToken token = new LTIToken();
        token.setAccess_token("token-" + seconds);
        token.setExpires_in(seconds);
        return token;
    }

    @Test
    public void testGetTokenReusesATokenUntilItNearlyExpires() throws Exception {
        PlatformDeployment platformDeployment = tokenPlatform(1L);
        LTIToken token = tokenExpiringIn(3600);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(new ResponseEntity<>(token, HttpStatus.OK));

        assertSame(token, advantageConnectorHelper.getToken(platformDeployment, "scope-a"));
        assertSame(token, advantageConnectorHelper.getToken(platformDeployment, "scope-a"));

        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
    }

    @Test
    public void testGetTokenKeepsTokensApartByScopeAndDeployment() throws Exception {
        PlatformDeployment first = tokenPlatform(1L);
        PlatformDeployment second = tokenPlatform(2L);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class)))
            .thenReturn(new ResponseEntity<>(tokenExpiringIn(3600), HttpStatus.OK), new ResponseEntity<>(tokenExpiringIn(3601), HttpStatus.OK), new ResponseEntity<>(tokenExpiringIn(3602), HttpStatus.OK));

        LTIToken scopeA = advantageConnectorHelper.getToken(first, "scope-a");
        LTIToken scopeB = advantageConnectorHelper.getToken(first, "scope-b");
        LTIToken otherDeployment = advantageConnectorHelper.getToken(second, "scope-a");

        assertNotSame(scopeA, scopeB);
        assertNotSame(scopeA, otherDeployment);
        verify(restTemplate, times(3)).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
    }

    @Test
    public void testGetTokenFetchesAgainWhenTheCachedTokenIsAboutToExpire() throws Exception {
        PlatformDeployment platformDeployment = tokenPlatform(1L);
        // inside the 60 second margin, so it's never reused
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(new ResponseEntity<>(tokenExpiringIn(30), HttpStatus.OK));

        advantageConnectorHelper.getToken(platformDeployment, "scope-a");
        advantageConnectorHelper.getToken(platformDeployment, "scope-a");

        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
    }

    @Test
    public void testGetTokenDoesNotCacheATokenWithoutAnExpiry() throws Exception {
        PlatformDeployment platformDeployment = tokenPlatform(1L);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(new ResponseEntity<>(tokenExpiringIn(0), HttpStatus.OK));

        advantageConnectorHelper.getToken(platformDeployment, "scope-a");
        advantageConnectorHelper.getToken(platformDeployment, "scope-a");

        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
    }

    @Test
    public void testGetTokenDoesNotCacheAnEmptyResponse() throws Exception {
        PlatformDeployment platformDeployment = tokenPlatform(1L);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertNull(advantageConnectorHelper.getToken(platformDeployment, "scope-a"));
        assertNull(advantageConnectorHelper.getToken(platformDeployment, "scope-a"));

        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), eq(LTIToken.class));
    }

    @Test
    public void testCreateRestTemplateIsShared() {
        assertSame(advantageConnectorHelper.createRestTemplate(), advantageConnectorHelper.createRestTemplate());
    }

    @Test
    public void testCreateRestTemplateUsesBufferingRequestFactory() {
        RestTemplate created = advantageConnectorHelper.createRestTemplate();

        assertInstanceOf(BufferingClientHttpRequestFactory.class, created.getRequestFactory());
    }

    @Test
    public void testNextPageReturnsNullWithoutLinkHeader() {
        assertNull(advantageConnectorHelper.nextPage(new HttpHeaders()));
    }

    @Test
    public void testNextPageReturnsNullWhenNoNextRel() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("link", "<https://lms.com/items?page=1>; rel=\"current\",<https://lms.com/items?page=3>; rel=\"last\"");

        assertNull(advantageConnectorHelper.nextPage(headers));
    }

    @Test
    public void testNextPageReturnsNullWhenNextUrlIsEmpty() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("link", "<>; rel=\"next\"");

        assertNull(advantageConnectorHelper.nextPage(headers));
    }

    @Test
    public void testNextPageReturnsDecodedNextUrl() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("link", "<https://lms.com/items?page=1>; rel=\"current\",<https://lms.com/items?page=2&q=a%20b>; rel=\"next\"");

        assertEquals("https://lms.com/items?page=2&q=a b", advantageConnectorHelper.nextPage(headers));
    }
}
