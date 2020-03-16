/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.utils.lti;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.unicon.lti13demo.model.PlatformDeployment;
import net.unicon.lti13demo.model.RSAKeyEntity;
import net.unicon.lti13demo.model.RSAKeyId;
import net.unicon.lti13demo.service.LTIDataService;
import net.unicon.lti13demo.utils.LtiStrings;
import net.unicon.lti13demo.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeepLinkUtils {

    /**
     *
     * @param lti3Request

     * @return
     */
    public static Map<String,String> generateDeepLinkJWT(LTIDataService ltiDataService, PlatformDeployment platformDeployment, LTI3Request lti3Request, String localUrl) throws GeneralSecurityException, IOException {

        Map deepLinkJwtMap = new HashMap<>();
        Date date = new Date();
        Optional<RSAKeyEntity> rsaKeyEntityOptional = ltiDataService.getRepos().rsaKeys.findById(new RSAKeyId(platformDeployment.getToolKid(),true));
        if (rsaKeyEntityOptional.isPresent()) {
            Key toolPrivateKey = OAuthUtils.loadPrivateKey(rsaKeyEntityOptional.get().getPrivateKey());

        // JWT 1:  Empty list of JSON
            String jwt1 = Jwts.builder()
                    .setHeaderParam("typ","JWT")
                    .setHeaderParam("kid", "000000000000000001")
                    .setHeaderParam("alg", "RS256")
                    .setIssuer(platformDeployment.getClientId())  //Client ID
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items",new HashMap<String,Object>())
                    .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                    .compact();

            deepLinkJwtMap.put("jwt1",jwt1);

        //JWT 2: One ltiResourcelink


            List<Map<String,Object>> oneDeepLink = createOneDeepLink(localUrl);
            String jwt2 = Jwts.builder()
                    .setHeaderParam("typ","JWT")
                    .setHeaderParam("kid", "000000000000000001")
                    .setHeaderParam("alg", "RS256")
                    .setIssuer(platformDeployment.getClientId())  //Client ID
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items", oneDeepLink)
                    .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                    .compact();

            deepLinkJwtMap.put("jwt2",jwt2);
            deepLinkJwtMap.put("jwt2Map",listMapToJson(oneDeepLink));

            //JWT 2b: One link (not ltiResourcelink)

            List<Map<String,Object>> oneDeepLinkNoLti = createOneDeepLinkNoLti(localUrl);
            String jwt2b = Jwts.builder()
                    .setHeaderParam("typ","JWT")
                    .setHeaderParam("kid", "000000000000000001")
                    .setHeaderParam("alg", "RS256")
                    .setIssuer(platformDeployment.getClientId())  //Client ID
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items", oneDeepLinkNoLti)
                    .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                    .compact();

            deepLinkJwtMap.put("jwt2b",jwt2b);
            deepLinkJwtMap.put("jwt2bMap",listMapToJson(oneDeepLinkNoLti));

        //JWT 3: More than one link
            List<Map<String,Object>> multipleDeepLink = createMultipleDeepLink(localUrl);
            String jwt3 = Jwts.builder()
                    .setHeaderParam("typ","JWT")
                    .setHeaderParam("kid", "000000000000000001")
                    .setHeaderParam("alg", "RS256")
                    .setIssuer(platformDeployment.getClientId())  //This is our own identifier, to know that we are the issuer.
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items",multipleDeepLink)
                    .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                    .compact();


            deepLinkJwtMap.put("jwt3",jwt3);
            deepLinkJwtMap.put("jwt3Map",listMapToJson(multipleDeepLink));


            //JWT 3b: More than one link but only ltiresourceLinks
            List<Map<String,Object>> multipleDeepLinkOnlyLti = createMultipleDeepLinkOnlyLti(localUrl);
            String jwt3b = Jwts.builder()
                    .setHeaderParam("typ","JWT")
                    .setHeaderParam("kid", "000000000000000001")
                    .setHeaderParam("alg", "RS256")
                    .setIssuer(platformDeployment.getClientId())  //This is our own identifier, to know that we are the issuer.
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items",multipleDeepLinkOnlyLti)
                    .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                    .compact();


            deepLinkJwtMap.put("jwt3b",jwt3b);
            deepLinkJwtMap.put("jwt3bMap",listMapToJson(multipleDeepLinkOnlyLti));


            return deepLinkJwtMap;
        } else {
            throw new GeneralSecurityException("Error generating the deep link responsed.");
        }
    }

    static List<Map<String,Object>> createOneDeepLink(String localUrl) {
        List<Map<String,Object>> deepLinks = new ArrayList<>();
        Map<String,Object> deepLink = new HashMap<>();

        deepLink.put("type","ltiResourceLink");
        deepLink.put("title","My test link");
        deepLink.put("url",localUrl + "/lti3?link=1234");

        deepLinks.add(deepLink);
        return deepLinks;


    }

    static List<Map<String,Object>> createOneDeepLinkNoLti(String localUrl) {
        List<Map<String,Object>> deepLinks = new ArrayList<>();

        Map<String,Object> deepLink2b = new HashMap<>();
        deepLink2b.put("type","link");
        deepLink2b.put("url","https://www.youtube.com/watch?v=corV3-WsIro");

        deepLinks.add(deepLink2b);
        return deepLinks;


    }


    static List<Map<String,Object>> createMultipleDeepLink(String localUrl) {
        List<Map<String,Object>> deepLinks = createOneDeepLink(localUrl);

        Map<String,Object> deepLink2 = new HashMap<>();
        deepLink2.put("type","link");
        deepLink2.put("url","https://www.youtube.com/watch?v=corV3-WsIro");

        Map<String,Object> embed = new HashMap<>();
        embed.put("html","<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/corV3-WsIro\" frameborder=\"0\" allow=\"autoplay; encrypted-media\" allowfullscreen></iframe>");
        deepLink2.put("embed",embed);

        Map<String,Object> window = new HashMap<>();
        window.put("targetName","youtube-corV3-WsIro");
        window.put("windowFeatures","height=560,width=315,menubar=no");
        deepLink2.put("window",window);

        Map<String,Object> iframe = new HashMap<>();
        iframe.put("src","https://www.youtube.com/embed/corV3-WsIro");
        iframe.put("width",new Integer("560"));
        iframe.put("height",new Integer("315"));
        deepLink2.put("iframe",iframe);
        deepLinks.add(deepLink2);

        Map<String,Object> ltiResourceLink = new HashMap<>();
        ltiResourceLink.put("type","ltiResourceLink");
        ltiResourceLink.put("title","Another deep link");
        ltiResourceLink.put("url",localUrl + "/lti3?link=4567");
        deepLinks.add(ltiResourceLink);


        Map<String,Object> deepLinkFilr = new HashMap<>();
        deepLinkFilr.put("type","file");
        deepLinkFilr.put("title","A file like a PDF that is my assignment submissions");
        deepLinkFilr.put("url","http://www.imsglobal.org/sites/default/files/ipr/imsipr_policyFinal.pdf");
        deepLinkFilr.put("mediaType","application/pdf");
        deepLinks.add(deepLinkFilr);

        return deepLinks;
    }

    static List<Map<String,Object>> createMultipleDeepLinkOnlyLti(String localUrl) {
        List<Map<String,Object>> deepLinks = createOneDeepLink(localUrl);

        Map<String,Object> ltiResourceLink = new HashMap<>();
        ltiResourceLink.put("type","ltiResourceLink");
        ltiResourceLink.put("title","Another deep link");
        ltiResourceLink.put("url",localUrl + "/lti3?link=4567");
        deepLinks.add(ltiResourceLink);
        return deepLinks;
    }

    private static String listMapToJson(List<Map<String,Object>> listMap){

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listMap);
            return json;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

}
