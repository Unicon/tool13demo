package net.unicon.lti.repository;

import net.unicon.lti.model.ApiOneUseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional
public interface ApiOneUseTokenRepository extends JpaRepository<ApiOneUseToken, String> {

    ApiOneUseToken findByToken(String token);
    //TODO, decide when we want to delete those.
    @Transactional
    void deleteByCreatedAtBefore(Date expiryDate);
}
