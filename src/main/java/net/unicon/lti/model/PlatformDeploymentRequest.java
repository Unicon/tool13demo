package net.unicon.lti.model;

/**
 * The fields an admin can set when creating or updating a platform deployment, so a request body can't set the
 * key id or the linked contexts.
 */
public record PlatformDeploymentRequest(String iss, String clientId, String oidcEndpoint, String jwksEndpoint,
                                        String oAuth2TokenUrl, String oAuth2TokenAud, String deploymentId) {

    public PlatformDeployment toEntity() {
        PlatformDeployment platformDeployment = new PlatformDeployment();
        applyTo(platformDeployment);
        platformDeployment.setoAuth2TokenAud(oAuth2TokenAud);
        return platformDeployment;
    }

    /**
     * Copies the fields an update changes; the token audience is only set when the deployment is created.
     */
    public void applyTo(PlatformDeployment platformDeployment) {
        platformDeployment.setIss(iss);
        platformDeployment.setClientId(clientId);
        platformDeployment.setOidcEndpoint(oidcEndpoint);
        platformDeployment.setJwksEndpoint(jwksEndpoint);
        platformDeployment.setoAuth2TokenUrl(oAuth2TokenUrl);
        platformDeployment.setDeploymentId(deploymentId);
    }

}
