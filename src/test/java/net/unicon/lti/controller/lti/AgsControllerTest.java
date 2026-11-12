package net.unicon.lti.controller.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.repository.LtiContextRepository;
import net.unicon.lti.repository.PlatformDeploymentRepository;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.utils.TextConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AgsControllerTest {

    private static final String JWT = "advantage-jwt";
    private static final String CONTEXT_KEY = "context-key";

    @InjectMocks
    private AgsController agsController;

    @Mock
    private LtiContextRepository ltiContextRepository;

    @Mock
    private APIJWTService apijwtService;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private AdvantageAGSService advantageAGSService;

    @Mock
    private HttpServletRequest req;

    private ExtendedModelMap model;
    private PlatformDeployment platformDeployment;
    private LtiContextEntity context;
    private LTIToken lineItemsToken;
    private LTIToken resultsToken;
    private LTIToken scoresToken;

    @BeforeEach
    public void setUp() throws ConnectionException {
        MockitoAnnotations.openMocks(this);
        model = new ExtendedModelMap();
        platformDeployment = new PlatformDeployment();
        context = new LtiContextEntity();
        lineItemsToken = token("lineitems");
        resultsToken = token("results");
        scoresToken = token("scores");
        when(advantageAGSService.getToken("lineitems", platformDeployment)).thenReturn(lineItemsToken);
        when(advantageAGSService.getToken("results", platformDeployment)).thenReturn(resultsToken);
        when(advantageAGSService.getToken("scores", platformDeployment)).thenReturn(scoresToken);
        when(ltiContextRepository.findByContextKeyAndPlatformDeployment(CONTEXT_KEY, platformDeployment)).thenReturn(context);
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

    private static LineItems lineItems(LineItem... items) {
        LineItems li = new LineItems();
        li.getLineItemList().addAll(List.of(items));
        return li;
    }

    private void validToken(boolean deploymentFound) {
        when(req.getParameter(TextConstants.ADVANTAGE_TOKEN)).thenReturn(JWT);
        Claims claims = mock(Claims.class);
        when(claims.get("platformDeploymentId")).thenReturn(5L);
        when(claims.get("contextKey")).thenReturn(CONTEXT_KEY);
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        when(apijwtService.validateToken(JWT)).thenReturn(jws);
        when(platformDeploymentRepository.findById(5L))
                .thenReturn(deploymentFound ? Optional.of(platformDeployment) : Optional.empty());
    }

    private void invalidToken() {
        when(req.getParameter(TextConstants.ADVANTAGE_TOKEN)).thenReturn(JWT);
        when(apijwtService.validateToken(JWT)).thenThrow(new JwtException("bad signature"));
    }

    private void assertNoSession() {
        assertEquals(true, model.getAttribute(TextConstants.NO_SESSION_VALUES));
        assertFalse(model.containsAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        verifyNoInteractions(apijwtService, platformDeploymentRepository, ltiContextRepository, advantageAGSService);
    }

    private void assertDeploymentNotFound() {
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertFalse(model.containsAttribute(TextConstants.LINEITEMS));
        assertFalse(model.containsAttribute(TextConstants.RESULTS));
        verifyNoInteractions(ltiContextRepository, advantageAGSService);
    }

    private void assertInvalidToken() {
        assertEquals(true, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertFalse(model.containsAttribute(TextConstants.LINEITEMS));
        verifyNoInteractions(platformDeploymentRepository, advantageAGSService);
    }

    @Test
    public void testGetLineItemsPopulatesModel() throws ConnectionException {
        validToken(true);
        LineItem a = lineItem("a");
        when(advantageAGSService.getLineItems(lineItemsToken, context, true, resultsToken)).thenReturn(lineItems(a));

        String view = agsController.agsGetLineItems(req, null, model);

        assertEquals("ltiAdvAgsMain", view);
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertEquals(List.of(a), model.getAttribute(TextConstants.LINEITEMS));
        assertEquals(JWT, model.getAttribute(TextConstants.ADVANTAGE_TOKEN));
    }

    @Test
    public void testGetLineItemsWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvAgsMain", agsController.agsGetLineItems(req, null, model));
        assertNoSession();
    }

    @Test
    public void testGetLineItemsUnknownDeployment() throws ConnectionException {
        validToken(false);

        assertEquals("ltiAdvAgsMain", agsController.agsGetLineItems(req, null, model));
        assertDeploymentNotFound();
    }

    @Test
    public void testGetLineItemsInvalidToken() throws ConnectionException {
        invalidToken();

        assertEquals("ltiAdvAgsMain", agsController.agsGetLineItems(req, null, model));
        assertInvalidToken();
    }

    @Test
    public void testGetLineItemsServiceFailureFlagsToken() throws ConnectionException {
        validToken(true);
        when(advantageAGSService.getLineItems(lineItemsToken, context, true, resultsToken)).thenThrow(new ConnectionException("down"));

        assertEquals("ltiAdvAgsMain", agsController.agsGetLineItems(req, null, model));
        assertEquals(true, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
        assertFalse(model.containsAttribute(TextConstants.LINEITEMS));
    }

    @Test
    public void testPostLineItemCleansPostsAndReloads() throws ConnectionException {
        validToken(true);
        LineItem posted = lineItem(null);
        LineItem created = lineItem("created");
        LineItem other = lineItem("other");
        when(advantageAGSService.postLineItem(lineItemsToken, context, posted)).thenReturn(lineItems(created));
        when(advantageAGSService.getLineItems(lineItemsToken, context, true, resultsToken)).thenReturn(lineItems(created, other));

        String view = agsController.agsPostLineItem(req, null, model, posted);

        assertEquals("ltiAdvAgsMain", view);
        InOrder order = inOrder(advantageAGSService);
        order.verify(advantageAGSService).cleanLineItem(posted);
        order.verify(advantageAGSService).postLineItem(lineItemsToken, context, posted);
        order.verify(advantageAGSService).getLineItems(lineItemsToken, context, true, resultsToken);
        assertEquals(List.of(created), model.getAttribute(TextConstants.RESULTS));
        assertEquals(List.of(created, other), model.getAttribute(TextConstants.LINEITEMS));
        assertEquals(JWT, model.getAttribute(TextConstants.ADVANTAGE_TOKEN));
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
    }

    @Test
    public void testPostLineItemWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvAgsMain", agsController.agsPostLineItem(req, null, model, new LineItem()));
        assertNoSession();
    }

    @Test
    public void testPostLineItemUnknownDeployment() throws ConnectionException {
        validToken(false);

        assertEquals("ltiAdvAgsMain", agsController.agsPostLineItem(req, null, model, new LineItem()));
        assertDeploymentNotFound();
    }

    @Test
    public void testPostLineItemInvalidToken() throws ConnectionException {
        invalidToken();

        assertEquals("ltiAdvAgsMain", agsController.agsPostLineItem(req, null, model, new LineItem()));
        assertInvalidToken();
    }

    @Test
    public void testGetLineItemPopulatesModel() throws ConnectionException {
        validToken(true);
        LineItem li = lineItem("9");
        when(advantageAGSService.getLineItem(lineItemsToken, resultsToken, context, "9")).thenReturn(li);

        String view = agsController.agsGetLineitem(req, null, model, "9");

        assertEquals("ltiAdvAgsDetail", view);
        assertEquals(List.of(li), model.getAttribute(TextConstants.LINEITEMS));
        assertEquals(JWT, model.getAttribute(TextConstants.ADVANTAGE_TOKEN));
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
    }

    @Test
    public void testGetLineItemWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvAgsDetail", agsController.agsGetLineitem(req, null, model, "9"));
        assertNoSession();
    }

    @Test
    public void testGetLineItemUnknownDeployment() throws ConnectionException {
        validToken(false);

        assertEquals("ltiAdvAgsDetail", agsController.agsGetLineitem(req, null, model, "9"));
        assertDeploymentNotFound();
    }

    @Test
    public void testGetLineItemInvalidToken() throws ConnectionException {
        invalidToken();

        assertEquals("ltiAdvAgsDetail", agsController.agsGetLineitem(req, null, model, "9"));
        assertInvalidToken();
    }

    @Test
    public void testPutLineItemSetsIdAndPopulatesModel() throws ConnectionException {
        validToken(true);
        LineItem body = new LineItem();
        body.setLabel("renamed");
        LineItem updated = lineItem("7");
        when(advantageAGSService.putLineItem(lineItemsToken, context, body)).thenReturn(updated);

        String view = agsController.agsPutLineitem(req, null, model, body, "7");

        assertEquals("ltiAdvAgsMain", view);
        assertEquals("7", body.getId());
        assertEquals(List.of(updated), model.getAttribute(TextConstants.RESULTS));
        assertEquals(JWT, model.getAttribute(TextConstants.ADVANTAGE_TOKEN));
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
    }

    @Test
    public void testPutLineItemWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvAgsMain", agsController.agsPutLineitem(req, null, model, new LineItem(), "7"));
        assertNoSession();
    }

    @Test
    public void testPutLineItemUnknownDeployment() throws ConnectionException {
        validToken(false);
        LineItem body = new LineItem();

        assertEquals("ltiAdvAgsMain", agsController.agsPutLineitem(req, null, model, body, "7"));
        assertDeploymentNotFound();
        assertNull(body.getId());
    }

    @Test
    public void testPutLineItemInvalidToken() throws ConnectionException {
        invalidToken();

        assertEquals("ltiAdvAgsMain", agsController.agsPutLineitem(req, null, model, new LineItem(), "7"));
        assertInvalidToken();
    }

    @Test
    public void testDeleteLineItemDeletesAndReloads() throws ConnectionException {
        validToken(true);
        LineItem remaining = lineItem("remaining");
        when(advantageAGSService.deleteLineItem(lineItemsToken, context, "3")).thenReturn(true);
        when(advantageAGSService.getLineItems(lineItemsToken, context)).thenReturn(lineItems(remaining));

        String view = agsController.agsPDeleteLineitem(req, null, model, "3");

        assertEquals("ltiAdvAgsMain", view);
        verify(advantageAGSService).deleteLineItem(lineItemsToken, context, "3");
        assertEquals(true, model.getAttribute("deleteResults"));
        assertEquals(List.of(remaining), model.getAttribute(TextConstants.LINEITEMS));
        assertEquals(JWT, model.getAttribute(TextConstants.ADVANTAGE_TOKEN));
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
    }

    @Test
    public void testDeleteLineItemWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvAgsMain", agsController.agsPDeleteLineitem(req, null, model, "3"));
        assertNoSession();
    }

    @Test
    public void testDeleteLineItemUnknownDeployment() throws ConnectionException {
        validToken(false);

        assertEquals("ltiAdvAgsMain", agsController.agsPDeleteLineitem(req, null, model, "3"));
        assertDeploymentNotFound();
    }

    @Test
    public void testDeleteLineItemInvalidToken() throws ConnectionException {
        invalidToken();

        assertEquals("ltiAdvAgsMain", agsController.agsPDeleteLineitem(req, null, model, "3"));
        assertInvalidToken();
    }

    @Test
    public void testPostScorePostsToLineItemAndReloadsIt() throws ConnectionException {
        validToken(true);
        Score score = new Score();
        LineItem before = lineItem("https://lms.com/li/4");
        LineItem after = lineItem("https://lms.com/li/4");
        after.setLabel("after");
        when(advantageAGSService.getLineItem(lineItemsToken, resultsToken, context, "4")).thenReturn(before, after);
        when(advantageAGSService.postScore(scoresToken, resultsToken, context, "https://lms.com/li/4", score)).thenReturn(new Results());

        String view = agsController.agsPostScore(req, null, model, score, "4");

        assertEquals("ltiAdvAgsDetail", view);
        verify(advantageAGSService).postScore(scoresToken, resultsToken, context, "https://lms.com/li/4", score);
        assertEquals(List.of(after), model.getAttribute(TextConstants.LINEITEMS));
        assertEquals(JWT, model.getAttribute(TextConstants.ADVANTAGE_TOKEN));
        assertEquals(false, model.getAttribute(TextConstants.NO_ADVANTAGE_TOKEN));
    }

    @Test
    public void testPostScoreWithoutToken() throws ConnectionException {
        assertEquals("ltiAdvAgsDetail", agsController.agsPostScore(req, null, model, new Score(), "4"));
        assertNoSession();
    }

    @Test
    public void testPostScoreUnknownDeployment() throws ConnectionException {
        validToken(false);

        assertEquals("ltiAdvAgsDetail", agsController.agsPostScore(req, null, model, new Score(), "4"));
        assertDeploymentNotFound();
    }

    @Test
    public void testPostScoreInvalidToken() throws ConnectionException {
        invalidToken();

        assertEquals("ltiAdvAgsDetail", agsController.agsPostScore(req, null, model, new Score(), "4"));
        assertInvalidToken();
        verify(advantageAGSService, never()).postScore(any(), any(), any(), anyString(), any());
    }
}
