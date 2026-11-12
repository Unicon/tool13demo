package net.unicon.lti.service.lti.impl;

import lombok.RequiredArgsConstructor;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.service.lti.NonceStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
@RequiredArgsConstructor
public class NonceStateServiceImpl implements NonceStateService {

    static final Logger log = LoggerFactory.getLogger(NonceStateServiceImpl.class);

    private final NonceStateRepository nonceStateRepository;

    //TODO, add more methods here and remove code from the controllers or services that call the repo directly.

    @Override
    public void deleteOldNonces() {
        Date thresholdDate = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
        nonceStateRepository.deleteByCreatedAtBefore(thresholdDate);
    }

    @Override
    public NonceState getNonce(String nonce) {
            return nonceStateRepository.findByNonce(nonce);
    }

    @Override
    public void deleteNonce(String nonce) {
        nonceStateRepository.deleteByNonce(nonce);
    }
}
