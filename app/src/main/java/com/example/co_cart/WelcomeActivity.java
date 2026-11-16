package com.example.co_cart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.co_cart.models.User;
import com.example.co_cart.repository.FirebaseHelper;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    private TextView tvWelcomeGreeting;
    private MaterialButton btnGetIntoApp;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        FirebaseHelper.init(this);
        firebaseHelper = FirebaseHelper.getInstance();

        tvWelcomeGreeting = findViewById(R.id.tvWelcomeGreeting);
        btnGetIntoApp = findViewById(R.id.btnGetIntoApp);

        String userName = getIntent().getStringExtra("userName");
        if (userName != null && !userName.isEmpty()) {
            String greeting = "Welcome back, " + userName + "!";
            tvWelcomeGreeting.setText(greeting);
        }

        btnGetIntoApp.setOnClickListener(v -> navigateNext());
    }

    private void navigateNext() {
        String userId = firebaseHelper.getCurrentUserId();
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                Intent intent;
                if (user.getHouseholdId() != null && !user.getHouseholdId().isEmpty()) {
                    intent = new Intent(WelcomeActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(WelcomeActivity.this, HouseholdActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Intent intent = new Intent(WelcomeActivity.this, HouseholdActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
