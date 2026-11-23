package net.unicon.lti.controller.lti;

import lombok.RequiredArgsConstructor;
import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.model.lti.dto.DeepLinkJWTDTO;
import net.unicon.lti.model.lti.dto.DeepLinkRequest;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.service.lti.DeepLinkService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.service.lti.NonceStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;


/**
 * This Deep Link controller will return the signed JWT with the deeplinks selected in
 * the deep link dialog.
 */
@Controller
@RequestMapping("/deeplink")
@RequiredArgsConstructor
public class DeepLinkController {

    static final Logger log = LoggerFactory.getLogger(DeepLinkController.class);

    private final LTIJWTService ltijwtService;

    private final DeepLinkService deepLinkService;

    private final NonceStateService nonceStateService;


    @PostMapping({"/toJwt"})
    public ResponseEntity<Object> deepLinksToJwt(@RequestBody DeepLinkRequest deeplinksRequested) throws ConnectionException, GeneralSecurityException, IOException {
        NonceState nonceState = nonceStateService.getNonce(deeplinksRequested.getNonce());
        ResponseEntity<Object> responseEntity = checkAccess(deeplinksRequested, nonceState);

        if (responseEntity != null) {
            return responseEntity;
        }
        Jws<Claims> stateClaims = ltijwtService.validateState(nonceState.getState());
        Jws<Claims> idToken = ltijwtService.validateJWT(deeplinksRequested.getId_token(), stateClaims.getBody().get("clientId", String.class));
        if (deeplinksRequested.getSelectedIds().isEmpty()){
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not items selected");
            errorResponse.put("message", "Empty list of items");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        log.info(String.join(",", deeplinksRequested.getSelectedIds()));
        DeepLinkJWTDTO JSONdeeplink = deepLinkService.generateDeepLinkJWT(deeplinksRequested.getSelectedIds(), idToken);

        return ResponseEntity.ok(JSONdeeplink);
    }

    @PostMapping({"/deleteNonce"})
    public ResponseEntity<Object> deleteNonce(@RequestBody DeepLinkRequest deeplinksRequested) throws ConnectionException, GeneralSecurityException, IOException {
        NonceState nonceState = nonceStateService.getNonce(deeplinksRequested.getNonce());
        ResponseEntity<Object> responseEntity = checkAccess(deeplinksRequested, nonceState);
        if (responseEntity != null) {
            return responseEntity;
        }

        try {
            nonceStateService.deleteNonce(deeplinksRequested.getNonce());
        }catch (Exception e){
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error deleting nonce");
            errorResponse.put("message", "Error deleting nonce");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        return ResponseEntity.ok("Deleted");
    }

    private ResponseEntity<Object> checkAccess(DeepLinkRequest deeplinksRequested, NonceState nonceState) throws ConnectionException, GeneralSecurityException, IOException {
        // Validate state and token again...
        if (nonceState == null) {
            return createErrorResponse("Nonce state not found", "Invalid nonce");
        }

        if (!nonceState.getStateHash().equals(deeplinksRequested.getState_hash())){
            return createErrorResponse("State does not match", "Invalid State");
        }

        String tohash = deeplinksRequested.getId_token() + deeplinksRequested.getState_hash() + deeplinksRequested.getNonce();
        String expected_hash = Hashing.sha256()
                .hashString(tohash, StandardCharsets.UTF_8)
                .toString();
        Jws<Claims> claims_token = ltijwtService.validateNonceState(deeplinksRequested.getToken());
        if (!expected_hash.equals(claims_token.getBody().get("expected_hash"))){
            return createErrorResponse("Token not valid", "Invalid Token");
        }
        return null; // Access is granted
    }

    private ResponseEntity<Object> createErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
