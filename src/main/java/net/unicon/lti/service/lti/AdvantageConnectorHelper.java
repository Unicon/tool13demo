package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public interface AdvantageConnectorHelper {
    HttpEntity createRequestEntity(String apiKey);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity createTokenizedRequestEntity(LTIAdvantageToken LTIAdvantageToken);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<LineItem> createTokenizedRequestEntity(LTIAdvantageToken LTIAdvantageToken, LineItem lineItem, String type);

    HttpEntity<LineItem> createTokenizedRequestEntity(LTIAdvantageToken LTIAdvantageToken, String type);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<LineItems> createTokenizedRequestEntity(LTIAdvantageToken LTIAdvantageToken, LineItems lineItems);

    // We put the token in the Authorization as a simple Bearer one.
    HttpEntity<Score> createTokenizedRequestEntity(LTIAdvantageToken LTIAdvantageToken, Score score);

    //Asking for a token. The scope will come in the scope parameter
    //The platformDeployment has the URL to ask for the token.
    LTIAdvantageToken getToken(PlatformDeployment platformDeployment, String scope) throws ConnectionException;

    RestTemplate createRestTemplate();

    String nextPage(HttpHeaders headers);
}
