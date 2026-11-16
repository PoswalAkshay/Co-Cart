package com.example.co_cart.models;

public class Settlement {
    private String settlementId;
    private String householdId;
    private String paidByUserId;
    private String paidByName;
    private double totalAmount;
    private int clearedItemCount;
    private long createdAt;

    public Settlement() {
        // Required empty constructor for Firestore
    }

    public Settlement(String settlementId, String householdId, String paidByUserId, String paidByName, double totalAmount, int clearedItemCount, long createdAt) {
        this.settlementId = settlementId;
        this.householdId = householdId;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.totalAmount = totalAmount;
        this.clearedItemCount = clearedItemCount;
        this.createdAt = createdAt;
    }

    public String getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(String settlementId) {
        this.settlementId = settlementId;
    }

    public String getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
    }

    public String getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(String paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getClearedItemCount() {
        return clearedItemCount;
    }

    public void setClearedItemCount(int clearedItemCount) {
        this.clearedItemCount = clearedItemCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
