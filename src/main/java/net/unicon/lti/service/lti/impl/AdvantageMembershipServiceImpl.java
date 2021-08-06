/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti.service.lti.impl;

import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.membership.CourseUser;
import net.unicon.lti.model.membership.CourseUsers;
import net.unicon.lti.model.oauth2.LTIToken;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.service.lti.AdvantageMembershipService;
import net.unicon.lti.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This manages all the Membership call for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class AdvantageMembershipServiceImpl implements AdvantageMembershipService {

    @Autowired
    AdvantageConnectorHelper advantageConnectorHelper;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    static final Logger log = LoggerFactory.getLogger(AdvantageMembershipServiceImpl.class);

    //Asking for a token with the right scope.
    @Override
    public LTIToken getToken(PlatformDeployment platformDeployment) throws ConnectionException {
        String scope = "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly";
        return advantageConnectorHelper.getToken(platformDeployment, scope);
    }

    //Calling the membership service and getting a paginated result of users.
    @Override
    public CourseUsers callMembershipService(LTIToken LTIToken, LtiContextEntity context) throws ConnectionException {
        CourseUsers courseUsers;
        log.debug(TextConstants.TOKEN + LTIToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIToken);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_MEMBERSHIP = context.getContext_memberships_url();
            log.debug("GET_MEMBERSHIP -  " + GET_MEMBERSHIP);
            ResponseEntity<CourseUsers> membershipGetResponse = restTemplate.
                    exchange(GET_MEMBERSHIP, HttpMethod.GET, request, CourseUsers.class);
            ResponseEntity<String> membershipGetResponse2 = restTemplate.
                    exchange(GET_MEMBERSHIP, HttpMethod.GET, request, String.class);
            HttpStatus status = membershipGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                courseUsers = membershipGetResponse.getBody();
                List<CourseUser> courseUserList = new ArrayList<>(Objects.requireNonNull(courseUsers).getCourseUserList());
                //We deal here with pagination
                log.debug("We have {} users", courseUsers.getCourseUserList().size());
                String nextPage = advantageConnectorHelper.nextPage(membershipGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    ResponseEntity<CourseUsers> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                            request, CourseUsers.class);
                    CourseUsers nextCourseList = responseForNextPage.getBody();
                    List<CourseUser> nextCourseUsersList = Objects.requireNonNull(nextCourseList)
                            .getCourseUserList();
                    log.debug("We have {} users in the next page", nextCourseList.getCourseUserList().size());
                    courseUserList.addAll(nextCourseUsersList);
                    nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                }
                courseUsers = new CourseUsers();
                courseUsers.getCourseUserList().addAll(courseUserList);
            } else {
                String exceptionMsg = "Can't get the membership";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the membership");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return courseUsers;
    }


}
