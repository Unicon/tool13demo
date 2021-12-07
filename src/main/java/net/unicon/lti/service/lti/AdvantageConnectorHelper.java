package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIToken;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public interface AdvantageConnectorHelper {
    HttpEntity createRequestEntity(String apiKey);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity createTokenizedRequestEntity(LTIToken LTIToken);

    //TODO. I needed to change the score one to send a JSON string instead the object.
    // The Lineitems are working fine as they are, but if someday they don't work we will need
    // to do the same.

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<LineItem> createTokenizedRequestEntity(LTIToken LTIToken, LineItem lineItem, String type);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<LineItems> createTokenizedRequestEntity(LTIToken LTIToken, LineItems lineItems);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<String> createTokenizedRequestEntity(LTIToken LTIToken, String score);

    //Asking for a token. The scope will come in the scope parameter
    //The platformDeployment has the URL to ask for the token.
    LTIToken getToken(PlatformDeployment platformDeployment, String scope) throws ConnectionException;

    RestTemplate createRestTemplate();

    String nextPage(HttpHeaders headers);
}
