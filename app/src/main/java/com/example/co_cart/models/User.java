package com.example.co_cart.models;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String countryCode;
    private String mobileNumber;
    private String householdId;
    private String age;
    private String gender;
    private String dob;
    private String profilePhotoUrl;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String email, String displayName, String householdId) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.householdId = householdId;
        this.firstName = "";
        this.middleName = "";
        this.lastName = "";
        this.countryCode = "+91";
        this.mobileNumber = "";
        this.age = "";
        this.gender = "";
        this.dob = "";
        this.profilePhotoUrl = "";
    }

    public User(String uid, String email, String displayName, String householdId, String age, String gender, String dob, String mobileNumber, String profilePhotoUrl) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.householdId = householdId;
        this.firstName = "";
        this.middleName = "";
        this.lastName = "";
        this.countryCode = "+91";
        this.mobileNumber = mobileNumber;
        this.age = age;
        this.gender = gender;
        this.dob = dob;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) sb.append(firstName);
        if (middleName != null && !middleName.isEmpty()) sb.append(" ").append(middleName);
        if (lastName != null && !lastName.isEmpty()) sb.append(" ").append(lastName);
        return sb.toString().trim();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFirstName() {
        return firstName != null ? firstName : "";
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName != null ? middleName : "";
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName != null ? lastName : "";
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCountryCode() {
        return countryCode != null && !countryCode.isEmpty() ? countryCode : "+91";
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
    }

    public String getAge() {
        return age != null ? age : "";
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender != null ? gender : "";
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob != null ? dob : "";
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getMobileNumber() {
        return mobileNumber != null ? mobileNumber : "";
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl != null ? profilePhotoUrl : "";
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
