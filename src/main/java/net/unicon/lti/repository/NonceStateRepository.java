package net.unicon.lti.repository;

import net.unicon.lti.model.lti.dto.NonceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional
public interface NonceStateRepository extends JpaRepository<NonceState, String> {

    NonceState findByNonce(String nonce);
    NonceState findByStateHash(String stateHash);

    Boolean existsByNonce(String nonce);


    @Transactional
    void deleteByNonce(String nonce);

    @Transactional
    void deleteByCreatedAtBefore(Date expiryDate);


}
