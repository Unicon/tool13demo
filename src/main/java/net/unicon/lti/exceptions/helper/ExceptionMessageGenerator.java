package net.unicon.lti.exceptions.helper;

import org.springframework.stereotype.Component;

@Component
public class ExceptionMessageGenerator {

    public String exceptionMessage(String customMessage, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(customMessage);
        sb.append("\n");
        if (e != null) {
            sb.append("Exception : ");
            sb.append(e.getMessage());
            if (e.getCause() != null) {
                sb.append("\n");
                sb.append("Cause :");
                sb.append(e.getCause().getMessage());
            }
        }
        return sb.toString();
    }
}
