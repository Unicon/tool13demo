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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.ToolLink;
import net.unicon.lti.model.lti.dto.DeepLinkDTO;
import net.unicon.lti.model.lti.dto.DeepLinkJWTDTO;
import net.unicon.lti.repository.ToolLinkRepository;
import net.unicon.lti.service.lti.DeepLinkService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.*;


/**
 * This manages all the Membership call for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class DeepLinkServiceImpl implements DeepLinkService {

    @Autowired
    ToolLinkRepository toolLinks;

    @Autowired
    LTIJWTServiceImpl ltijwtService;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    static final Logger log = LoggerFactory.getLogger(DeepLinkServiceImpl.class);

    //Asking for a token with the right scope.
    @Override
    public List<DeepLinkDTO> getDeepLinks() {
       List<ToolLink> toolLinkList = toolLinks.findAll();
       List<DeepLinkDTO> deepLinkDTOS = new ArrayList<DeepLinkDTO>();
       for (ToolLink toolLink:toolLinkList){
           DeepLinkDTO deepLinkDTO = new DeepLinkDTO(toolLink.getToolLinkId(),toolLink.getTitle(), toolLink.getDescription());
           deepLinkDTOS.add(deepLinkDTO);
       }
       return deepLinkDTOS;
    }

    private static String listMapToJson(List<Map<String, Object>> listMap) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listMap);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return "";
        }
    }
    @Override
    public DeepLinkJWTDTO generateDeepLinkJWT(List<String> deepLinkRequestIds, Jws<Claims> id_token) throws GeneralSecurityException, IOException {

        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        DeepLinkJWTDTO deepLinkJWTDTO = new DeepLinkJWTDTO();

        List<Map<String,Object>> jsonDeepLinks = createDeepLinksJSON(deepLinkRequestIds);
        deepLinkJWTDTO.setJSONString(jsonDeepLinks);

        String jwt = Jwts.builder()
                .setHeaderParam(LtiStrings.TYP, LtiStrings.JWT)
                .setHeaderParam(LtiStrings.KID, TextConstants.DEFAULT_KID)
                .setHeaderParam(LtiStrings.ALG, LtiStrings.RS256)
                .setIssuer(Iterables.getOnlyElement(id_token.getBody().getAudience()))//Client ID
                .setAudience(id_token.getBody().getIssuer())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LtiStrings.LTI_NONCE, id_token.getBody().get(LtiStrings.LTI_NONCE, String.class))
                .claim(LtiStrings.LTI_AZP, id_token.getBody().getIssuer())
                .claim(LtiStrings.LTI_DEPLOYMENT_ID, id_token.getBody().get(LtiStrings.LTI_DEPLOYMENT_ID, String.class))
                .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
                .claim(LtiStrings.LTI_DATA, id_token.getBody().get(LtiStrings.DEEP_LINKING_SETTINGS, Map.class).get(LtiStrings.DEEP_LINK_DATA))
                .claim(LtiStrings.LTI_CONTENT_ITEMS, jsonDeepLinks)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();

        deepLinkJWTDTO.setJWTString(jwt);
        return deepLinkJWTDTO;
    }

    private List<Map<String,Object>> createDeepLinksJSON(List<String> deepLinkRequestIds) {
        List<Map<String,Object>> jsonList = new ArrayList<>();
        String localURL = ltiDataService.getLocalUrl();
        for (String id: deepLinkRequestIds){
            if (toolLinks.findById(id).isPresent()){
                ToolLink toolLink = toolLinks.findById(id).get();
                jsonList.add(createDeepLinkJson(localURL,toolLink));
            }
        }
        return jsonList;

    }

    static Map<String, Object> createDeepLinkJson(String localUrl,ToolLink toolLink) {
        Map<String, Object> deepLink = new HashMap<>();

        deepLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        deepLink.put(LtiStrings.DEEP_LINK_TITLE, toolLink.getTitle());
        deepLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=" + toolLink.getToolLinkId());
        if (toolLink.getIsAssignment()){
            deepLink.put("lineItem", lineItem(toolLink));
        }

        //Map<String, String> availableDates = new HashMap<>();
        //Map<String, String> submissionDates = new HashMap<>();
        //Map<String, String> custom = new HashMap<>();

        //availableDates.put("startDateTime", "2018-03-07T20:00:03Z");
        //availableDates.put("endDateTime", "2022-03-07T20:00:03Z");
        //submissionDates.put("startDateTime", "2019-03-07T20:00:03Z");
        //submissionDates.put("endDateTime", "2021-08-07T20:00:03Z");
        //custom.put("dueDate", "$Resource.submission.endDateTime");
        //custom.put("controlValue", "This is whatever I want to write here");
        //deepLink.put("available", availableDates);
        //deepLink.put("submission", submissionDates);
        //deepLink.put("custom", custom);
        return deepLink;
    }

    static Map<String, Object> lineItem(ToolLink toolLink) {
        Map<String, Object> lineItem = new HashMap<>();

        lineItem.put("scoreMaximum", toolLink.getMaxGrade());
        lineItem.put("label", toolLink.getTitle());
        lineItem.put("resourceId", toolLink.getToolLinkId());
        //deepLink.put("tag", "Tag);
        return lineItem;
    }


}
