package com.aipath.careermitra;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ExperienceActivity extends AppCompatActivity {

    private EditText editTextRole, editTextDuration, editTextCompany;
    private Button buttonAddExperience, buttonNext;
    private RecyclerView recyclerViewExperience;
    private ExperienceAdapter adapter;
    private ArrayList<String> experienceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experience);

        editTextRole = findViewById(R.id.editTextRole);
        editTextDuration = findViewById(R.id.editTextDuration);
        editTextCompany = findViewById(R.id.editTextCompany);
        buttonAddExperience = findViewById(R.id.buttonAddExperience);
        buttonNext = findViewById(R.id.buttonNext);
        recyclerViewExperience = findViewById(R.id.recyclerViewExperience);

        experienceList = new ArrayList<>();
        adapter = new ExperienceAdapter(experienceList);
        recyclerViewExperience.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExperience.setAdapter(adapter);

        buttonAddExperience.setOnClickListener(v -> {
            String role = editTextRole.getText().toString().trim();
            String duration = editTextDuration.getText().toString().trim();
            String company = editTextCompany.getText().toString().trim();

            if (role.isEmpty() || duration.isEmpty() || company.isEmpty()) {
                // UI: shake only the fields that are empty
                if (role.isEmpty()) shake(editTextRole);
                if (duration.isEmpty()) shake(editTextDuration);
                if (company.isEmpty()) shake(editTextCompany);
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String exp = role + " | " + duration + " | " + company;
            experienceList.add(exp);
            adapter.notifyItemInserted(experienceList.size() - 1);

            editTextRole.setText("");
            editTextDuration.setText("");
            editTextCompany.setText("");

            // UI
            updateEmptyState();
            recyclerViewExperience.smoothScrollToPosition(experienceList.size() - 1);
            hideKeyboard();
            editTextRole.requestFocus();
        });

        buttonNext.setOnClickListener(v -> {
            if (experienceList.isEmpty()) {
                shake(findViewById(R.id.cardInput)); // UI
                Toast.makeText(this, "Add at least one experience", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ExperienceActivity.this, QuestionsActivity.class);

            // Pass all previous data
            Intent prevIntent = getIntent();
            intent.putExtra("name", prevIntent.getStringExtra("name"));
            intent.putStringArrayListExtra("education", prevIntent.getStringArrayListExtra("education"));
            intent.putStringArrayListExtra("skills", prevIntent.getStringArrayListExtra("skills"));
            intent.putStringArrayListExtra("interests", prevIntent.getStringArrayListExtra("interests"));
            intent.putStringArrayListExtra("experience", experienceList);

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // UI
        });

        // UI: animations (run after the original setup)
        updateEmptyState();
        playEntranceAnimation();
        addPressFeedback(buttonNext);
        addPressFeedback(buttonAddExperience);
    }

    // ---------------- UI helpers (no effect on app logic) ----------------

    private void updateEmptyState() {
        boolean isEmpty = experienceList.isEmpty();
        findViewById(R.id.tvEmptyState).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewExperience.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
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

        slideUp(findViewById(R.id.cardInput), 350L, 120f);
        slideUp(findViewById(R.id.tvEmptyState), 550L, 60f);
        slideUp(buttonNext, 650L, 80f);

        // Pulsing empty-state hint
        ObjectAnimator pulse = ObjectAnimator.ofFloat(findViewById(R.id.tvEmptyState), "alpha", 1f, 0.45f);
        pulse.setStartDelay(1400L);
        pulse.setDuration(1600L);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();

        // Slowly drifting header blob
        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobExperience), "translationY", 0f, 35f);
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

    private void shake(View v) {
        ObjectAnimator.ofFloat(v, "translationX", 0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f)
                .setDuration(420L).start();
    }

    /** Press-in/release scale. Returns false so the normal click still fires. */
    private void addPressFeedback(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(110L).start();
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