package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import net.unicon.lti.utils.AGSScope;
import org.springframework.http.ResponseEntity;

public interface AdvantageAGSService {
    //Asking for a token with the right scope.
    LTIAdvantageToken getToken(AGSScope agsScope, PlatformDeployment platformDeployment) throws ConnectionException;

    //Calling the AGS service and getting a paginated result of lineitems.
    LineItems getLineItems(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context) throws ConnectionException;

    LineItems getLineItems(PlatformDeployment platformDeployment, String lineitemsUrl) throws ConnectionException, DataServiceException;

    LineItems getLineItems(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, boolean results, LTIAdvantageToken resultsToken) throws ConnectionException;

    boolean deleteLineItem(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, String id) throws ConnectionException;

    LineItem putLineItem(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;

    LineItem getLineItem(LTIAdvantageToken LTIAdvantageToken, LTIAdvantageToken LTIAdvantageTokenResults, LtiContextEntity context, String id) throws ConnectionException;

    LineItems postLineItem(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;

    Results getResults(LTIAdvantageToken LTIAdvantageTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException;

    Results postScore(LTIAdvantageToken LTIAdvantageTokenScores, LTIAdvantageToken LTIAdvantageTokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException;

    ResponseEntity<Void> postScore(LTIAdvantageToken LTIAdvantageTokenScores, String lineItemId, Score score) throws ConnectionException;

    void cleanLineItem(LineItem lineItem);
}
