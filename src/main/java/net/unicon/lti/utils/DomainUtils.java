package net.unicon.lti.utils;

import org.apache.commons.lang3.StringUtils;


public class DomainUtils {

    public static String insertDomain(String altDomain, String placement, String prefix) {

        StringBuffer buf = new StringBuffer(placement);
        int start = placement.indexOf("//") + 2;
        int end = placement.indexOf(".");
        buf.replace(start, end, prefix + altDomain);
        return buf.toString();
    }

    public static String extractDomain(String host){

        if (host.contains("lti-stag")){
            return null;
        }
        if (host.contains("//" + TextConstants.LTI_PREFIX)){
            host = StringUtils.substringAfter(host,"//" + TextConstants.LTI_PREFIX);
            host = StringUtils.substringBefore(host, ".");
            return host;
        } else if (host.contains("//" + TextConstants.HOME_PREFIX)){
            host = StringUtils.substringAfter(host,"//" + TextConstants.HOME_PREFIX);
            host = StringUtils.substringBefore(host, ".");
            return host;
        } else {
            return null;
        }
    }
}
