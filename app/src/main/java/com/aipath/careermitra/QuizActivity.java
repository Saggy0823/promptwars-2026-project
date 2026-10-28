package com.aipath.careermitra;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final long TIMEOUT_MS = 4 * 60 * 1000L;
    private static final int PHASE_WAITING = 0, PHASE_QUIZ = 1, PHASE_ERROR = 2;

    // ---- extras ----
    private String userKey, requestId, domain, name;

    // ---- views ----
    private View layoutLoading, layoutError, layoutQuiz, cardQuestion, barBottom, headerQuiz;
    private View viewProgressFill;
    private TextView tvLoading, tvErrorMessage, tvQuizDomain, tvQuestionCount;
    private TextView tvTopic, tvDifficulty, tvQuestionText;
    private android.widget.LinearLayout optionsContainer;
    private ScrollView scrollQuiz;
    private Button buttonPrev, buttonSkip, buttonNext, buttonErrorRetry;

    // ---- state ----
    private int phase = PHASE_WAITING;
    private final List<Question> questions = new ArrayList<>();
    private int[] selected;
    private long[] timeMs;
    private int current = 0;
    private long questionStartMs;
    private boolean animating = false;
    private boolean submitting = false;
    private String quizError = "";

    private final View[] optionViews = new View[4];
    private final TextView[] optionLetters = new TextView[4];

    private DatabaseReference quizRef;
    private ValueEventListener quizListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    // UI: infinite animators, cancelled in onDestroy
    private final List<Animator> loopingAnimators = new ArrayList<>();

    private static class Question {
        int id;
        String text;
        String topic;
        String difficulty;
        String[] options = new String[4];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        userKey = getIntent().getStringExtra("username");
        requestId = getIntent().getStringExtra("requestId");
        domain = getIntent().getStringExtra("domain");
        name = getIntent().getStringExtra("name");
        if (domain == null) domain = "your topic";
        if (name == null) name = "";

        if (userKey == null || userKey.isEmpty() || requestId == null || requestId.isEmpty()) {
            Toast.makeText(this, "Quiz request missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupBackPress();

        tvLoading.setText("🧠  Building your " + domain + " quiz…");
        playLoadingAnimation();

        buttonErrorRetry.setOnClickListener(v -> {
            startActivity(new Intent(this, UserDataActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        buttonPrev.setOnClickListener(v -> {
            if (current > 0) goTo(current - 1);
        });
        buttonSkip.setOnClickListener(v -> {
            selected[current] = -1;
            onNext();
        });
        buttonNext.setOnClickListener(v -> onNext());
        addPressFeedback(buttonNext);
        addPressFeedback(buttonPrev);

        startTimeout();
        startListening();
    }

    private void bindViews() {
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutError = findViewById(R.id.layoutError);
        layoutQuiz = findViewById(R.id.layoutQuiz);
        cardQuestion = findViewById(R.id.cardQuestion);
        barBottom = findViewById(R.id.barBottom);
        headerQuiz = findViewById(R.id.headerQuiz);
        viewProgressFill = findViewById(R.id.viewProgressFill);
        tvLoading = findViewById(R.id.tvLoading);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvQuizDomain = findViewById(R.id.tvQuizDomain);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvTopic = findViewById(R.id.tvTopic);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        optionsContainer = findViewById(R.id.optionsContainer);
        scrollQuiz = findViewById(R.id.scrollQuiz);
        buttonPrev = findViewById(R.id.buttonPrev);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonNext = findViewById(R.id.buttonNext);
        buttonErrorRetry = findViewById(R.id.buttonErrorRetry);
    }

    // =====================================================================
    // PHASE 1: WAITING FOR THE QUIZ
    // =====================================================================

    private void startListening() {
        quizRef = FirebaseDatabase.getInstance().getReference("gap_quizzes").child(userKey);

        quizListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String rid = snapshot.child("requestId").getValue(String.class);
                String quizJson = snapshot.child("quiz").getValue(String.class);

                // Only accept the quiz that belongs to THIS submission
                if (!requestId.equals(rid)) return;
                if (quizJson == null || quizJson.trim().isEmpty()) return;

                stopListening();
                onQuizReceived(quizJson);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Database error: " + error.getMessage());
            }
        };
        quizRef.addValueEventListener(quizListener);
    }

    private void stopListening() {
        if (quizRef != null && quizListener != null) {
            quizRef.removeEventListener(quizListener);
        }
        handler.removeCallbacks(timeoutRunnable);
    }

    private void startTimeout() {
        timeoutRunnable = () -> {
            if (isFinishing() || phase != PHASE_WAITING) return;
            new AlertDialog.Builder(this)
                    .setTitle("Taking longer than expected")
                    .setMessage("The AI is still working on your quiz. Do you want to keep waiting?")
                    .setCancelable(false)
                    .setPositiveButton("Keep waiting", (d, w) -> handler.postDelayed(timeoutRunnable, TIMEOUT_MS))
                    .setNegativeButton("Go back", (d, w) -> finish())
                    .show();
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS);
    }

    private void onQuizReceived(String quizJson) {
        try {
            List<Question> parsed = parseQuiz(quizJson);
            if (parsed.isEmpty()) {
                showError(quizError.isEmpty()
                        ? "The quiz could not be created. Please try again."
                        : quizError);
                return;
            }
            questions.clear();
            questions.addAll(parsed);
            selected = new int[questions.size()];
            timeMs = new long[questions.size()];
            for (int i = 0; i < selected.length; i++) selected[i] = -1;
            showQuiz();
        } catch (JSONException e) {
            showError("The quiz data was invalid. Please try again.");
        }
    }

    /** Parses and validates the quiz JSON from the server. */
    private List<Question> parseQuiz(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        quizError = root.optString("error", "");
        String quizDomain = root.optString("domain", "");
        if (!quizDomain.isEmpty()) domain = quizDomain;

        List<Question> list = new ArrayList<>();
        JSONArray arr = root.optJSONArray("questions");
        if (arr == null) return list;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String text = o.optString("question", "").trim();
            JSONArray opts = o.optJSONArray("options");
            if (text.isEmpty() || opts == null || opts.length() != 4) continue;

            int correct = o.optInt("correctIndex", -1);
            if (correct < 0 || correct > 3) continue;

            Question q = new Question();
            q.id = o.optInt("id", i + 1);
            q.text = text;
            q.topic = o.optString("topic", domain);
            q.difficulty = o.optString("difficulty", "medium").toLowerCase();
            boolean ok = true;
            for (int k = 0; k < 4; k++) {
                q.options[k] = opts.optString(k, "").trim();
                if (q.options[k].isEmpty()) ok = false;
            }
            if (ok) list.add(q);
        }
        return list;
    }

    private void showError(String message) {
        phase = PHASE_ERROR;
        stopListening();
        layoutLoading.setVisibility(View.GONE);
        layoutQuiz.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
        layoutError.setVisibility(View.VISIBLE);
    }

    // =====================================================================
    // PHASE 2: THE QUIZ
    // =====================================================================

    private void showQuiz() {
        phase = PHASE_QUIZ;
        tvQuizDomain.setText(domain);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        layoutQuiz.setVisibility(View.VISIBLE);

        bindQuestion();
        playQuizEntrance();
    }

    private void bindQuestion() {
        Question q = questions.get(current);
        int total = questions.size();

        tvQuestionCount.setText("Question " + (current + 1) + " of " + total);
        tvTopic.setText(q.topic);
        styleDifficulty(q.difficulty);
        tvQuestionText.setText(q.text);

        optionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            View row = inflater.inflate(R.layout.item_quiz_option, optionsContainer, false);
            TextView letter = row.findViewById(R.id.tvLetter);
            TextView text = row.findViewById(R.id.tvOptionText);
            letter.setText(letters[i]);
            text.setText(q.options[i]);
            final int idx = i;
            row.setOnClickListener(v -> selectOption(idx));
            optionsContainer.addView(row);
            optionViews[i] = row;
            optionLetters[i] = letter;
        }
        refreshOptions();

        buttonPrev.setVisibility(current == 0 ? View.INVISIBLE : View.VISIBLE);
        buttonNext.setText(current == total - 1 ? "Submit Quiz ✅" : "Next →");
        animateProgress();
        scrollQuiz.scrollTo(0, 0);
        questionStartMs = SystemClock.elapsedRealtime();
    }

    private void styleDifficulty(String difficulty) {
        int bg, fg;
        String label;
        switch (difficulty) {
            case "easy":
                bg = Color.parseColor("#DCFCE7"); fg = Color.parseColor("#15803D"); label = "Easy";
                break;
            case "hard":
                bg = Color.parseColor("#FEE2E2"); fg = Color.parseColor("#B91C1C"); label = "Hard";
                break;
            default:
                bg = Color.parseColor("#FEF3C7"); fg = Color.parseColor("#B45309"); label = "Medium";
        }
        tvDifficulty.setText(label);
        tvDifficulty.setTextColor(fg);
        tvDifficulty.setBackgroundTintList(ColorStateList.valueOf(bg));
    }

    private void selectOption(int idx) {
        if (animating || submitting) return;
        selected[current] = idx;
        refreshOptions();

        optionViews[idx].performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        View v = optionViews[idx];
        v.animate().cancel();
        v.setScaleX(0.97f);
        v.setScaleY(0.97f);
        v.animate().scaleX(1f).scaleY(1f).setDuration(160L).start();
    }

    private void refreshOptions() {
        for (int i = 0; i < 4; i++) {
            boolean on = selected[current] == i;
            optionViews[i].setSelected(on);
            optionLetters[i].setSelected(on);
        }
    }

    private void animateProgress() {
        float target = (current + 1) / (float) questions.size();
        viewProgressFill.setPivotX(0f);
        viewProgressFill.animate().scaleX(target).setDuration(400L)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void recordTime() {
        long now = SystemClock.elapsedRealtime();
        timeMs[current] += now - questionStartMs;
        questionStartMs = now;
    }

    private void onNext() {
        if (animating || submitting) return;
        if (current < questions.size() - 1) {
            goTo(current + 1);
        } else {
            confirmSubmit();
        }
    }

    /** Slides the question card out and the next one in. Header and bottom bar stay still. */
    private void goTo(final int newIndex) {
        if (animating) return;
        recordTime();
        animating = true;

        final int dir = newIndex > current ? 1 : -1;
        final float shift = cardQuestion.getWidth() * 0.3f;

        cardQuestion.animate().translationX(-dir * shift).alpha(0f).setDuration(170L)
                .withEndAction(() -> {
                    current = newIndex;
                    bindQuestion();
                    cardQuestion.setTranslationX(dir * shift);
                    cardQuestion.animate().translationX(0f).alpha(1f).setDuration(220L)
                            .setInterpolator(new DecelerateInterpolator(1.6f))
                            .withEndAction(() -> animating = false)
                            .start();
                }).start();
    }

    // =====================================================================
    // SUBMIT
    // =====================================================================

    private void confirmSubmit() {
        recordTime();
        int unanswered = 0;
        for (int s : selected) if (s < 0) unanswered++;

        if (unanswered > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Submit quiz?")
                    .setMessage("You skipped " + unanswered + (unanswered == 1 ? " question" : " questions")
                            + ". Submit anyway?")
                    .setPositiveButton("Submit", (d, w) -> submitAnswers())
                    .setNegativeButton("Review", null)
                    .show();
        } else {
            submitAnswers();
        }
    }

    private void submitAnswers() {
        if (submitting) return;
        setSubmitting(true);

        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < questions.size(); i++) {
                JSONObject o = new JSONObject();
                o.put("id", questions.get(i).id);
                o.put("selectedIndex", selected[i]);
                o.put("timeSpentSec", Math.round(timeMs[i] / 1000f));
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("answers", arr);

            HashMap<String, Object> payload = new HashMap<>();
            payload.put("requestId", requestId);
            payload.put("answers", root.toString());
            payload.put("updatedAt", System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference("gap_answers").child(userKey)
                    .setValue(payload)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(QuizActivity.this, GapReportActivity.class);
                            intent.putExtra("username", userKey);
                            intent.putExtra("requestId", requestId);
                            intent.putExtra("domain", domain);
                            intent.putExtra("name", name);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        } else {
                            setSubmitting(false);
                            Toast.makeText(this, "Failed to submit answers", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (JSONException e) {
            setSubmitting(false);
            Toast.makeText(this, "Failed to submit answers", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        buttonNext.setEnabled(!value);
        buttonPrev.setEnabled(!value);
        buttonSkip.setEnabled(!value);
        buttonNext.setText(value ? "Submitting…"
                : (current == questions.size() - 1 ? "Submit Quiz ✅" : "Next →"));
    }

    // =====================================================================
    // BACK PRESS
    // =====================================================================

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (phase == PHASE_QUIZ && !submitting) {
                    new AlertDialog.Builder(QuizActivity.this)
                            .setTitle("Leave the quiz?")
                            .setMessage("Your progress will be lost.")
                            .setPositiveButton("Leave", (d, w) -> finish())
                            .setNegativeButton("Stay", null)
                            .show();
                } else if (!submitting) {
                    finish();
                }
            }
        });
    }

    // =====================================================================
    // UI ANIMATIONS
    // =====================================================================

    private void playLoadingAnimation() {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(tvLoading, "alpha", 1f, 0.4f);
        pulse.setDuration(1100L);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();
        loopingAnimators.add(pulse);
    }

    private void playQuizEntrance() {
        headerQuiz.setAlpha(0f);
        headerQuiz.animate().alpha(1f).setDuration(400L).start();

        slideUp(cardQuestion, 150L, 100f);
        slideUp(barBottom, 300L, 80f);

        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobQuiz), "translationY", 0f, 30f);
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
                .setStartDelay(delay).setDuration(600L)
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

    @Override
    protected void onDestroy() {
        stopListening();
        handler.removeCallbacksAndMessages(null);
        for (Animator a : loopingAnimators) a.cancel();
        loopingAnimators.clear();
        super.onDestroy();
    }
}