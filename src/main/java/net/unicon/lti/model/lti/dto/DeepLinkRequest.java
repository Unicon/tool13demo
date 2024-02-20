package net.unicon.lti.model.lti.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
public class DeepLinkRequest {

    @Getter
    @Setter
    private List<String> selectedIds;
    @Getter
    @Setter
    private String token;
    @Getter
    @Setter
    private String id_token;
    @Getter
    @Setter
    private String state_hash;
    @Getter
    @Setter
    private String nonce;



    public DeepLinkRequest(){ //Empty on purpose
    }

}
