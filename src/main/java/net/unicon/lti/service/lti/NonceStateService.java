package net.unicon.lti.service.lti;

import net.unicon.lti.model.lti.dto.NonceState;

import java.util.Date;

public interface NonceStateService {
    //Here we could add other checks like expiration of the state (not implemented)
    void deleteOldNonces();

    NonceState getNonce(String nonce);

    void deleteNonce(String nonce);
}
