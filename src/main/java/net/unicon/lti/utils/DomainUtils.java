package net.unicon.lti.utils;

import org.apache.commons.lang3.StringUtils;


public class DomainUtils {

    public static String insertWildcardDomain(String altDomain, String placement){
        String after = StringUtils.substringAfter(placement, "://");
        String before = StringUtils.substringBefore(placement, "://");
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(before)){
            sb.append(before).append("://");
        }
        sb.append(altDomain).append(".").append(after);
        return sb.toString();
    }

    public static boolean isWildcardDomain(String received, String host){
        String afterReceived = StringUtils.substringAfter(received, "://");
        String afterHost = StringUtils.substringAfter(host, "://");
        if (StringUtils.contains(afterReceived,afterHost)) {
            if (StringUtils.indexOf(afterReceived, afterHost) == 0){
                return false;
            }
            return true;
        }
        return false;
    }

    public static String extractWildcardDomain(String host){
        return StringUtils.substringBetween(host, "://", ".");
    }


    public static String insertDomain(String altDomain, String placement) {

        StringBuffer buf = new StringBuffer(placement);
        int dot = placement.indexOf(".");
        buf.insert(dot,  "-" + altDomain);
        return buf.toString();
    }

    public static String extractDomain(String host){

        StringBuffer buf = new StringBuffer(host);
        int dot = host.indexOf(".");
        String toFindMinus = buf.substring(0, dot);
        int start = toFindMinus.lastIndexOf("-");
        if (start == -1){
            return null;
        }
        String prefix = buf.substring(start+1, dot);

        if ((TextConstants.FILTERED_DOMAINS.contains(prefix))){
            return null;
        } else {
            return prefix;
        }
    }
}
