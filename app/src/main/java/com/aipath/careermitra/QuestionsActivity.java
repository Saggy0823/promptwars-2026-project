package com.aipath.careermitra;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class QuestionsActivity extends AppCompatActivity {

    private SeekBar seekBar1, seekBar2, seekBar3, seekBar4, seekBar5;
    private Button buttonGetRecommendations;

    // UI: mood emoji for each slider value 0..10
    private static final String[] MOODS = {
            "😶", "😞", "😕", "😐", "🙂", "🙂", "😊", "😄", "😄", "🤩", "🚀"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        seekBar1 = findViewById(R.id.seekBar1);
        seekBar2 = findViewById(R.id.seekBar2);
        seekBar3 = findViewById(R.id.seekBar3);
        seekBar4 = findViewById(R.id.seekBar4);
        seekBar5 = findViewById(R.id.seekBar5);
        buttonGetRecommendations = findViewById(R.id.buttonGetRecommendations);

        buttonGetRecommendations.setOnClickListener(v -> {

            HashMap<String, Integer> questionsMap = new HashMap<>();
            questionsMap.put("question1", seekBar1.getProgress());
            questionsMap.put("question2", seekBar2.getProgress());
            questionsMap.put("question3", seekBar3.getProgress());
            questionsMap.put("question4", seekBar4.getProgress());
            questionsMap.put("question5", seekBar5.getProgress());

            Intent prevIntent = getIntent();
            String name = prevIntent.getStringExtra("name");
            ArrayList<String> education = prevIntent.getStringArrayListExtra("education");
            ArrayList<String> skills = prevIntent.getStringArrayListExtra("skills");
            ArrayList<String> interests = prevIntent.getStringArrayListExtra("interests");
            ArrayList<String> experience = prevIntent.getStringArrayListExtra("experience");

            if (name == null || name.trim().isEmpty()) {
                Toast.makeText(this, "Name missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            String userKey = name.trim().toLowerCase();

            DatabaseReference inputsRef =
                    FirebaseDatabase.getInstance().getReference("inputs").child(userKey);

            DatabaseReference outputsRef =
                    FirebaseDatabase.getInstance().getReference("outputs").child(userKey);

            setLoading(true); // UI: show "Generating..." and block double taps

            outputsRef.removeValue().addOnCompleteListener(deleteTask -> {
                if (!deleteTask.isSuccessful()) {
                    setLoading(false); // UI
                    Toast.makeText(this, "Failed to clear old data", Toast.LENGTH_SHORT).show();
                    return;
                }

                HashMap<String, Object> userInput = new HashMap<>();
                userInput.put("name", name);
                userInput.put("education", education);
                userInput.put("skills", skills);
                userInput.put("interests", interests);
                userInput.put("experience", experience);
                userInput.put("questions", questionsMap);

                // 🔥 REQUIRED
                userInput.put("updatedAt", System.currentTimeMillis());

                inputsRef.setValue(userInput).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Generating recommendations...", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(
                                QuestionsActivity.this,
                                RecommendationsActivity.class
                        );
                        intent.putExtra("username", userKey);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // UI
                        finish();
                    } else {
                        setLoading(false); // UI
                        Toast.makeText(this, "Failed to upload data", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // UI: live value labels, then animations (run after the original setup)
        bindSlider(seekBar1, (TextView) findViewById(R.id.tvValue1));
        bindSlider(seekBar2, (TextView) findViewById(R.id.tvValue2));
        bindSlider(seekBar3, (TextView) findViewById(R.id.tvValue3));
        bindSlider(seekBar4, (TextView) findViewById(R.id.tvValue4));
        bindSlider(seekBar5, (TextView) findViewById(R.id.tvValue5));
        playEntranceAnimation();
        addPressFeedback(buttonGetRecommendations);
    }

    // ---------------- UI helpers (no effect on app logic) ----------------

    /** Shows "emoji value/10" in the pill and bounces it as the slider moves. */
    private void bindSlider(SeekBar seekBar, final TextView valueView) {
        valueView.setText(MOODS[seekBar.getProgress()] + "  " + seekBar.getProgress() + "/10");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                valueView.setText(MOODS[progress] + "  " + progress + "/10");
                if (fromUser) {
                    sb.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    valueView.animate().cancel();
                    valueView.setScaleX(1.18f);
                    valueView.setScaleY(1.18f);
                    valueView.animate().scaleX(1f).scaleY(1f).setDuration(160L).start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) { }

            @Override
            public void onStopTrackingTouch(SeekBar sb) { }
        });
    }

    /** Disables the button and changes its label while Firebase is working. */
    private void setLoading(boolean loading) {
        buttonGetRecommendations.setEnabled(!loading);
        buttonGetRecommendations.setText(loading
                ? "Generating…"
                : "Get Career Recommendations  ✨");
    }

    private void playEntranceAnimation() {
        View[] header = new View[]{
                findViewById(R.id.tvBrand),
                findViewById(R.id.tvTitle),
                findViewById(R.id.tvSubtitle)
        };
        for (int i = 0; i < header.length; i++) {
            View v = header[i];
            v.setAlpha(0f);
            v.setTranslationY(-30f);
            v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(100L + i * 110L)
                    .setDuration(550L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }

        slideUp(findViewById(R.id.cardQuestions), 350L, 120f);

        // Each question fades up one after another inside the card
        int[] ids = {R.id.q1, R.id.q2, R.id.q3, R.id.q4, R.id.q5};
        for (int i = 0; i < ids.length; i++) {
            View q = findViewById(ids[i]);
            q.setAlpha(0f);
            q.setTranslationY(40f);
            q.animate().alpha(1f).translationY(0f)
                    .setStartDelay(600L + i * 120L)
                    .setDuration(500L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }

        slideUp(buttonGetRecommendations, 1200L, 80f);

        // Slowly drifting header blob
        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobQuestions), "translationY", 0f, 35f);
        drift.setDuration(6000L);
        drift.setRepeatCount(ObjectAnimator.INFINITE);
        drift.setRepeatMode(ObjectAnimator.REVERSE);
        drift.setInterpolator(new AccelerateDecelerateInterpolator());
        drift.start();
    }

    private void slideUp(View v, long delay, float distance) {
        v.setAlpha(0f);
        v.setTranslationY(distance);
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delay).setDuration(650L)
                .setInterpolator(new DecelerateInterpolator(2.2f))
                .start();
    }

    /** Press-in/release scale. Returns false so the normal click still fires. */
    private void addPressFeedback(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(110L).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(160L).start();
                    break;
            }
            return false;
        });
    }
}