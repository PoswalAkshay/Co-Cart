package com.example.co_cart;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.co_cart.models.User;
import com.example.co_cart.repository.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegisterLink;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseHelper.init(this);
        firebaseHelper = FirebaseHelper.getInstance();

        if (firebaseHelper.isLoggedIn()) {
            navigateAfterAuth();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        setLoading(true);

        firebaseHelper.loginUser(email, password, new FirebaseHelper.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Welcome back, " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                navigateAfterAuth();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Login Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateAfterAuth() {
        String userId = firebaseHelper.getCurrentUserId();
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Intent intent;
                if (user.getHouseholdId() != null && !user.getHouseholdId().isEmpty()) {
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, HouseholdActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Intent intent = new Intent(LoginActivity.this, HouseholdActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
        }
    }
}
