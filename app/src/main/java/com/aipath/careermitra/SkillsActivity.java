package com.aipath.careermitra;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SkillsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewSkills;
    private EditText editTextNewSkill;
    private Button buttonAddSkill, buttonNext;

    private SkillsAdapter adapter;
    private ArrayList<String> skillsList;
    private ArrayList<String> preSelectedSkills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skills);

        recyclerViewSkills = findViewById(R.id.recyclerViewSkills);
        editTextNewSkill = findViewById(R.id.editTextNewSkill);
        buttonAddSkill = findViewById(R.id.buttonAddSkill);
        buttonNext = findViewById(R.id.buttonNext);

        // Default skills
        preSelectedSkills = new ArrayList<>();
        preSelectedSkills.add("Communication");
        preSelectedSkills.add("Group Work");

        // Initial skills list (can add more)
        skillsList = new ArrayList<>();
        skillsList.add("Communication");
        skillsList.add("Group Work");
        skillsList.add("Problem Solving");
        skillsList.add("Leadership");
        skillsList.add("Time Management");

        adapter = new SkillsAdapter(skillsList, preSelectedSkills);
        recyclerViewSkills.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSkills.setAdapter(adapter);

        buttonAddSkill.setOnClickListener(v -> {
            String newSkill = editTextNewSkill.getText().toString().trim();
            if (!newSkill.isEmpty() && !skillsList.contains(newSkill)) {
                skillsList.add(newSkill);
                adapter.notifyItemInserted(skillsList.size() - 1);
                editTextNewSkill.setText("");
                recyclerViewSkills.smoothScrollToPosition(skillsList.size() - 1); // UI
            } else {
                shake(editTextNewSkill); // UI: empty or duplicate skill
            }
        });

        buttonNext.setOnClickListener(v -> {
            ArrayList<String> selectedSkills = adapter.getSelectedSkills();
            if (selectedSkills.isEmpty()) {
                shake(recyclerViewSkills); // UI
                Toast.makeText(this, "Select at least one skill", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass data to InterestsActivity
            Intent intent = new Intent(SkillsActivity.this, InterestsActivity.class);
            intent.putStringArrayListExtra("skills", selectedSkills);

            // Pass previous name and education if available
            Intent prevIntent = getIntent();
            intent.putExtra("name", prevIntent.getStringExtra("name"));
            intent.putStringArrayListExtra("education", prevIntent.getStringArrayListExtra("education"));

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // UI
        });

        // UI: animations (run after the original setup)
        playEntranceAnimation();
        addPressFeedback(buttonNext);
        addPressFeedback(buttonAddSkill);
    }

    // ---------------- UI helpers (no effect on app logic) ----------------

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

        slideUp(findViewById(R.id.cardSkills), 350L, 120f);
        slideUp(findViewById(R.id.cardAddSkill), 500L, 100f);
        slideUp(buttonNext, 650L, 80f);

        // Slowly drifting header blob
        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobSkills), "translationY", 0f, 35f);
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