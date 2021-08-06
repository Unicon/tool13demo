package net.unicon.lti.model.oauth2;

import java.sql.Timestamp;
import java.util.List;

public class SecuredInfo {

    long platformDeploymentId;
    long contextId;
    String userId;
    List<String> roles;
    String canvasUserId;
    String canvasUserGlobalId;
    String canvasLoginId;
    String canvasUserName;
    String canvasCourseId;
    String canvasAssignmentId;
    Timestamp dueAt;
    Timestamp lockAt;
    Timestamp unlockAt;
    String nonce;

    public SecuredInfo() {
    }

    public long getPlatformDeploymentId() {
        return platformDeploymentId;
    }

    public void setPlatformDeploymentId(long platformDeploymentId) {
        this.platformDeploymentId = platformDeploymentId;
    }

    public long getContextId() {
        return contextId;
    }

    public void setContextId(long contextId) {
        this.contextId = contextId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getCanvasUserId() {
        return canvasUserId;
    }

    public void setCanvasUserId(String canvasUserId) {
        this.canvasUserId = canvasUserId;
    }

    public String getCanvasUserGlobalId() {
        return canvasUserGlobalId;
    }

    public void setCanvasUserGlobalId(String canvasUserGlobalId) {
        this.canvasUserGlobalId = canvasUserGlobalId;
    }

    public String getCanvasLoginId() {
        return canvasLoginId;
    }

    public void setCanvasLoginId(String canvasLoginId) {
        this.canvasLoginId = canvasLoginId;
    }

    public String getCanvasUserName() {
        return canvasUserName;
    }

    public void setCanvasUserName(String canvasUserName) {
        this.canvasUserName = canvasUserName;
    }

    public String getCanvasCourseId() {
        return canvasCourseId;
    }

    public void setCanvasCourseId(String canvasCourseId) {
        this.canvasCourseId = canvasCourseId;
    }

    public String getCanvasAssignmentId() {
        return canvasAssignmentId;
    }

    public void setCanvasAssignmentId(String canvasAssignmentId) {
        this.canvasAssignmentId = canvasAssignmentId;
    }

    public Timestamp getDueAt() {
        return dueAt;
    }

    public void setDueAt(Timestamp dueAt) {
        this.dueAt = dueAt;
    }

    public Timestamp getLockAt() {
        return lockAt;
    }

    public void setLockAt(Timestamp lockAt) {
        this.lockAt = lockAt;
    }

    public Timestamp getUnlockAt() {
        return unlockAt;
    }

    public void setUnlockAt(Timestamp unlockAt) {
        this.unlockAt = unlockAt;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

}
