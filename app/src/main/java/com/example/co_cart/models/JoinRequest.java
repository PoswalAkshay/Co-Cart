package com.example.co_cart.models;

public class JoinRequest {
    private String requestId;
    private String householdId;
    private String userId;
    private String userName;
    private String userEmail;
    private String status; // "PENDING", "ACCEPTED", "REJECTED"
    private long createdAt;

    public JoinRequest() {
        // Empty constructor for Firestore
    }

    public JoinRequest(String requestId, String householdId, String userId, String userName, String userEmail, String status, long createdAt) {
        this.requestId = requestId;
        this.householdId = householdId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
