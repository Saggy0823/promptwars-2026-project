package com.aipath.careermitra;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private final List<Animator> loopingAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // ---------- ORIGINAL LOGIC (unchanged) ----------
        Button getRecommendationsBtn = findViewById(R.id.btn_get_recommendations);
        getRecommendationsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, EducationActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        // ------------------------------------------------

        // ---------- NEW: Know your Knowledge gap ----------
        Button knowledgeGapBtn = findViewById(R.id.btn_knowledge_gap);
        knowledgeGapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, UserDataActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        // --------------------------------------------------

        playEntranceAnimation();
        startAmbientMotion();
        addPressFeedback(getRecommendationsBtn);
        addPressFeedback(knowledgeGapBtn); // UI
    }

    /** Staggered fade + slide-up for each element. */
    private void playEntranceAnimation() {
        View[] items = new View[]{
                findViewById(R.id.fl_icon),
                findViewById(R.id.tv_title),
                findViewById(R.id.tv_subtitle),
                findViewById(R.id.fl_button_wrap),
                findViewById(R.id.tv_or),
                findViewById(R.id.fl_gap_wrap),
                findViewById(R.id.ll_chips)
        };
        for (int i = 0; i < items.length; i++) {
            View v = items[i];
            v.setAlpha(0f);
            v.setTranslationY(70f);
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(200L + i * 120L)
                    .setDuration(650L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }
    }

    /** Looping motion: floating emoji, pulse ring, drifting blobs, breathing primary button. */
    private void startAmbientMotion() {
        // Emoji gently floats
        loop(ObjectAnimator.ofFloat(findViewById(R.id.tv_icon_emoji), "translationY", 0f, -14f), 2200L);

        // Pulse ring expands and fades
        View ring = findViewById(R.id.view_pulse_ring);
        ObjectAnimator ringX = ObjectAnimator.ofFloat(ring, "scaleX", 0.85f, 1.35f);
        ObjectAnimator ringY = ObjectAnimator.ofFloat(ring, "scaleY", 0.85f, 1.35f);
        ObjectAnimator ringA = ObjectAnimator.ofFloat(ring, "alpha", 0.9f, 0f);
        for (ObjectAnimator a : new ObjectAnimator[]{ringX, ringY, ringA}) {
            a.setDuration(2000L);
            a.setRepeatCount(ObjectAnimator.INFINITE);
            a.setInterpolator(new LinearInterpolator());
            a.start();
            loopingAnimators.add(a);
        }

        // Background blobs drift slowly
        View blobTop = findViewById(R.id.blob_top);
        View blobBottom = findViewById(R.id.blob_bottom);
        loop(ObjectAnimator.ofFloat(blobTop, "translationY", 0f, 40f), 6000L);
        loop(ObjectAnimator.ofFloat(blobTop, "translationX", 0f, -30f), 7500L);
        loop(ObjectAnimator.ofFloat(blobBottom, "translationY", 0f, -50f), 7000L);
        loop(ObjectAnimator.ofFloat(blobBottom, "translationX", 0f, 40f), 8500L);

        // Only the PRIMARY button wrapper breathes; the secondary stays still
        View wrap = findViewById(R.id.fl_button_wrap);
        loop(ObjectAnimator.ofFloat(wrap, "scaleX", 1f, 1.04f), 1400L);
        loop(ObjectAnimator.ofFloat(wrap, "scaleY", 1f, 1.04f), 1400L);
    }

    private void loop(ObjectAnimator animator, long duration) {
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
        loopingAnimators.add(animator);
    }

    /** Press-in / release scale. Returns false so the normal click still fires. */
    private void addPressFeedback(View button) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(120L).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(180L).start();
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        for (Animator a : loopingAnimators) {
            a.cancel();
        }
        loopingAnimators.clear();
        super.onDestroy();
    }
}