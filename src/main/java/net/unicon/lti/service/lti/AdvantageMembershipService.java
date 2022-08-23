package net.unicon.lti.service.lti;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.membership.CourseUsers;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;

public interface AdvantageMembershipService {
    //Asking for a token with the right scope.
    LTIAdvantageToken getToken(PlatformDeployment platformDeployment) throws ConnectionException;

    //Calling the membership service and getting a paginated result of users.
    CourseUsers callMembershipService(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context) throws ConnectionException;
}
