package com.example.co_cart;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.co_cart.models.Household;
import com.example.co_cart.models.JoinRequest;
import com.example.co_cart.repository.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

public class HouseholdActivity extends AppCompatActivity {

    private TextInputEditText etHouseholdName, etHouseholdCode;
    private MaterialButton btnCreateHousehold, btnJoinHousehold;
    private TextView tvLogout;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    private AlertDialog waitingDialog;
    private ListenerRegistration requestStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_household);

        FirebaseHelper.init(this);
        firebaseHelper = FirebaseHelper.getInstance();

        etHouseholdName = findViewById(R.id.etHouseholdName);
        etHouseholdCode = findViewById(R.id.etHouseholdCode);
        btnCreateHousehold = findViewById(R.id.btnCreateHousehold);
        btnJoinHousehold = findViewById(R.id.btnJoinHousehold);
        tvLogout = findViewById(R.id.tvLogout);
        progressBar = findViewById(R.id.progressBar);

        btnCreateHousehold.setOnClickListener(v -> createHousehold());

        btnJoinHousehold.setOnClickListener(v -> joinHousehold());

        tvLogout.setOnClickListener(v -> {
            firebaseHelper.logout();
            Intent intent = new Intent(HouseholdActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void createHousehold() {
        String name = etHouseholdName.getText() != null ? etHouseholdName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            etHouseholdName.setError("Household name is required");
            return;
        }

        setLoading(true);
        firebaseHelper.createHousehold(name, new FirebaseHelper.Callback<Household>() {
            @Override
            public void onSuccess(Household household) {
                setLoading(false);
                Toast.makeText(HouseholdActivity.this, "Household created! Code: " + household.getInviteCode(), Toast.LENGTH_LONG).show();
                navigateToMain();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(HouseholdActivity.this, "Error creating household: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void joinHousehold() {
        String code = etHouseholdCode.getText() != null ? etHouseholdCode.getText().toString().trim() : "";
        if (TextUtils.isEmpty(code) || !code.matches("[A-Za-z0-9]{6}")) {
            etHouseholdCode.setError("Enter a valid 6-character code");
            return;
        }

        setLoading(true);
        firebaseHelper.sendJoinRequest(code, new FirebaseHelper.Callback<JoinRequest>() {
            @Override
            public void onSuccess(JoinRequest request) {
                setLoading(false);
                showWaitingForApprovalDialog(code);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(HouseholdActivity.this, "Failed to send join request: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showWaitingForApprovalDialog(String code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join Request Sent ⏳");
        builder.setMessage("Your request to join household '" + code.toUpperCase() + "' has been sent to the Host.\n\nPlease wait for the Host to accept your request...");
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel Request", (dialog, which) -> {
            if (requestStatusListener != null) {
                requestStatusListener.remove();
            }
        });

        waitingDialog = builder.create();
        waitingDialog.show();

        // Listen for Host Approval
        requestStatusListener = firebaseHelper.listenToMyJoinRequestStatus(code, new FirebaseHelper.JoinRequestStatusListener() {
            @Override
            public void onAccepted(String householdId) {
                if (waitingDialog != null && waitingDialog.isShowing()) {
                    waitingDialog.dismiss();
                }
                Toast.makeText(HouseholdActivity.this, "Request Accepted by Host! Welcome 🎉", Toast.LENGTH_LONG).show();
                navigateToMain();
            }

            @Override
            public void onRejected() {
                if (waitingDialog != null && waitingDialog.isShowing()) {
                    waitingDialog.dismiss();
                }
                Toast.makeText(HouseholdActivity.this, "Join request was rejected by the Host.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                // Ignore transient network errors
            }
        });
    }

    private void navigateToMain() {
        if (requestStatusListener != null) {
            requestStatusListener.remove();
        }
        Intent intent = new Intent(HouseholdActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            btnCreateHousehold.setEnabled(false);
            btnJoinHousehold.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnCreateHousehold.setEnabled(true);
            btnJoinHousehold.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestStatusListener != null) {
            requestStatusListener.remove();
        }
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }
    }
}
