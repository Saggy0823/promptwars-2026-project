package com.aipath.careermitra;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;

public class EducationActivity extends AppCompatActivity {

    private EditText editTextName;
    private ChipGroup chipGroupEducation;
    private Button buttonNext;
    private LinearLayout layoutSpecializationContainer;

    // Map to hold dynamic input fields for each selected advanced education
    private HashMap<String, EditText> dynamicInputs = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education);

        editTextName = findViewById(R.id.editTextName);
        chipGroupEducation = findViewById(R.id.chipGroupEducation);
        buttonNext = findViewById(R.id.buttonNext);
        layoutSpecializationContainer = findViewById(R.id.layoutSpecializationContainer);

        // Education list
        ArrayList<String> educationList = new ArrayList<>();
        educationList.add("10th Grade");
        educationList.add("12th Grade");
        educationList.add("Diploma");
        educationList.add("Bachelor's Degree");
        educationList.add("Master's Degree");
        educationList.add("PhD");
        educationList.add("Certificate Courses");
        educationList.add("Vocational Training");

        for (String edu : educationList) {
            Chip chip = new Chip(this);
            chip.setText(edu);
            chip.setCheckable(true);
            chip.setTextSize(16f);
            chip.setChipCornerRadius(50f);
            chip.setPadding(24, 12, 24, 12);

            // UI: chip colors, border and size
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.chip_bg));
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text));
            chip.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.chip_stroke));
            chip.setChipStrokeWidth(dp(1));
            chip.setChipMinHeight(dp(44));

            chipGroupEducation.addView(chip);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // UI: small bounce whenever a chip is toggled
                bounce(buttonView);

                if (isChecked) {
                    // Only show dynamic input for advanced education
                    if (edu.equals("Diploma") || edu.equals("Bachelor's Degree") ||
                            edu.equals("Master's Degree") || edu.equals("PhD") ||
                            edu.equals("Certificate Courses")) {

                        EditText editText = new EditText(EducationActivity.this);
                        editText.setHint(getHintForEducation(edu));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.topMargin = dp(10); // UI: spacing between fields
                        editText.setLayoutParams(params);
                        editText.setPadding(24, 24, 24, 24);

                        // UI: field styling
                        editText.setBackgroundResource(R.drawable.bg_input_field);
                        editText.setPadding(dp(16), dp(14), dp(16), dp(14));
                        editText.setTextSize(15f);
                        editText.setTextColor(0xFF1C1C1E);
                        editText.setHintTextColor(0xFF9A9AA5);

                        layoutSpecializationContainer.addView(editText);
                        dynamicInputs.put(edu, editText);

                        // UI: slide-in and fade-in
                        editText.setAlpha(0f);
                        editText.setTranslationY(-24f);
                        editText.animate().alpha(1f).translationY(0f)
                                .setDuration(350L)
                                .setInterpolator(new DecelerateInterpolator(2f))
                                .start();
                    }
                } else {
                    // Remove input field if deselected
                    if (dynamicInputs.containsKey(edu)) {
                        // UI: fade out first, then remove the view. The map is
                        // updated immediately so the logic is unaffected.
                        final EditText toRemove = dynamicInputs.get(edu);
                        dynamicInputs.remove(edu);
                        toRemove.animate().alpha(0f).translationY(-16f)
                                .setDuration(200L)
                                .withEndAction(() -> layoutSpecializationContainer.removeView(toRemove))
                                .start();
                    }
                }
            });
        }

        buttonNext.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            if (name.isEmpty()) {
                shake(findViewById(R.id.til_name)); // UI: shake the field
                Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> selectedEducations = new ArrayList<>();
            HashMap<String, String> specializationMap = new HashMap<>();

            for (int i = 0; i < chipGroupEducation.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupEducation.getChildAt(i);
                if (chip.isChecked()) {
                    selectedEducations.add(chip.getText().toString());

                    // Save specialization or degree name if present
                    if (dynamicInputs.containsKey(chip.getText().toString())) {
                        String specialization = dynamicInputs.get(chip.getText().toString())
                                .getText().toString().trim();
                        specializationMap.put(chip.getText().toString(), specialization);
                    }
                }
            }

            if (selectedEducations.isEmpty()) {
                shake(chipGroupEducation); // UI: shake the chips
                Toast.makeText(this, "Select at least one education", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass data to next activity
            Intent intent = new Intent(EducationActivity.this, SkillsActivity.class);
            intent.putExtra("name", name);
            intent.putStringArrayListExtra("education", selectedEducations);

            // For specialization map, you can serialize as JSON if needed
            for (String key : specializationMap.keySet()) {
                intent.putExtra(key + "_specialization", specializationMap.get(key));
            }

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // UI
        });

        // UI: animations (run after the original setup)
        playEntranceAnimation();
        animateChipsIn();
        addPressFeedback(buttonNext);
    }

    // ---------------- UI helpers (no effect on app logic) ----------------

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void playEntranceAnimation() {
        View[] items = new View[]{
                findViewById(R.id.tv_edu_brand),
                findViewById(R.id.tv_edu_title),
                findViewById(R.id.tv_edu_sub)
        };
        for (int i = 0; i < items.length; i++) {
            View v = items[i];
            v.setAlpha(0f);
            v.setTranslationY(-30f);
            v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(100L + i * 110L)
                    .setDuration(550L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }

        View card = findViewById(R.id.card_education);
        card.setAlpha(0f);
        card.setTranslationY(120f);
        card.animate().alpha(1f).translationY(0f)
                .setStartDelay(350L).setDuration(700L)
                .setInterpolator(new DecelerateInterpolator(2.2f))
                .start();

        buttonNext.setAlpha(0f);
        buttonNext.setTranslationY(80f);
        buttonNext.animate().alpha(1f).translationY(0f)
                .setStartDelay(650L).setDuration(600L)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();

        // Slowly drifting header blob
        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blob_edu), "translationY", 0f, 35f);
        drift.setDuration(6000L);
        drift.setRepeatCount(ObjectAnimator.INFINITE);
        drift.setRepeatMode(ObjectAnimator.REVERSE);
        drift.setInterpolator(new AccelerateDecelerateInterpolator());
        drift.start();
    }

    /** Chips pop in one after another. */
    private void animateChipsIn() {
        for (int i = 0; i < chipGroupEducation.getChildCount(); i++) {
            View chip = chipGroupEducation.getChildAt(i);
            chip.setAlpha(0f);
            chip.setScaleX(0.6f);
            chip.setScaleY(0.6f);
            chip.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(700L + i * 60L)
                    .setDuration(380L)
                    .setInterpolator(new OvershootInterpolator(1.6f))
                    .start();
        }
    }

    private void bounce(final View v) {
        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(110L)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(140L).start())
                .start();
    }

    private void shake(View v) {
        ObjectAnimator.ofFloat(v, "translationX", 0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f)
                .setDuration(420L).start();
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

    private String getHintForEducation(String edu) {
        switch (edu) {
            case "Diploma":
                return "Enter specialization (e.g., Mechanical, Nursing)";
            case "Bachelor's Degree":
                return "Enter your degree (e.g., B.Tech in CS)";
            case "Master's Degree":
                return "Enter your degree (e.g., MBA in Marketing)";
            case "PhD":
                return "Enter your PhD specialization";
            case "Certificate Courses":
                return "Enter course name (e.g., Python)";
            default:
                return "";
        }
    }
}