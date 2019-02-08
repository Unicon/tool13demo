package net.unicon.lti13demo.utils.lti;

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
            Key issPrivateKey = OAuthUtils.loadPrivateKey(rsaKeyEntityOptional.get().getPrivateKey());

        // JWT 1:  Empty list of JSON
            String jwt1 = Jwts.builder()
                    .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items",new HashMap<String,Object>())
                    .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                    .compact();

            deepLinkJwtMap.put("jwt1",jwt1);
        //JWT 2: One link



            String jwt2 = Jwts.builder()
                    .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items", createOneDeepLink())
                    .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                    .compact();

            deepLinkJwtMap.put("jwt2",jwt2);

        //JWT 3: More than one link

            String jwt3 = Jwts.builder()
                    .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                    .setAudience(lti3Request.getIss())
                    .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                    .setIssuedAt(date) // for example, now
                    .claim("nonce",lti3Request.getNonce())
                    .claim("azp",lti3Request.getIss())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/deployment_id",lti3Request.getLtiDeploymentId())
                    .claim("https://purl.imsglobal.org/spec/lti/claim/message_type", LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                    .claim("https://purl.imsglobal.org/spec/lti/claim/version",LtiStrings.LTI_VERSION_3)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/data",lti3Request.deepLinkData)
                    .claim("https://purl.imsglobal.org/spec/lti-dl/claim/content_items",createMultipleDeepLink())
                    .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                    .compact();


            deepLinkJwtMap.put("jwt3",jwt3);

            return deepLinkJwtMap;
        } else {
            throw new GeneralSecurityException("Error generating the deep link responsed.");
        }
    }

    static Map<String,Object> createOneDeepLink() {
        Map<String,Object> deepLink = new HashMap<>();

        deepLink.put("type","link");
        deepLink.put("title","My Home Page");
        deepLink.put("url","https://something.example.com/page.html");

        Map<String,Object> icon = new HashMap<>();
        icon.put("url","link");
        icon.put("width",new Integer("100"));
        icon.put("height",new Integer("100"));
        deepLink.put("icon",icon);

        Map<String,Object> thumbnail = new HashMap<>();
        icon.put("url","link");
        icon.put("width",new Integer("90"));
        icon.put("height",new Integer("90"));


        deepLink.put("thumbnail",thumbnail);

        return deepLink;


    }


    static List<Map<String,Object>> createMultipleDeepLink() {
        List<Map<String,Object>> deepLinks = new ArrayList<>();

        deepLinks.add(createOneDeepLink());


        Map<String,Object> deepLinkHtml = new HashMap<>();
        deepLinkHtml.put("type","html");
        deepLinkHtml.put("html","<h1>A Custom Title</h1>");
        deepLinks.add(deepLinkHtml);


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

        Map<String,Object> deepLinkImage = new HashMap<>();
        deepLinkHtml.put("type","image");

        Map<String,Object> resourceMetadata = new HashMap<>();
        resourceMetadata.put("url","https://www.example.com/image.png");
        deepLinkImage.put("https://www.example.com/resourceMetadata",resourceMetadata);
        deepLinks.add(deepLinkImage);


        Map<String,Object> ltiResourceLink = new HashMap<>();

        ltiResourceLink.put("type","ltiResourceLink");
        ltiResourceLink.put("title","A title");
        ltiResourceLink.put("url","https://lti.example.com/launchMe");

        Map<String,Object> presentation = new HashMap<>();
        presentation.put("documentTarget","iframe");
        presentation.put("width",new Integer("500"));
        presentation.put("height",new Integer("600"));
        ltiResourceLink.put("presentation",presentation);

        Map<String,Object> icon2 = new HashMap<>();
        icon2.put("url","https://lti.example.com/image.jpg");
        icon2.put("width",new Integer("100"));
        icon2.put("height",new Integer("100"));
        ltiResourceLink.put("icon",icon2);

        Map<String,Object> thumbnail2 = new HashMap<>();
        thumbnail2.put("url","https://lti.example.com/thumb.jpg");
        thumbnail2.put("width",new Integer("90"));
        thumbnail2.put("height",new Integer("90"));
        ltiResourceLink.put("thumbnail",thumbnail2);

        Map<String,Object> lineitem = new HashMap<>();
        lineitem.put("label","Chapter 12 quiz");
        lineitem.put("scoreMaximum",new Long("87"));
        lineitem.put("resourceId","xyzpdq1234");
        lineitem.put("tag","originality");
        ltiResourceLink.put("lineitem",lineitem);

        Map<String,Object> custom = new HashMap<>();
        custom.put("quiz_id","az-123");
        custom.put("duedate","$Resource.submission.endDateTime");
        ltiResourceLink.put("custom",custom);

        Map<String,Object> window2 = new HashMap<>();
        window2.put("targetName","examplePublisherContent");
        ltiResourceLink.put("window",window2);

        Map<String,Object> iframe2 = new HashMap<>();
        iframe2.put("width",new Integer("890"));
        ltiResourceLink.put("iframe",iframe2);

        deepLinks.add(ltiResourceLink);


        Map<String,Object> deepLinkFilr = new HashMap<>();
        deepLinkFilr.put("type","file");
        deepLinkFilr.put("title","A file like a PDF that is my assignment submissions");
        deepLinkFilr.put("url","https://my.example.com/assignment1.pdf");
        deepLinkFilr.put("mediaType","application/pdf");
        deepLinkFilr.put("expiresAt","2018-03-06T20:05:02Z");
        deepLinks.add(deepLinkFilr);

        Map<String,Object> deepLinkCustom = new HashMap<>();
        ltiResourceLink.put("type","https://www.example.com/custom_type");
        ltiResourceLink.put("data","somedata");
        deepLinks.add(deepLinkCustom);


        return deepLinks;
    }


}
