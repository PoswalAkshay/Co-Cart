package com.example.co_cart;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.co_cart.models.User;
import com.example.co_cart.repository.FirebaseHelper;

public class SplashActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseHelper.init(this);
        firebaseHelper = FirebaseHelper.getInstance();

        View layoutContent = findViewById(R.id.layoutSplashContent);

        // ObjectAnimator for logo & title scaling and fade-in
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(layoutContent, View.SCALE_X, 0.5f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(layoutContent, View.SCALE_Y, 0.5f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(layoutContent, View.ALPHA, 0.0f, 1.0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(1200);
        animatorSet.setInterpolator(new DecelerateInterpolator());

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                checkAuthAndNavigate();
            }
        });

        animatorSet.start();
    }

    private void checkAuthAndNavigate() {
        if (firebaseHelper.isLoggedIn()) {
            String userId = firebaseHelper.getCurrentUserId();
            firebaseHelper.getUserProfile(userId, new FirebaseHelper.Callback<User>() {
                @Override
                public void onSuccess(User user) {
                    Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                    intent.putExtra("userName", user.getDisplayName() != null ? user.getDisplayName() : "User");
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                    intent.putExtra("userName", "User");
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
