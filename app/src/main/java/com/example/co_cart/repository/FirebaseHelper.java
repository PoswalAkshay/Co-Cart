package com.example.co_cart.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.co_cart.models.GroceryItem;
import com.example.co_cart.models.Household;
import com.example.co_cart.models.JoinRequest;
import com.example.co_cart.models.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseHelper {
    private static FirebaseHelper instance;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isFirebaseAvailable = false;

    // Local fallback store for offline/demo operation when Firebase config is pending
    private SharedPreferences prefs;
    private static final String PREF_NAME = "cocart_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_HOUSEHOLD_ID = "household_id";
    private static final String KEY_HOUSEHOLD_NAME = "household_name";
    private static final String KEY_HOUSEHOLD_CODE = "household_code";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public interface GroceryItemsListener {
        void onItemsUpdated(List<GroceryItem> items);
        void onError(String errorMessage);
    }

    public interface JoinRequestsListener {
        void onRequestsUpdated(List<JoinRequest> requests);
        void onError(String errorMessage);
    }

    public interface JoinRequestStatusListener {
        void onAccepted(String householdId);
        void onRejected();
        void onError(String errorMessage);
    }

    private FirebaseHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            if (!FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) {
                auth = FirebaseAuth.getInstance();
                db = FirebaseFirestore.getInstance();
                isFirebaseAvailable = true;
            } else {
                FirebaseApp.initializeApp(context.getApplicationContext());
                auth = FirebaseAuth.getInstance();
                db = FirebaseFirestore.getInstance();
                isFirebaseAvailable = true;
            }
        } catch (Exception e) {
            isFirebaseAvailable = false;
        }
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context);
        }
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FirebaseHelper not initialized. Call init(context) first.");
        }
        return instance;
    }

    public boolean isFirebaseConfigured() {
        return isFirebaseAvailable;
    }

    // --- AUTHENTICATION ---

    public boolean isLoggedIn() {
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            return true;
        }
        return prefs.contains(KEY_USER_ID);
    }

    public String getCurrentUserId() {
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return prefs.getString(KEY_USER_ID, UUID.randomUUID().toString());
    }

    public void registerUser(String email, String password, String name, Callback<User> callback) {
        if (isFirebaseAvailable && auth != null) {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {
                            User user = new User(firebaseUser.getUid(), email, name, "");
                            saveLocalUser(user);
                            if (db != null) {
                                db.collection("users").document(firebaseUser.getUid())
                                        .set(user)
                                        .addOnCompleteListener(task -> callback.onSuccess(user));
                            } else {
                                callback.onSuccess(user);
                            }
                        } else {
                            callback.onError("Registration failed: empty user returned.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        String uid = UUID.randomUUID().toString();
                        User user = new User(uid, email, name, "");
                        saveLocalUser(user);
                        callback.onSuccess(user);
                    });
        } else {
            String uid = UUID.randomUUID().toString();
            User user = new User(uid, email, name, "");
            saveLocalUser(user);
            callback.onSuccess(user);
        }
    }

    public void loginUser(String email, String password, Callback<User> callback) {
        if (isFirebaseAvailable && auth != null) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {
                            getUserProfile(firebaseUser.getUid(), callback);
                        } else {
                            callback.onError("Login failed: empty user returned.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        String uid = prefs.getString(KEY_USER_ID, UUID.randomUUID().toString());
                        String name = prefs.getString(KEY_USER_NAME, "User");
                        String householdId = prefs.getString(KEY_HOUSEHOLD_ID, "");
                        User user = new User(uid, email, name, householdId);
                        saveLocalUser(user);
                        callback.onSuccess(user);
                    });
        } else {
            String uid = prefs.getString(KEY_USER_ID, UUID.randomUUID().toString());
            String name = prefs.getString(KEY_USER_NAME, "User");
            String householdId = prefs.getString(KEY_HOUSEHOLD_ID, "");
            User user = new User(uid, email, name, householdId);
            saveLocalUser(user);
            callback.onSuccess(user);
        }
    }

    public void getUserProfile(String uid, Callback<User> callback) {
        if (isFirebaseAvailable && db != null) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String localHId = prefs.getString(KEY_HOUSEHOLD_ID, "");
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                if ((user.getHouseholdId() == null || user.getHouseholdId().isEmpty()) && !localHId.isEmpty()) {
                                    user.setHouseholdId(localHId);
                                    updateUserHousehold(uid, localHId);
                                }
                                saveLocalUser(user);
                                callback.onSuccess(user);
                                return;
                            }
                        }
                        String email = prefs.getString(KEY_USER_EMAIL, "");
                        String name = prefs.getString(KEY_USER_NAME, "User");
                        String age = prefs.getString("user_age", "");
                        String gender = prefs.getString("user_gender", "");
                        String dob = prefs.getString("user_dob", "");
                        String mobile = prefs.getString("user_mobile", "");
                        String photo = prefs.getString("user_photo", "");
                        User user = new User(uid, email, name, localHId, age, gender, dob, mobile, photo);
                        callback.onSuccess(user);
                    })
                    .addOnFailureListener(e -> {
                        String email = prefs.getString(KEY_USER_EMAIL, "");
                        String name = prefs.getString(KEY_USER_NAME, "User");
                        String localHId = prefs.getString(KEY_HOUSEHOLD_ID, "");
                        String age = prefs.getString("user_age", "");
                        String gender = prefs.getString("user_gender", "");
                        String dob = prefs.getString("user_dob", "");
                        String mobile = prefs.getString("user_mobile", "");
                        String photo = prefs.getString("user_photo", "");
                        callback.onSuccess(new User(uid, email, name, localHId, age, gender, dob, mobile, photo));
                    });
        } else {
            String email = prefs.getString(KEY_USER_EMAIL, "");
            String name = prefs.getString(KEY_USER_NAME, "User");
            String localHId = prefs.getString(KEY_HOUSEHOLD_ID, "");
            String age = prefs.getString("user_age", "");
            String gender = prefs.getString("user_gender", "");
            String dob = prefs.getString("user_dob", "");
            String mobile = prefs.getString("user_mobile", "");
            String photo = prefs.getString("user_photo", "");
            callback.onSuccess(new User(uid, email, name, localHId, age, gender, dob, mobile, photo));
        }
    }

    public void updateUserProfile(User user, Callback<Boolean> callback) {
        saveLocalUser(user);
        if (isFirebaseAvailable && db != null && user.getUid() != null && !user.getUid().isEmpty()) {
            db.collection("users").document(user.getUid())
                    .set(user, com.google.firebase.firestore.SetOptions.merge())
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    public void logout() {
        if (isFirebaseAvailable && auth != null) {
            auth.signOut();
        }
        prefs.edit().clear().apply();
    }

    private void saveLocalUser(User user) {
        prefs.edit()
                .putString(KEY_USER_ID, user.getUid())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_NAME, user.getDisplayName())
                .putString(KEY_HOUSEHOLD_ID, user.getHouseholdId())
                .putString("user_age", user.getAge())
                .putString("user_gender", user.getGender())
                .putString("user_dob", user.getDob())
                .putString("user_mobile", user.getMobileNumber())
                .putString("user_photo", user.getProfilePhotoUrl())
                .apply();
    }

    // --- HOUSEHOLD MANAGEMENT (CLOUDFIRESTORE DIRECT) ---

    public void createHousehold(String name, Callback<Household> callback) {
        String inviteCode = generateInviteCode();
        String householdId = "household_" + inviteCode;
        String currentUserId = getCurrentUserId();
        long createdAt = System.currentTimeMillis();

        Household household = new Household(householdId, name.trim(), inviteCode, currentUserId, createdAt);
        saveLocalHousehold(household);
        prefs.edit().putString(KEY_HOUSEHOLD_ID, householdId).apply();

        if (isFirebaseAvailable && db != null) {
            db.collection("households").document(householdId)
                    .set(household)
                    .addOnCompleteListener(task -> {
                        updateUserHousehold(currentUserId, householdId);
                        callback.onSuccess(household);
                    });
        } else {
            callback.onSuccess(household);
        }
    }

    public void joinHousehold(String inviteCode, Callback<Household> callback) {
        String cleanCode = inviteCode.trim().toUpperCase();
        String householdId = "household_" + cleanCode;
        String currentUserId = getCurrentUserId();

        Household household = new Household(householdId, "Shared Household (" + cleanCode + ")", cleanCode, currentUserId, System.currentTimeMillis());
        saveLocalHousehold(household);
        prefs.edit().putString(KEY_HOUSEHOLD_ID, householdId).apply();
        updateUserHousehold(currentUserId, householdId);

        if (isFirebaseAvailable && db != null) {
            db.collection("households").document(householdId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Household h = doc.toObject(Household.class);
                            if (h != null) {
                                saveLocalHousehold(h);
                                callback.onSuccess(h);
                                return;
                            }
                        }
                        callback.onSuccess(household);
                    })
                    .addOnFailureListener(e -> callback.onSuccess(household));
        } else {
            callback.onSuccess(household);
        }
    }

    public void regenerateHouseholdCode(String householdId, Callback<String> callback) {
        String newInviteCode = generateInviteCode();
        prefs.edit().putString(KEY_HOUSEHOLD_CODE, newInviteCode).apply();

        if (isFirebaseAvailable && db != null && householdId != null && !householdId.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("inviteCode", newInviteCode);
            db.collection("households").document(householdId)
                    .update(updates)
                    .addOnCompleteListener(task -> callback.onSuccess(newInviteCode));
        } else {
            callback.onSuccess(newInviteCode);
        }
    }

    // --- HOST APPROVAL WORKFLOW FOR JOIN REQUESTS ---

    public void sendJoinRequest(String inviteCode, Callback<JoinRequest> callback) {
        String cleanCode = inviteCode.trim().toUpperCase();
        String householdId = "household_" + cleanCode;
        String currentUserId = getCurrentUserId();
        String currentUserName = prefs.getString(KEY_USER_NAME, "Roommate");
        String currentUserEmail = prefs.getString(KEY_USER_EMAIL, "");

        JoinRequest request = new JoinRequest(
                currentUserId,
                householdId,
                currentUserId,
                currentUserName,
                currentUserEmail,
                "PENDING",
                System.currentTimeMillis()
        );

        if (isFirebaseAvailable && db != null) {
            db.collection("households")
                    .document(householdId)
                    .collection("join_requests")
                    .document(currentUserId)
                    .set(request)
                    .addOnCompleteListener(task -> callback.onSuccess(request));
        } else {
            callback.onSuccess(request);
        }
    }

    public ListenerRegistration listenToPendingJoinRequests(String householdId, JoinRequestsListener listener) {
        if (isFirebaseAvailable && db != null && householdId != null && !householdId.isEmpty()) {
            return db.collection("households")
                    .document(householdId)
                    .collection("join_requests")
                    .whereEqualTo("status", "PENDING")
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null) {
                            listener.onRequestsUpdated(new ArrayList<>());
                            return;
                        }
                        List<JoinRequest> requests = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            JoinRequest req = doc.toObject(JoinRequest.class);
                            if (req != null) {
                                requests.add(req);
                            }
                        }
                        listener.onRequestsUpdated(requests);
                    });
        } else {
            listener.onRequestsUpdated(new ArrayList<>());
            return null;
        }
    }

    public void respondToJoinRequest(JoinRequest request, boolean accept, Callback<Boolean> callback) {
        String newStatus = accept ? "ACCEPTED" : "REJECTED";
        request.setStatus(newStatus);

        if (accept) {
            updateUserHousehold(request.getUserId(), request.getHouseholdId());
        }

        if (isFirebaseAvailable && db != null) {
            db.collection("households")
                    .document(request.getHouseholdId())
                    .collection("join_requests")
                    .document(request.getUserId())
                    .update("status", newStatus)
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    public ListenerRegistration listenToMyJoinRequestStatus(String inviteCode, JoinRequestStatusListener listener) {
        String cleanCode = inviteCode.trim().toUpperCase();
        String householdId = "household_" + cleanCode;
        String currentUserId = getCurrentUserId();

        if (isFirebaseAvailable && db != null) {
            return db.collection("households")
                    .document(householdId)
                    .collection("join_requests")
                    .document(currentUserId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null || !snapshot.exists()) {
                            return;
                        }
                        JoinRequest req = snapshot.toObject(JoinRequest.class);
                        if (req != null) {
                            if ("ACCEPTED".equalsIgnoreCase(req.getStatus())) {
                                Household household = new Household(householdId, "Shared Household (" + cleanCode + ")", cleanCode, currentUserId, System.currentTimeMillis());
                                saveLocalHousehold(household);
                                updateUserHousehold(currentUserId, householdId);
                                listener.onAccepted(householdId);
                            } else if ("REJECTED".equalsIgnoreCase(req.getStatus())) {
                                listener.onRejected();
                            }
                        }
                    });
        } else {
            Household household = new Household(householdId, "Shared Household (" + cleanCode + ")", cleanCode, currentUserId, System.currentTimeMillis());
            saveLocalHousehold(household);
            updateUserHousehold(currentUserId, householdId);
            listener.onAccepted(householdId);
            return null;
        }
    }

    public void getHouseholdDetails(String householdId, Callback<Household> callback) {
        if (isFirebaseAvailable && db != null) {
            db.collection("households").document(householdId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Household h = doc.toObject(Household.class);
                            if (h != null) {
                                saveLocalHousehold(h);
                                callback.onSuccess(h);
                                return;
                            }
                        }
                        String name = prefs.getString(KEY_HOUSEHOLD_NAME, "My Household");
                        String code = prefs.getString(KEY_HOUSEHOLD_CODE, "COCART");
                        callback.onSuccess(new Household(householdId, name, code, getCurrentUserId(), System.currentTimeMillis()));
                    })
                    .addOnFailureListener(e -> {
                        String name = prefs.getString(KEY_HOUSEHOLD_NAME, "My Household");
                        String code = prefs.getString(KEY_HOUSEHOLD_CODE, "COCART");
                        callback.onSuccess(new Household(householdId, name, code, getCurrentUserId(), System.currentTimeMillis()));
                    });
        } else {
            String name = prefs.getString(KEY_HOUSEHOLD_NAME, "My Household");
            String code = prefs.getString(KEY_HOUSEHOLD_CODE, "COCART");
            callback.onSuccess(new Household(householdId, name, code, getCurrentUserId(), System.currentTimeMillis()));
        }
    }

    public void leaveHousehold(Callback<Boolean> callback) {
        String userId = getCurrentUserId();
        clearLocalHousehold();
        if (isFirebaseAvailable && db != null && userId != null) {
            db.collection("users").document(userId)
                    .update("householdId", "")
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    private void updateUserHousehold(String userId, String householdId) {
        prefs.edit().putString(KEY_HOUSEHOLD_ID, householdId).apply();
        if (isFirebaseAvailable && db != null && userId != null && !userId.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("householdId", householdId);
            db.collection("users").document(userId)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge());
        }
    }

    private void saveLocalHousehold(Household household) {
        prefs.edit()
                .putString(KEY_HOUSEHOLD_ID, household.getHouseholdId())
                .putString(KEY_HOUSEHOLD_NAME, household.getName())
                .putString(KEY_HOUSEHOLD_CODE, household.getInviteCode())
                .apply();
    }

    private void clearLocalHousehold() {
        prefs.edit().remove(KEY_HOUSEHOLD_ID).remove(KEY_HOUSEHOLD_NAME).remove(KEY_HOUSEHOLD_CODE).apply();
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // --- GROCERY LIST MANAGEMENT (REAL-TIME CRUD + LOCAL CACHE) ---

    private List<GroceryItem> getLocalGroceryItems(String householdId) {
        List<GroceryItem> items = new ArrayList<>();
        String json = prefs.getString("items_" + householdId, "");
        if (!json.isEmpty()) {
            try {
                org.json.JSONArray array = new org.json.JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    org.json.JSONObject obj = array.getJSONObject(i);
                    GroceryItem item = new GroceryItem(
                            obj.optString("itemId"),
                            obj.optString("householdId"),
                            obj.optString("name"),
                            obj.optString("quantity"),
                            obj.optString("category", "General 🛒"),
                            obj.optBoolean("purchased"),
                            obj.optDouble("estimatedCost", 0.0),
                            obj.optBoolean("isUrgent", false),
                            obj.optString("addedBy"),
                            obj.optString("addedByName"),
                            obj.optLong("createdAt")
                    );
                    items.add(item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return items;
    }

    private void saveLocalGroceryItems(String householdId, List<GroceryItem> items) {
        try {
            org.json.JSONArray array = new org.json.JSONArray();
            for (GroceryItem item : items) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("itemId", item.getItemId());
                obj.put("householdId", item.getHouseholdId());
                obj.put("name", item.getName());
                obj.put("quantity", item.getQuantity());
                obj.put("category", item.getCategory());
                obj.put("purchased", item.isPurchased());
                obj.put("estimatedCost", item.getEstimatedCost());
                obj.put("isUrgent", item.isUrgent());
                obj.put("addedBy", item.getAddedBy());
                obj.put("addedByName", item.getAddedByName());
                obj.put("createdAt", item.getCreatedAt());
                array.put(obj);
            }
            prefs.edit().putString("items_" + householdId, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GroceryItemsListener activeGroceryListener;
    private String activeHouseholdId;

    public ListenerRegistration listenToGroceryItems(String householdId, GroceryItemsListener listener) {
        this.activeHouseholdId = householdId;
        this.activeGroceryListener = listener;

        List<GroceryItem> localItems = getLocalGroceryItems(householdId);
        listener.onItemsUpdated(localItems);

        if (isFirebaseAvailable && db != null) {
            return db.collection("households")
                    .document(householdId)
                    .collection("grocery_items")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            if (activeGroceryListener != null) {
                                activeGroceryListener.onItemsUpdated(getLocalGroceryItems(householdId));
                            }
                            return;
                        }
                        if (value != null) {
                            List<GroceryItem> items = new ArrayList<>();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                GroceryItem item = doc.toObject(GroceryItem.class);
                                if (item != null) {
                                    items.add(item);
                                }
                            }
                            java.util.Collections.sort(items, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                            saveLocalGroceryItems(householdId, items);
                            if (activeGroceryListener != null) {
                                activeGroceryListener.onItemsUpdated(items);
                            }
                        }
                    });
        } else {
            return null;
        }
    }

    private void notifyActiveListener(String householdId) {
        if (activeGroceryListener != null && householdId != null && householdId.equals(activeHouseholdId)) {
            activeGroceryListener.onItemsUpdated(getLocalGroceryItems(householdId));
        }
    }

    public void addGroceryItem(String householdId, String name, String quantity, String category, double estimatedCost, boolean isUrgent, Callback<Boolean> callback) {
        String itemId = UUID.randomUUID().toString();
        String currentUserId = getCurrentUserId();
        String currentUserName = prefs.getString(KEY_USER_NAME, "Roommate");
        long createdAt = System.currentTimeMillis();

        GroceryItem item = new GroceryItem(itemId, householdId, name, quantity, category, false, estimatedCost, isUrgent, currentUserId, currentUserName, createdAt);

        List<GroceryItem> items = getLocalGroceryItems(householdId);
        items.add(0, item);
        saveLocalGroceryItems(householdId, items);
        notifyActiveListener(householdId);

        if (isFirebaseAvailable && db != null) {
            db.collection("households")
                    .document(householdId)
                    .collection("grocery_items")
                    .document(itemId)
                    .set(item)
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    public void addGroceryItem(String householdId, String name, String quantity, String category, Callback<Boolean> callback) {
        addGroceryItem(householdId, name, quantity, category, 0.0, false, callback);
    }

    public void updateGroceryItem(String householdId, String itemId, String name, String quantity, String category, Callback<Boolean> callback) {
        List<GroceryItem> items = getLocalGroceryItems(householdId);
        for (GroceryItem item : items) {
            if (item.getItemId().equals(itemId)) {
                item.setName(name);
                item.setQuantity(quantity);
                item.setCategory(category);
                break;
            }
        }
        saveLocalGroceryItems(householdId, items);
        notifyActiveListener(householdId);

        if (isFirebaseAvailable && db != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("quantity", quantity);
            updates.put("category", category);

            db.collection("households")
                    .document(householdId)
                    .collection("grocery_items")
                    .document(itemId)
                    .update(updates)
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    public void toggleGroceryItemPurchased(String householdId, String itemId, boolean isPurchased, Callback<Boolean> callback) {
        List<GroceryItem> items = getLocalGroceryItems(householdId);
        for (GroceryItem item : items) {
            if (item.getItemId().equals(itemId)) {
                item.setPurchased(isPurchased);
                break;
            }
        }
        saveLocalGroceryItems(householdId, items);
        notifyActiveListener(householdId);

        if (isFirebaseAvailable && db != null) {
            db.collection("households")
                    .document(householdId)
                    .collection("grocery_items")
                    .document(itemId)
                    .update("purchased", isPurchased)
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }

    public void deleteGroceryItem(String householdId, String itemId, Callback<Boolean> callback) {
        List<GroceryItem> items = getLocalGroceryItems(householdId);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItemId().equals(itemId)) {
                items.remove(i);
                break;
            }
        }
        saveLocalGroceryItems(householdId, items);
        notifyActiveListener(householdId);

        if (isFirebaseAvailable && db != null) {
            db.collection("households")
                    .document(householdId)
                    .collection("grocery_items")
                    .document(itemId)
                    .delete()
                    .addOnCompleteListener(task -> callback.onSuccess(true));
        } else {
            callback.onSuccess(true);
        }
    }
}
