package com.aipath.careermitra;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    private Handler handler;
    private Runnable goToWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ---------- ORIGINAL LOGIC (unchanged) ----------
        // Navigate to WelcomeActivity after delay
        handler = new Handler();
        goToWelcome = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish(); // Close SplashActivity
            }
        };
        handler.postDelayed(goToWelcome, SPLASH_DELAY);
        // ------------------------------------------------

        playSplashAnimation();
    }

    private void playSplashAnimation() {
        View logoWrap = findViewById(R.id.splash_logo_wrap);
        View logo = findViewById(R.id.splash_logo);
        View title = findViewById(R.id.splash_title);
        View tagline = findViewById(R.id.splash_tagline);
        View progressWrap = findViewById(R.id.splash_progress_wrap);
        View progressFill = findViewById(R.id.splash_progress_fill);
        View ring = findViewById(R.id.splash_pulse_ring);

        // Logo pops in with a slight overshoot and a small spin
        logoWrap.setAlpha(0f);
        logoWrap.setScaleX(0.3f);
        logoWrap.setScaleY(0.3f);
        logoWrap.setRotation(-20f);
        logoWrap.animate()
                .alpha(1f).scaleX(1f).scaleY(1f).rotation(0f)
                .setDuration(800L)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        // Logo then breathes gently
        ObjectAnimator breatheX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.06f);
        ObjectAnimator breatheY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.06f);
        for (ObjectAnimator a : new ObjectAnimator[]{breatheX, breatheY}) {
            a.setStartDelay(800L);
            a.setDuration(900L);
            a.setRepeatCount(ObjectAnimator.INFINITE);
            a.setRepeatMode(ObjectAnimator.REVERSE);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.start();
        }

        // Pulse ring
        ObjectAnimator ringX = ObjectAnimator.ofFloat(ring, "scaleX", 0.85f, 1.3f);
        ObjectAnimator ringY = ObjectAnimator.ofFloat(ring, "scaleY", 0.85f, 1.3f);
        ObjectAnimator ringA = ObjectAnimator.ofFloat(ring, "alpha", 0.9f, 0f);
        for (ObjectAnimator a : new ObjectAnimator[]{ringX, ringY, ringA}) {
            a.setDuration(1500L);
            a.setRepeatCount(ObjectAnimator.INFINITE);
            a.setInterpolator(new LinearInterpolator());
            a.start();
        }

        // Title and tagline slide up and fade in
        fadeUp(title, 450L);
        fadeUp(tagline, 650L);

        // Progress bar fades in and fills over the splash duration
        progressWrap.setAlpha(0f);
        progressWrap.animate().alpha(1f).setStartDelay(300L).setDuration(400L).start();
        progressFill.setPivotX(0f);
        progressFill.setScaleX(0f);
        progressFill.animate()
                .scaleX(1f)
                .setStartDelay(300L)
                .setDuration(SPLASH_DELAY - 300L)
                .setInterpolator(new LinearInterpolator())
                .start();

        // Drifting background blobs
        drift(findViewById(R.id.splash_blob_top), "translationY", 40f, 5000L);
        drift(findViewById(R.id.splash_blob_bottom), "translationY", -50f, 6000L);
    }

    private void fadeUp(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(40f);
        v.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(delay)
                .setDuration(600L)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
    }

    private void drift(View v, String property, float distance, long duration) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, property, 0f, distance);
        a.setDuration(duration);
        a.setRepeatCount(ObjectAnimator.INFINITE);
        a.setRepeatMode(ObjectAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
    }

    @Override
    protected void onDestroy() {
        if (handler != null && goToWelcome != null) {
            handler.removeCallbacks(goToWelcome);
        }
        super.onDestroy();
    }
}