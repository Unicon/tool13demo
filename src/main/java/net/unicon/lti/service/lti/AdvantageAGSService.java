package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;

public interface AdvantageAGSService {
    //Asking for a token with the right scope.
    LTIToken getToken(String scope, PlatformDeployment platformDeployment) throws ConnectionException;

    //Calling the AGS service and getting a paginated result of lineitems.
    LineItems getLineItems(LTIToken LTIToken, LtiContextEntity context) throws ConnectionException;

    LineItems getLineItems(LTIToken LTIToken, LtiContextEntity context, boolean results, LTIToken resultsToken) throws ConnectionException;

    boolean deleteLineItem(LTIToken LTIToken, LtiContextEntity context, String id) throws ConnectionException;

    LineItem putLineItem(LTIToken LTIToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;

    LineItem getLineItem(LTIToken LTIToken, LTIToken LTITokenResults, LtiContextEntity context, String id) throws ConnectionException;

    LineItems postLineItem(LTIToken LTIToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException;

    Results getResults(LTIToken LTITokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException;

    Results postScore(LTIToken LTITokenScores, LTIToken LTITokenResults,LtiContextEntity context, String lineItemId, Score score) throws ConnectionException;

    void cleanLineItem(LineItem lineItem);
}
