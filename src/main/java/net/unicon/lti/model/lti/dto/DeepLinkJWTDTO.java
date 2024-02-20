package net.unicon.lti.model.lti.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeepLinkJWTDTO {


    @Getter
    @Setter
    private List<Map<String,Object>> JSONString;
    @Getter
    @Setter
    private String JWTString;

    public DeepLinkJWTDTO() {//Empty on purpose
    }

    public DeepLinkJWTDTO(List<Map<String,Object>> JSONString, String JWTString) {
        this.JSONString = JSONString;
        this.JWTString = JWTString;
    }


}
