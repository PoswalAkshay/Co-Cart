package com.example.co_cart.models;

public class GroceryItem {
    private String itemId;
    private String householdId;
    private String name;
    private String quantity;
    private String category;
    private boolean purchased;
    private double estimatedCost;
    private boolean isUrgent;
    private String addedBy;
    private String addedByName;
    private long createdAt;

    public GroceryItem() {
        // Required empty constructor for Firestore
    }

    public GroceryItem(String itemId, String householdId, String name, String quantity, String category, boolean purchased, double estimatedCost, boolean isUrgent, String addedBy, String addedByName, long createdAt) {
        this.itemId = itemId;
        this.householdId = householdId;
        this.name = name;
        this.quantity = quantity;
        this.category = category != null && !category.isEmpty() ? category : "General 🛒";
        this.purchased = purchased;
        this.estimatedCost = estimatedCost;
        this.isUrgent = isUrgent;
        this.addedBy = addedBy;
        this.addedByName = addedByName;
        this.createdAt = createdAt;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category != null && !category.isEmpty() ? category : "General 🛒";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public boolean isUrgent() {
        return isUrgent;
    }

    public void setUrgent(boolean urgent) {
        isUrgent = urgent;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public String getAddedByName() {
        return addedByName;
    }

    public void setAddedByName(String addedByName) {
        this.addedByName = addedByName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
