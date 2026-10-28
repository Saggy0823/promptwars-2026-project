package com.aipath.careermitra;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class UserDataActivity extends AppCompatActivity {

    private EditText editTextName, editTextEducation, editTextField, editTextDomain, editTextTopics;
    private Button buttonGenerateQuiz;

    private TextView[] levelChips;
    private TextView[] countChips;
    private String level = "Intermediate";
    private int numQuestions = 10;

    // UI: infinite animators, cancelled in onDestroy
    private final List<Animator> loopingAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_data);

        editTextName = findViewById(R.id.editTextName);
        editTextEducation = findViewById(R.id.editTextEducation);
        editTextField = findViewById(R.id.editTextField);
        editTextDomain = findViewById(R.id.editTextDomain);
        editTextTopics = findViewById(R.id.editTextTopics);
        buttonGenerateQuiz = findViewById(R.id.buttonGenerateQuiz);

        levelChips = new TextView[]{
                findViewById(R.id.chipBeginner),
                findViewById(R.id.chipIntermediate),
                findViewById(R.id.chipAdvanced)
        };
        countChips = new TextView[]{
                findViewById(R.id.chipQ5),
                findViewById(R.id.chipQ10),
                findViewById(R.id.chipQ15)
        };

        setupChips();

        // "Done" on the keyboard validates focus only; it never submits by itself
        editTextTopics.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        buttonGenerateQuiz.setOnClickListener(v -> submit());

        playEntranceAnimation();
        addPressFeedback(buttonGenerateQuiz);
    }

    // ---------------- CHIPS ----------------

    private void setupChips() {
        for (TextView chip : levelChips) {
            chip.setOnClickListener(v -> {
                selectChip(levelChips, (TextView) v);
                level = ((TextView) v).getText().toString();
                hideKeyboard();
            });
        }
        for (TextView chip : countChips) {
            chip.setOnClickListener(v -> {
                selectChip(countChips, (TextView) v);
                numQuestions = Integer.parseInt(((TextView) v).getText().toString());
                hideKeyboard();
            });
        }
        selectChip(levelChips, levelChips[1]); // Intermediate
        selectChip(countChips, countChips[1]); // 10
    }

    private void selectChip(TextView[] group, TextView selected) {
        for (TextView c : group) {
            c.setSelected(c == selected);
        }
        // UI: small bounce
        selected.animate().cancel();
        selected.setScaleX(1.12f);
        selected.setScaleY(1.12f);
        selected.animate().scaleX(1f).scaleY(1f).setDuration(160L).start();
    }

    // ---------------- SUBMIT ----------------

    private void submit() {
        String name = editTextName.getText().toString().trim();
        String education = editTextEducation.getText().toString().trim();
        String field = editTextField.getText().toString().trim();
        String domain = editTextDomain.getText().toString().trim();
        String topics = editTextTopics.getText().toString().trim();

        if (name.isEmpty() || education.isEmpty() || domain.isEmpty()) {
            if (name.isEmpty()) shake(editTextName);
            if (education.isEmpty()) shake(editTextEducation);
            if (domain.isEmpty()) shake(editTextDomain);
            Toast.makeText(this, "Please fill name, education and domain", Toast.LENGTH_SHORT).show();
            return;
        }

        String userKey = makeUserKey(name);
        if (userKey.isEmpty()) {
            shake(editTextName);
            Toast.makeText(this, "Please enter a valid name", Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        setLoading(true);

        final String requestId = UUID.randomUUID().toString();
        final int count = numQuestions;
        final String chosenLevel = level;

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference inputsRef = db.getReference("gap_inputs").child(userKey);

        // Clear any old quiz, answers and report for this user first
        Task<Void> t1 = db.getReference("gap_quizzes").child(userKey).removeValue();
        Task<Void> t2 = db.getReference("gap_reports").child(userKey).removeValue();
        Task<Void> t3 = db.getReference("gap_answers").child(userKey).removeValue();

        Tasks.whenAll(t1, t2, t3).addOnCompleteListener(deleteTask -> {
            if (!deleteTask.isSuccessful()) {
                setLoading(false);
                Toast.makeText(this, "Failed to clear old data", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, Object> input = new HashMap<>();
            input.put("requestId", requestId);
            input.put("name", name);
            input.put("education", education);
            input.put("field", field);
            input.put("domain", domain);
            input.put("topics", topics);
            input.put("level", chosenLevel);
            input.put("numQuestions", count);
            input.put("updatedAt", System.currentTimeMillis());

            inputsRef.setValue(input).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Building your quiz…", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(UserDataActivity.this, QuizActivity.class);
                    intent.putExtra("username", userKey);
                    intent.putExtra("requestId", requestId);
                    intent.putExtra("domain", domain);
                    intent.putExtra("name", name);
                    intent.putExtra("numQuestions", count);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // UI
                    finish();
                } else {
                    setLoading(false);
                    Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /** Same rule the Python backend expects: lowercase, and . # $ [ ] / become "_". */
    private String makeUserKey(String name) {
        return name.trim().toLowerCase().replaceAll("[.#$\\[\\]/]", "_");
    }

    private void setLoading(boolean loading) {
        buttonGenerateQuiz.setEnabled(!loading);
        buttonGenerateQuiz.setText(loading ? "Sending…" : "Generate My Quiz  🧠");
    }

    // ---------------- UI helpers ----------------

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

        slideUp(findViewById(R.id.cardUserData), 350L, 120f);

        View[] fields = new View[]{
                editTextName, editTextEducation, editTextField, editTextDomain, editTextTopics,
                findViewById(R.id.rowLevel), findViewById(R.id.rowCount)
        };
        for (int i = 0; i < fields.length; i++) {
            View f = fields[i];
            f.setAlpha(0f);
            f.setTranslationY(30f);
            f.animate().alpha(1f).translationY(0f)
                    .setStartDelay(600L + i * 90L)
                    .setDuration(450L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        }

        slideUp(buttonGenerateQuiz, 1200L, 80f);
        slideUp(findViewById(R.id.tvHint), 1300L, 40f);

        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobUserData), "translationY", 0f, 35f);
        drift.setDuration(6000L);
        drift.setRepeatCount(ObjectAnimator.INFINITE);
        drift.setRepeatMode(ObjectAnimator.REVERSE);
        drift.setInterpolator(new AccelerateDecelerateInterpolator());
        drift.start();
        loopingAnimators.add(drift);
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

    @Override
    protected void onDestroy() {
        for (Animator a : loopingAnimators) a.cancel();
        loopingAnimators.clear();
        super.onDestroy();
    }
}