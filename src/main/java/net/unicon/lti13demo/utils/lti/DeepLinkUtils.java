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
    public static Map<String,String> generateDeepLinkJWT(LTIDataService ltiDataService, PlatformDeployment platformDeployment, LTI3Request lti3Request) throws GeneralSecurityException, IOException {

        Map deepLinkJwtMap = new HashMap<>();
        Date date = new Date();
        Optional<RSAKeyEntity> rsaKeyEntityOptional = ltiDataService.getRepos().rsaKeys.findById(new RSAKeyId(platformDeployment.getToolKid(),true));
        if (rsaKeyEntityOptional.isPresent()) {
            Key toolPrivateKey = OAuthUtils.loadPrivateKey(rsaKeyEntityOptional.get().getPrivateKey());

        // JWT 1:  Empty list of JSON
            String jwt1 = Jwts.builder()
                    .setHeaderParam("typ","JWT")
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

        //JWT 2: One link


            List<Map<String,Object>> oneDeepLink = createOneDeepLink();
            String jwt2 = Jwts.builder()
                    .setHeaderParam("typ","JWT")
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

        //JWT 3: More than one link
            List<Map<String,Object>> multipleDeepLink = createMultipleDeepLink();
            String jwt3 = Jwts.builder()
                    .setHeaderParam("typ","JWT")
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


            return deepLinkJwtMap;
        } else {
            throw new GeneralSecurityException("Error generating the deep link responsed.");
        }
    }

    static List<Map<String,Object>> createOneDeepLink() {
        List<Map<String,Object>> deepLinks = new ArrayList<>();
        Map<String,Object> deepLink = new HashMap<>();

        deepLink.put("type","ltiResourceLink");
        deepLink.put("title","My test link");
        deepLink.put("url","https://localhost:9090/lti3?link=1234");

        deepLinks.add(deepLink);
        return deepLinks;


    }


    static List<Map<String,Object>> createMultipleDeepLink() {
        List<Map<String,Object>> deepLinks = createOneDeepLink();

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
        ltiResourceLink.put("url","https://localhost:9090/lti3?link=4567");
        deepLinks.add(ltiResourceLink);


        Map<String,Object> deepLinkFilr = new HashMap<>();
        deepLinkFilr.put("type","file");
        deepLinkFilr.put("title","A file like a PDF that is my assignment submissions");
        deepLinkFilr.put("url","https://my.example.com/assignment1.pdf");
        deepLinkFilr.put("mediaType","application/pdf");
        deepLinkFilr.put("expiresAt","2018-03-06T20:05:02Z");
        deepLinks.add(deepLinkFilr);

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
