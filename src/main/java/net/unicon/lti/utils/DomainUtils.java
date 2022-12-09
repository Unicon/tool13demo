package net.unicon.lti.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;


public class DomainUtils {

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
