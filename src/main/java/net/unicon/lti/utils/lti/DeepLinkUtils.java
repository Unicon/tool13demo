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
package net.unicon.lti.utils.lti;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DeepLinkUtils {

    private DeepLinkUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     *
     */
    public static Map<String, List<String>> generateDeepLinkJWT(LTIDataService ltiDataService, PlatformDeployment platformDeployment, LTI3Request lti3Request, String localUrl) throws GeneralSecurityException, IOException {

        Map<String, List<String>> deepLinkJwtMap = new TreeMap<>();
        Date date = new Date();

        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());

        // JWT 1:  Empty list of JSON
        String jwt1 = Jwts.builder()
                .setHeaderParam(LtiStrings.TYP, LtiStrings.JWT)
                .setHeaderParam(LtiStrings.KID, TextConstants.DEFAULT_KID)
                .setHeaderParam(LtiStrings.ALG, LtiStrings.RS256)
                .setIssuer(platformDeployment.getClientId())  //Client ID
                .setAudience(lti3Request.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
                .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
                .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
                .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
                .claim(LtiStrings.LTI_DATA, lti3Request.deepLinkData)
                .claim(LtiStrings.LTI_CONTENT_ITEMS, new HashMap<String, Object>())
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();

        List<String> jwt1List = new ArrayList<>();
        jwt1List.add(jwt1);
        jwt1List.add("{}");
        deepLinkJwtMap.put("Link 1 content: EMPTY json", jwt1List);

        //JWT 2: One ltiResourcelink
        List<Map<String, Object>> oneDeepLink = createOneDeepLinkWithGrades(localUrl);
        String jwt2 = Jwts.builder()
                .setHeaderParam(LtiStrings.TYP, LtiStrings.JWT)
                .setHeaderParam(LtiStrings.KID, TextConstants.DEFAULT_KID)
                .setHeaderParam(LtiStrings.ALG, LtiStrings.RS256)
                .setIssuer(platformDeployment.getClientId())  //Client ID
                .setAudience(lti3Request.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
                .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
                .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
                .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
                .claim(LtiStrings.LTI_DATA, lti3Request.deepLinkData)
                .claim(LtiStrings.LTI_CONTENT_ITEMS, oneDeepLink)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();

        List<String> jwt2List = new ArrayList<>();
        jwt2List.add(jwt2);
        jwt2List.add(listMapToJson(oneDeepLink));
        deepLinkJwtMap.put("Link 2 content: ONE Standard LTI Core Link (ltiResourceLink)", jwt2List);

        //JWT 2b: One link (not ltiResourcelink)
        List<Map<String, Object>> oneDeepLinkNoLti = createOneDeepLinkNoLti();
        String jwt2b = Jwts.builder()
                .setHeaderParam(LtiStrings.TYP, LtiStrings.JWT)
                .setHeaderParam(LtiStrings.KID, TextConstants.DEFAULT_KID)
                .setHeaderParam(LtiStrings.ALG, LtiStrings.RS256)
                .setIssuer(platformDeployment.getClientId())  //Client ID
                .setAudience(lti3Request.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
                .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
                .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
                .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
                .claim(LtiStrings.LTI_DATA, lti3Request.deepLinkData)
                .claim(LtiStrings.LTI_CONTENT_ITEMS, oneDeepLinkNoLti)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();

        List<String> jwt2bList = new ArrayList<>();
        jwt2bList.add(jwt2b);
        jwt2bList.add(listMapToJson(oneDeepLinkNoLti));
        deepLinkJwtMap.put("Link 3 content: ONE External (YouTube) Link (NON ltiResourceLink)", jwt2bList);

        //JWT 3: More than one link
        List<Map<String, Object>> multipleDeepLink = createMultipleDeepLink(localUrl);
        String jwt3 = Jwts.builder()
                .setHeaderParam(LtiStrings.TYP, LtiStrings.JWT)
                .setHeaderParam(LtiStrings.KID, TextConstants.DEFAULT_KID)
                .setHeaderParam(LtiStrings.ALG, LtiStrings.RS256)
                .setIssuer(platformDeployment.getClientId())  //This is our own identifier, to know that we are the issuer.
                .setAudience(lti3Request.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
                .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
                .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
                .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
                .claim(LtiStrings.LTI_DATA, lti3Request.deepLinkData)
                .claim(LtiStrings.LTI_CONTENT_ITEMS, multipleDeepLink)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();

        List<String> jwt3List = new ArrayList<>();
        jwt3List.add(jwt3);
        jwt3List.add(listMapToJson(multipleDeepLink));
        deepLinkJwtMap.put("Link 4 content: TWO Standard LTI Core Links (ltiResourceLinks) and TWO external links", jwt3List);

        //JWT 3b: More than one link but only ltiresourceLinks
        List<Map<String, Object>> multipleDeepLinkOnlyLti = createMultipleDeepLinkOnlyLti(localUrl);
        String jwt3b = Jwts.builder()
                .setHeaderParam(LtiStrings.TYP, LtiStrings.JWT)
                .setHeaderParam(LtiStrings.KID, TextConstants.DEFAULT_KID)
                .setHeaderParam(LtiStrings.ALG, LtiStrings.RS256)
                .setIssuer(platformDeployment.getClientId())  //This is our own identifier, to know that we are the issuer.
                .setAudience(lti3Request.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LtiStrings.LTI_NONCE, lti3Request.getNonce())
                .claim(LtiStrings.LTI_AZP, lti3Request.getIss())
                .claim(LtiStrings.LTI_DEPLOYMENT_ID, lti3Request.getLtiDeploymentId())
                .claim(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3)
                .claim(LtiStrings.LTI_DATA, lti3Request.deepLinkData)
                .claim(LtiStrings.LTI_CONTENT_ITEMS, multipleDeepLinkOnlyLti)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();

        List<String> jwt3bList = new ArrayList<>();
        jwt3bList.add(jwt3b);
        jwt3bList.add(listMapToJson(multipleDeepLinkOnlyLti));
        deepLinkJwtMap.put("Link 5 content: TWO Standard LTI Core Links (ltiResourceLinks)", jwt3bList);

        return deepLinkJwtMap;

    }

    static List<Map<String, Object>> createOneDeepLink(String localUrl) {
        List<Map<String, Object>> deepLinks = new ArrayList<>();
        Map<String, Object> deepLink = new HashMap<>();

        deepLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        deepLink.put(LtiStrings.DEEP_LINK_TITLE, "My test link");
        deepLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=1234");
        deepLinks.add(deepLink);
        return deepLinks;


    }

    static List<Map<String, Object>> createOneDeepLinkWithGrades(String localUrl) {
        List<Map<String, Object>> deepLinks = new ArrayList<>();
        Map<String, Object> deepLink = new HashMap<>();

        deepLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        deepLink.put(LtiStrings.DEEP_LINK_TITLE, "My test link");
        deepLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=1234");
        deepLink.put("lineItem", lineItem());
        Map<String, String> availableDates = new HashMap<>();
        Map<String, String> submissionDates = new HashMap<>();
        Map<String, String> custom = new HashMap<>();

        availableDates.put("startDateTime", "2018-03-07T20:00:03Z");
        availableDates.put("endDateTime", "2022-03-07T20:00:03Z");
        submissionDates.put("startDateTime", "2019-03-07T20:00:03Z");
        submissionDates.put("endDateTime", "2021-08-07T20:00:03Z");
        custom.put("dueDate", "$Resource.submission.endDateTime");
        custom.put("controlValue", "This is whatever I want to write here");
        deepLink.put("available", availableDates);
        deepLink.put("submission", submissionDates);
        deepLink.put("custom", custom);
        deepLinks.add(deepLink);
        return deepLinks;


    }

    static Map<String, Object> lineItem() {
        Map<String, Object> deepLink = new HashMap<>();

        deepLink.put("scoreMaximum", 87);
        deepLink.put("label", "LTI 1234 Quiz");
        deepLink.put("resourceId", "1234");
        deepLink.put("tag", "myquiztest");
        return deepLink;
    }


    static List<Map<String, Object>> createOneDeepLinkNoLti() {
        List<Map<String, Object>> deepLinks = new ArrayList<>();

        Map<String, Object> deepLink2b = new HashMap<>();
        deepLink2b.put(LtiStrings.DEEP_LINK_TYPE, "link");
        deepLink2b.put(LtiStrings.DEEP_LINK_URL, "https://www.youtube.com/watch?v=corV3-WsIro");

        deepLinks.add(deepLink2b);
        return deepLinks;


    }


    static List<Map<String, Object>> createMultipleDeepLink(String localUrl) {
        List<Map<String, Object>> deepLinks = createOneDeepLink(localUrl);

        Map<String, Object> deepLink2 = new HashMap<>();
        deepLink2.put(LtiStrings.DEEP_LINK_TYPE, "link");
        deepLink2.put(LtiStrings.DEEP_LINK_URL, "https://www.youtube.com/watch?v=corV3-WsIro");

        Map<String, Object> embed = new HashMap<>();
        embed.put("html", "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/corV3-WsIro\" frameborder=\"0\" allow=\"autoplay; encrypted-media\" allowfullscreen></iframe>");
        deepLink2.put("embed", embed);

        Map<String, Object> window = new HashMap<>();
        window.put("targetName", "youtube-corV3-WsIro");
        window.put("windowFeatures", "height=560,width=315,menubar=no");
        deepLink2.put("window", window);

        Map<String, Object> iframe = new HashMap<>();
        iframe.put("src", "https://www.youtube.com/embed/corV3-WsIro");
        iframe.put("width", 560);
        iframe.put("height", 315);
        deepLink2.put("iframe", iframe);
        deepLinks.add(deepLink2);

        Map<String, Object> ltiResourceLink = new HashMap<>();
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TITLE, "Another deep link");
        ltiResourceLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=4567");
        deepLinks.add(ltiResourceLink);


        Map<String, Object> deepLinkFilr = new HashMap<>();
        deepLinkFilr.put(LtiStrings.DEEP_LINK_TYPE, "file");
        deepLinkFilr.put(LtiStrings.DEEP_LINK_TITLE, "A file like a PDF that is my assignment submissions");
        deepLinkFilr.put(LtiStrings.DEEP_LINK_URL, "http://www.imsglobal.org/sites/default/files/ipr/imsipr_policyFinal.pdf");
        deepLinkFilr.put("mediaType", "application/pdf");
        deepLinks.add(deepLinkFilr);

        return deepLinks;
    }

    static List<Map<String, Object>> createMultipleDeepLinkOnlyLti(String localUrl) {
        List<Map<String, Object>> deepLinks = createOneDeepLink(localUrl);

        Map<String, Object> ltiResourceLink = new HashMap<>();
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TYPE, LtiStrings.DEEP_LINK_LTIRESOURCELINK);
        ltiResourceLink.put(LtiStrings.DEEP_LINK_TITLE, "Another deep link");
        ltiResourceLink.put(LtiStrings.DEEP_LINK_URL, localUrl + "/lti3?link=4567");
        deepLinks.add(ltiResourceLink);
        return deepLinks;
    }

    private static String listMapToJson(List<Map<String, Object>> listMap) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

}
