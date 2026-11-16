package com.example.co_cart;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.co_cart.adapter.GroceryAdapter;
import com.example.co_cart.adapter.JoinRequestAdapter;
import com.example.co_cart.models.GroceryItem;
import com.example.co_cart.models.Household;
import com.example.co_cart.models.JoinRequest;
import com.example.co_cart.models.User;
import com.example.co_cart.repository.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GroceryAdapter.OnItemActionListener {

    private TextView tvHouseholdName, tvInviteCode, tvItemCount;
    private TextView tvProgressLabel, tvProgressPercent, tvLiveCartTotal, tvPurchasedTotal;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton btnShareInvite, btnCopyCode, btnPendingRequests, btnClearPurchased;
    private RecyclerView rvGroceryList;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabAddItem;

    private GroceryAdapter adapter;
    private FirebaseHelper firebaseHelper;
    private ListenerRegistration groceryListener;
    private ListenerRegistration pendingRequestsListener;

    private String currentHouseholdId = "";
    private String currentHouseholdName = "";
    private String currentInviteCode = "";
    private List<JoinRequest> pendingJoinRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseHelper.init(this);
        firebaseHelper = FirebaseHelper.getInstance();

        if (!firebaseHelper.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupItemTouchHelper();
        loadHouseholdData();
    }

    private void initViews() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        tvHouseholdName = findViewById(R.id.tvHouseholdName);
        tvInviteCode = findViewById(R.id.tvInviteCode);
        tvItemCount = findViewById(R.id.tvItemCount);
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvLiveCartTotal = findViewById(R.id.tvLiveCartTotal);
        tvPurchasedTotal = findViewById(R.id.tvPurchasedTotal);
        btnClearPurchased = findViewById(R.id.btnClearPurchased);
        progressIndicator = findViewById(R.id.progressIndicator);
        btnPendingRequests = findViewById(R.id.btnPendingRequests);
        btnShareInvite = findViewById(R.id.btnShareInvite);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        MaterialButton btnQuickProfile = findViewById(R.id.btnQuickProfile);
        rvGroceryList = findViewById(R.id.rvGroceryList);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabAddItem = findViewById(R.id.fabAddItem);

        if (btnQuickProfile != null) {
            btnQuickProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        }
        btnPendingRequests.setOnClickListener(v -> showJoinRequestsDialog());
        btnShareInvite.setOnClickListener(v -> shareHouseholdInvite());
        btnCopyCode.setOnClickListener(v -> copyInviteCodeToClipboard());
        if (btnClearPurchased != null) {
            btnClearPurchased.setOnClickListener(v -> showClearSettlementDialog());
        }
        fabAddItem.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void setupRecyclerView() {
        adapter = new GroceryAdapter(this);
        rvGroceryList.setLayoutManager(new LinearLayoutManager(this));
        rvGroceryList.setAdapter(adapter);
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && adapter != null && adapter.getItems() != null && position < adapter.getItems().size()) {
                    GroceryItem item = adapter.getItems().get(position);
                    if (direction == ItemTouchHelper.RIGHT) {
                        boolean newStatus = !item.isPurchased();
                        onTogglePurchased(item, newStatus);
                        Toast.makeText(MainActivity.this, newStatus ? "Marked as Purchased ✔️" : "Marked as Active 🛒", Toast.LENGTH_SHORT).show();
                    } else if (direction == ItemTouchHelper.LEFT) {
                        onDelete(item);
                    }
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvGroceryList);
    }

    private void loadHouseholdData() {
        String userId = firebaseHelper.getCurrentUserId();
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user.getHouseholdId() == null || user.getHouseholdId().isEmpty()) {
                    startActivity(new Intent(MainActivity.this, HouseholdActivity.class));
                    finish();
                    return;
                }

                currentHouseholdId = user.getHouseholdId();
                fetchHouseholdDetails(currentHouseholdId);
                subscribeToGroceryItems(currentHouseholdId);
                subscribeToPendingRequests(currentHouseholdId);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchHouseholdDetails(String householdId) {
        firebaseHelper.getHouseholdDetails(householdId, new FirebaseHelper.Callback<Household>() {
            @Override
            public void onSuccess(Household household) {
                currentHouseholdName = household.getName();
                currentInviteCode = household.getInviteCode();
                tvHouseholdName.setText(currentHouseholdName);
                String codeText = "Invite Code: " + currentInviteCode;
                tvInviteCode.setText(codeText);
            }

            @Override
            public void onError(String errorMessage) {
                tvHouseholdName.setText("Shared Household");
            }
        });
    }

    private void subscribeToGroceryItems(String householdId) {
        if (groceryListener != null) {
            groceryListener.remove();
        }

        groceryListener = firebaseHelper.listenToGroceryItems(householdId, new FirebaseHelper.GroceryItemsListener() {
            @Override
            public void onItemsUpdated(List<GroceryItem> items) {
                sortItemsBySupermarketAisle(items);
                adapter.setItems(items);

                int totalCount = items.size();
                int purchasedCount = 0;
                double totalCost = 0.0;
                double purchasedCost = 0.0;

                for (GroceryItem item : items) {
                    totalCost += item.getEstimatedCost();
                    if (item.isPurchased()) {
                        purchasedCount++;
                        purchasedCost += item.getEstimatedCost();
                    }
                }

                String countText = totalCount + (totalCount == 1 ? " item" : " items");
                tvItemCount.setText(countText);

                // Update Progress Indicator Card
                String labelText = "Progress: " + purchasedCount + " of " + totalCount + " items purchased";
                tvProgressLabel.setText(labelText);

                int percent = totalCount > 0 ? (int) (((float) purchasedCount / totalCount) * 100) : 0;
                String percentText = percent + "%";
                tvProgressPercent.setText(percentText);
                progressIndicator.setProgress(percent, true);

                // Update Live Cart Total & Purchased Total
                if (tvLiveCartTotal != null) {
                    String totalText = "Total Cost: ₹ " + String.format(Locale.US, "%.2f", totalCost);
                    tvLiveCartTotal.setText(totalText);
                }

                if (tvPurchasedTotal != null) {
                    String purchasedText = "Purchased: ₹ " + String.format(Locale.US, "%.2f", purchasedCost);
                    tvPurchasedTotal.setText(purchasedText);
                }

                if (btnClearPurchased != null) {
                    btnClearPurchased.setVisibility(purchasedCount > 0 ? View.VISIBLE : View.GONE);
                }

                if (items.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvGroceryList.setVisibility(View.GONE);
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvGroceryList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Sync error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sortItemsBySupermarketAisle(List<GroceryItem> items) {
        if (items == null) return;
        java.util.Collections.sort(items, (a, b) -> {
            if (a.isPurchased() != b.isPurchased()) {
                return Boolean.compare(a.isPurchased(), b.isPurchased());
            }
            if (a.isUrgent() != b.isUrgent()) {
                return Boolean.compare(b.isUrgent(), a.isUrgent());
            }
            int rankA = getCategoryAisleRank(a.getCategory());
            int rankB = getCategoryAisleRank(b.getCategory());
            if (rankA != rankB) {
                return Integer.compare(rankA, rankB);
            }
            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        });
    }

    private int getCategoryAisleRank(String category) {
        if (category == null) return 6;
        if (category.contains("Dairy")) return 1;
        if (category.contains("Produce")) return 2;
        if (category.contains("Bakery")) return 3;
        if (category.contains("Snacks")) return 4;
        if (category.contains("Drinks")) return 5;
        return 6;
    }

    private void showClearSettlementDialog() {
        List<GroceryItem> items = adapter.getItems();
        int purchasedCount = 0;
        double purchasedAmount = 0.0;

        for (GroceryItem item : items) {
            if (item.isPurchased()) {
                purchasedCount++;
                purchasedAmount += item.getEstimatedCost();
            }
        }

        if (purchasedCount == 0) {
            Toast.makeText(this, "No purchased items to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        final double finalAmount = purchasedAmount;
        final int finalCount = purchasedCount;

        new AlertDialog.Builder(this)
                .setTitle("Clear Purchased Items 🧹")
                .setMessage("Are you sure you want to clear " + finalCount + " purchased item" + (finalCount == 1 ? "" : "s") + " (worth ₹ " + String.format(Locale.US, "%.2f", finalAmount) + ") from your checklist?")
                .setPositiveButton("Clear Items", (dialog, which) -> {
                    for (GroceryItem item : items) {
                        if (item.isPurchased()) {
                            firebaseHelper.deleteGroceryItem(currentHouseholdId, item.getItemId(), new FirebaseHelper.Callback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {}
                                @Override
                                public void onError(String errorMessage) {}
                            });
                        }
                    }
                    Toast.makeText(MainActivity.this, "Cleared " + finalCount + " purchased items!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void subscribeToPendingRequests(String householdId) {
        if (pendingRequestsListener != null) {
            pendingRequestsListener.remove();
        }

        pendingRequestsListener = firebaseHelper.listenToPendingJoinRequests(householdId, new FirebaseHelper.JoinRequestsListener() {
            @Override
            public void onRequestsUpdated(List<JoinRequest> requests) {
                pendingJoinRequests = requests;
                if (!requests.isEmpty()) {
                    btnPendingRequests.setVisibility(View.VISIBLE);
                    String btnText = requests.size() + (requests.size() == 1 ? " Pending Join Request 🔔" : " Pending Join Requests 🔔");
                    btnPendingRequests.setText(btnText);
                } else {
                    btnPendingRequests.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                btnPendingRequests.setVisibility(View.GONE);
            }
        });
    }

    private void showJoinRequestsDialog() {
        if (pendingJoinRequests.isEmpty()) {
            Toast.makeText(this, "No pending join requests.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_requests, null);
        builder.setView(dialogView);

        RecyclerView rvJoinRequests = dialogView.findViewById(R.id.rvJoinRequests);
        rvJoinRequests.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = builder.create();

        JoinRequestAdapter requestAdapter = new JoinRequestAdapter((request, accept) -> {
            firebaseHelper.respondToJoinRequest(request, accept, new FirebaseHelper.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Toast.makeText(MainActivity.this, accept ? "Request Accepted! Roommate joined." : "Request Rejected.", Toast.LENGTH_SHORT).show();
                    if (pendingJoinRequests.isEmpty() && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(MainActivity.this, "Error responding to request", Toast.LENGTH_SHORT).show();
                }
            });
        });

        rvJoinRequests.setAdapter(requestAdapter);
        requestAdapter.setRequests(pendingJoinRequests);

        builder.setNegativeButton("Close", null);
        dialog.show();
    }

    private void showAddEditDialog(GroceryItem itemToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_item, null);
        builder.setView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputEditText etItemName = dialogView.findViewById(R.id.etItemName);
        TextInputEditText etItemQuantity = dialogView.findViewById(R.id.etItemQuantity);
        TextInputEditText etItemCost = dialogView.findViewById(R.id.etItemCost);
        SwitchMaterial switchUrgent = dialogView.findViewById(R.id.switchUrgent);
        ChipGroup chipGroupCategory = dialogView.findViewById(R.id.chipGroupCategory);

        Chip chipPreset1Ltr = dialogView.findViewById(R.id.chipPreset1Ltr);
        Chip chipPreset1Kg = dialogView.findViewById(R.id.chipPreset1Kg);
        Chip chipPreset1Pack = dialogView.findViewById(R.id.chipPreset1Pack);
        Chip chipPreset500g = dialogView.findViewById(R.id.chipPreset500g);
        Chip chipPreset2Pcs = dialogView.findViewById(R.id.chipPreset2Pcs);

        View.OnClickListener presetListener = v -> {
            Chip chip = (Chip) v;
            etItemQuantity.setText(chip.getText().toString());
        };

        if (chipPreset1Ltr != null) chipPreset1Ltr.setOnClickListener(presetListener);
        if (chipPreset1Kg != null) chipPreset1Kg.setOnClickListener(presetListener);
        if (chipPreset1Pack != null) chipPreset1Pack.setOnClickListener(presetListener);
        if (chipPreset500g != null) chipPreset500g.setOnClickListener(presetListener);
        if (chipPreset2Pcs != null) chipPreset2Pcs.setOnClickListener(presetListener);

        boolean isEditing = itemToEdit != null;
        if (isEditing) {
            tvDialogTitle.setText("Edit Grocery Item");
            etItemName.setText(itemToEdit.getName());
            etItemQuantity.setText(itemToEdit.getQuantity());
            if (etItemCost != null && itemToEdit.getEstimatedCost() > 0) {
                etItemCost.setText(String.valueOf(itemToEdit.getEstimatedCost()));
            }
            if (switchUrgent != null) {
                switchUrgent.setChecked(itemToEdit.isUrgent());
            }
            selectCategoryChip(chipGroupCategory, itemToEdit.getCategory());
        } else {
            tvDialogTitle.setText("Add Grocery Item");
        }

        builder.setPositiveButton(isEditing ? "Update" : "Add", (dialog, which) -> {
            String name = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
            String quantity = etItemQuantity.getText() != null ? etItemQuantity.getText().toString().trim() : "";
            String costStr = etItemCost != null && etItemCost.getText() != null ? etItemCost.getText().toString().trim() : "0";
            boolean isUrgent = switchUrgent != null && switchUrgent.isChecked();
            String category = getSelectedCategory(chipGroupCategory);

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(MainActivity.this, "Item name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(quantity)) {
                quantity = "1";
            }

            double cost = 0.0;
            try {
                if (!TextUtils.isEmpty(costStr)) {
                    cost = Double.parseDouble(costStr);
                }
            } catch (Exception e) {
                cost = 0.0;
            }

            if (isEditing) {
                itemToEdit.setName(name);
                itemToEdit.setQuantity(quantity);
                itemToEdit.setCategory(category);
                itemToEdit.setEstimatedCost(cost);
                itemToEdit.setUrgent(isUrgent);

                firebaseHelper.updateGroceryItem(currentHouseholdId, itemToEdit.getItemId(), name, quantity, category, new FirebaseHelper.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(MainActivity.this, "Item updated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Error updating item: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                firebaseHelper.addGroceryItem(currentHouseholdId, name, quantity, category, cost, isUrgent, new FirebaseHelper.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(MainActivity.this, "Item added", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Error adding item: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private String getSelectedCategory(ChipGroup chipGroup) {
        if (chipGroup == null) return "General 🛒";
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip checkedChip = chipGroup.findViewById(checkedId);
            if (checkedChip != null && checkedChip.getText() != null) {
                return checkedChip.getText().toString();
            }
        }
        return "General 🛒";
    }

    private void selectCategoryChip(ChipGroup chipGroup, String category) {
        if (chipGroup == null || category == null) return;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.getText() != null && chip.getText().toString().equalsIgnoreCase(category)) {
                    chip.setChecked(true);
                    break;
                }
            }
        }
    }

    @Override
    public void onTogglePurchased(GroceryItem item, boolean isPurchased) {
        firebaseHelper.toggleGroceryItemPurchased(currentHouseholdId, item.getItemId(), isPurchased, new FirebaseHelper.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Item status updated via real-time listener
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Failed to update item status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEdit(GroceryItem item) {
        showAddEditDialog(item);
    }

    @Override
    public void onDelete(GroceryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete '" + item.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebaseHelper.deleteGroceryItem(currentHouseholdId, item.getItemId(), new FirebaseHelper.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Toast.makeText(MainActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Failed to delete item", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareHouseholdInvite() {
        if (TextUtils.isEmpty(currentInviteCode)) {
            Toast.makeText(this, "Invite code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Join my household '" + (currentHouseholdName.isEmpty() ? "Co-Cart" : currentHouseholdName) + "' on Co-Cart to manage our grocery checklist together!\n\nUse Invite Code: " + currentInviteCode;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share Household Invite Code"));
    }

    private void copyInviteCodeToClipboard() {
        if (TextUtils.isEmpty(currentInviteCode)) {
            Toast.makeText(this, "Invite code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Household Code", currentInviteCode);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Household Code copied to clipboard: " + currentInviteCode, Toast.LENGTH_LONG).show();
        }
    }

    private void regenerateHouseholdCode() {
        new AlertDialog.Builder(this)
                .setTitle("Regenerate Code")
                .setMessage("Are you sure you want to generate a new Household Invite Code?")
                .setPositiveButton("Regenerate", (dialog, which) -> {
                    firebaseHelper.regenerateHouseholdCode(currentHouseholdId, new FirebaseHelper.Callback<String>() {
                        @Override
                        public void onSuccess(String newCode) {
                            currentInviteCode = newCode;
                            String codeText = "Invite Code: " + currentInviteCode;
                            tvInviteCode.setText(codeText);
                            Toast.makeText(MainActivity.this, "New Invite Code generated: " + newCode, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Failed to regenerate code", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_profile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_leave_household) {
            leaveHousehold();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_pending_requests) {
            showJoinRequestsDialog();
            return true;
        } else if (id == R.id.action_regenerate_code) {
            regenerateHouseholdCode();
            return true;
        } else if (id == R.id.action_share_code) {
            shareHouseholdInvite();
            return true;
        } else if (id == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Settings ⚙️")
                .setMessage("Co-Cart Version 1.0\n\n• Real-Time Cloud Sync: Active\n• Supermarket Aisle Auto-Sorting: Active\n• Swipe Gestures: Enabled\n• Host Approval Security: Enabled")
                .setPositiveButton("OK", null)
                .show();
    }

    private void leaveHousehold() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Household")
                .setMessage("Are you sure you want to leave this household?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    firebaseHelper.leaveHousehold(new FirebaseHelper.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Intent intent = new Intent(MainActivity.this, HouseholdActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Error leaving household", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        firebaseHelper.logout();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groceryListener != null) {
            groceryListener.remove();
        }
        if (pendingRequestsListener != null) {
            pendingRequestsListener.remove();
        }
    }
}
