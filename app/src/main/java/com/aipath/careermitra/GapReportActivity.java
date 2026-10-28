package com.aipath.careermitra;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GapReportActivity extends AppCompatActivity {

    private static final long TIMEOUT_MS = 4 * 60 * 1000L;

    private String userKey, requestId, domain, name;

    private View layoutLoading, scrollReport, cardHero, layoutActions;
    private TextView tvLoading, tvReportSubtitle, tvLevelChip, tvSummary;
    private GaugeView gaugeView;
    private LinearLayout sectionsContainer;

    private DatabaseReference reportRef;
    private ValueEventListener reportListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean reportShown = false;

    // Kept for the Share button
    private int scorePct = 0, gapPct = 0;
    private String weakAreasText = "";

    // UI: infinite animators, cancelled in onDestroy
    private final List<Animator> loopingAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gap_report);

        userKey = getIntent().getStringExtra("username");
        requestId = getIntent().getStringExtra("requestId");
        domain = getIntent().getStringExtra("domain");
        name = getIntent().getStringExtra("name");
        if (domain == null) domain = "";
        if (name == null) name = "";

        if (userKey == null || userKey.isEmpty() || requestId == null || requestId.isEmpty()) {
            Toast.makeText(this, "Report request missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        layoutLoading = findViewById(R.id.layoutLoading);
        scrollReport = findViewById(R.id.scrollReport);
        cardHero = findViewById(R.id.cardHero);
        layoutActions = findViewById(R.id.layoutActions);
        tvLoading = findViewById(R.id.tvLoading);
        tvReportSubtitle = findViewById(R.id.tvReportSubtitle);
        tvLevelChip = findViewById(R.id.tvLevelChip);
        tvSummary = findViewById(R.id.tvSummary);
        gaugeView = findViewById(R.id.gaugeView);
        sectionsContainer = findViewById(R.id.sectionsContainer);

        findViewById(R.id.buttonAnother).setOnClickListener(v -> {
            startActivity(new Intent(this, UserDataActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.buttonHome).setOnClickListener(v -> {
            Intent i = new Intent(this, WelcomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
        findViewById(R.id.buttonShare).setOnClickListener(v -> shareReport());
        addPressFeedback(findViewById(R.id.buttonAnother));
        addPressFeedback(findViewById(R.id.buttonShare));
        addPressFeedback(findViewById(R.id.buttonHome));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(GapReportActivity.this, WelcomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            }
        });

        ObjectAnimator pulse = ObjectAnimator.ofFloat(tvLoading, "alpha", 1f, 0.4f);
        pulse.setDuration(1100L);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();
        loopingAnimators.add(pulse);

        startTimeout();
        startListening();
    }

    // =====================================================================
    // PHASE 1: WAITING FOR THE REPORT
    // =====================================================================

    private void startListening() {
        reportRef = FirebaseDatabase.getInstance().getReference("gap_reports").child(userKey);

        reportListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || reportShown) return;

                String rid = snapshot.child("requestId").getValue(String.class);
                String json = snapshot.child("report").getValue(String.class);

                if (!requestId.equals(rid)) return;
                if (json == null || json.trim().isEmpty()) return;

                stopListening();
                try {
                    renderReport(json);
                } catch (JSONException e) {
                    Toast.makeText(GapReportActivity.this,
                            "The report data was invalid. Please take the quiz again.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GapReportActivity.this,
                        "Database Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        reportRef.addValueEventListener(reportListener);
    }

    private void stopListening() {
        if (reportRef != null && reportListener != null) {
            reportRef.removeEventListener(reportListener);
        }
        handler.removeCallbacks(timeoutRunnable);
    }

    private void startTimeout() {
        timeoutRunnable = () -> {
            if (isFinishing() || reportShown) return;
            new AlertDialog.Builder(this)
                    .setTitle("Taking longer than expected")
                    .setMessage("The AI is still writing your report. Keep waiting?")
                    .setCancelable(false)
                    .setPositiveButton("Keep waiting", (d, w) -> handler.postDelayed(timeoutRunnable, TIMEOUT_MS))
                    .setNegativeButton("Go back", (d, w) -> finish())
                    .show();
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS);
    }

    // =====================================================================
    // PHASE 2: RENDER THE REPORT
    // =====================================================================

    private void renderReport(String json) throws JSONException {
        JSONObject r = new JSONObject(json);

        scorePct = r.optInt("overallScorePercent", 0);
        gapPct = r.optInt("knowledgeGapPercent", 100 - scorePct);
        String level = r.optString("level", "");
        String summary = r.optString("summary", "");

        tvReportSubtitle.setText(name.isEmpty() ? domain : domain + " • " + name);
        if (level.isEmpty()) tvLevelChip.setVisibility(View.GONE);
        else tvLevelChip.setText("Level: " + level);
        tvSummary.setText(summary);
        if (summary.isEmpty()) tvSummary.setVisibility(View.GONE);

        sectionsContainer.removeAllViews();
        buildTopics(r.optJSONArray("topicBreakdown"));
        buildBullets("💪  Strengths", r.optJSONArray("strengths"), "#16A34A");
        buildBullets("🎯  Weak areas", r.optJSONArray("weakAreas"), "#EF4444");
        weakAreasText = joinArray(r.optJSONArray("weakAreas"));
        buildMistakes(r.optJSONArray("mistakes"));
        buildStudyPlan(r.optJSONArray("studyPlan"));
        buildResources(r.optJSONArray("resourceTypes"));
        buildMotivation(r.optString("motivation", ""));

        reportShown = true;
        layoutLoading.setVisibility(View.GONE);
        scrollReport.setVisibility(View.VISIBLE);
        playReportAnimation();
    }

    private LinearLayout addSection(String title) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_section_card, sectionsContainer, false);
        ((TextView) card.findViewById(R.id.tvSectionTitle)).setText(title);
        sectionsContainer.addView(card);
        return card.findViewById(R.id.sectionBody);
    }

    private void buildTopics(JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        LinearLayout body = addSection("📚  Topic breakdown");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            int pct = Math.max(0, Math.min(100, o.optInt("scorePercent", 0)));
            View row = inflater.inflate(R.layout.item_topic_row, body, false);
            ((TextView) row.findViewById(R.id.tvTopicName)).setText(o.optString("topic", ""));
            ((TextView) row.findViewById(R.id.tvTopicScore)).setText(
                    o.optInt("correct", 0) + "/" + o.optInt("total", 0) + "  •  " + pct + "%");

            View fill = row.findViewById(R.id.viewBarFill);
            fill.setBackgroundTintList(ColorStateList.valueOf(statusColor(o.optString("status", ""), pct)));
            fill.setPivotX(0f);
            fill.setScaleX(0f);
            fill.animate().scaleX(pct / 100f)
                    .setStartDelay(1700L + Math.min(i, 8) * 100L)
                    .setDuration(700L)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
            body.addView(row);
        }
    }

    private int statusColor(String status, int pct) {
        switch (status) {
            case "Strong": return Color.parseColor("#16A34A");
            case "Moderate": return Color.parseColor("#F59E0B");
            case "Weak": return Color.parseColor("#EF4444");
        }
        if (pct >= 75) return Color.parseColor("#16A34A");
        if (pct >= 40) return Color.parseColor("#F59E0B");
        return Color.parseColor("#EF4444");
    }

    private void buildBullets(String title, JSONArray arr, String dotColor) {
        if (arr == null || arr.length() == 0) return;
        LinearLayout body = addSection(title);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < arr.length(); i++) {
            String text = arr.optString(i, "").trim();
            if (text.isEmpty()) continue;
            View row = inflater.inflate(R.layout.item_bullet, body, false);
            TextView dot = row.findViewById(R.id.tvBulletDot);
            dot.setTextColor(Color.parseColor(dotColor));
            ((TextView) row.findViewById(R.id.tvBulletText)).setText(text);
            body.addView(row);
        }
    }

    private void buildMistakes(JSONArray arr) {
        if (arr == null || arr.length() == 0) {
            LinearLayout body = addSection("🎉  Perfect score");
            TextView t = new TextView(this);
            t.setText("You answered every question correctly. No mistakes to review!");
            t.setTextSize(14);
            t.setTextColor(Color.parseColor("#3A3A3C"));
            body.addView(t);
            return;
        }

        final LinearLayout body = addSection("🔍  Review your mistakes");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            View row = inflater.inflate(R.layout.item_mistake, body, false);
            ((TextView) row.findViewById(R.id.tvMistakeQuestion)).setText(o.optString("question", ""));
            ((TextView) row.findViewById(R.id.tvYourAnswer)).setText("Your answer: " + o.optString("yourAnswer", ""));
            ((TextView) row.findViewById(R.id.tvCorrectAnswer)).setText("Correct answer: " + o.optString("correctAnswer", ""));
            ((TextView) row.findViewById(R.id.tvExplanation)).setText(o.optString("explanation", ""));

            final View details = row.findViewById(R.id.mistakeDetails);
            final TextView arrow = row.findViewById(R.id.tvMistakeArrow);
            row.setOnClickListener(v -> {
                TransitionManager.beginDelayedTransition(body);
                boolean open = details.getVisibility() == View.VISIBLE;
                details.setVisibility(open ? View.GONE : View.VISIBLE);
                arrow.animate().rotation(open ? 0f : 180f).setDuration(200L).start();
            });
            body.addView(row);
        }
    }

    private void buildStudyPlan(JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        LinearLayout body = addSection("🗓  Your study plan");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            View card = inflater.inflate(R.layout.item_week, body, false);
            ((TextView) card.findViewById(R.id.tvWeekLabel)).setText(o.optString("week", "Week " + (i + 1)));
            ((TextView) card.findViewById(R.id.tvWeekFocus)).setText(o.optString("focus", ""));

            LinearLayout actions = card.findViewById(R.id.weekActions);
            JSONArray acts = o.optJSONArray("actions");
            if (acts != null) {
                for (int k = 0; k < acts.length(); k++) {
                    String a = acts.optString(k, "").trim();
                    if (a.isEmpty()) continue;
                    View bullet = inflater.inflate(R.layout.item_bullet, actions, false);
                    ((TextView) bullet.findViewById(R.id.tvBulletDot)).setTextColor(Color.parseColor("#4F46E5"));
                    ((TextView) bullet.findViewById(R.id.tvBulletText)).setText(a);
                    actions.addView(bullet);
                }
            }
            body.addView(card);
        }
    }

    private void buildResources(JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        LinearLayout body = addSection("📖  Where to learn");

        ChipGroup group = new ChipGroup(this);
        group.setChipSpacingHorizontal(dp(8));
        group.setChipSpacingVertical(dp(8));

        for (int i = 0; i < arr.length(); i++) {
            String text = arr.optString(i, "").trim();
            if (text.isEmpty()) continue;
            TextView chip = new TextView(this);
            chip.setText(text);
            chip.setTextSize(13);
            chip.setTextColor(Color.parseColor("#4F46E5"));
            chip.setBackgroundResource(R.drawable.bg_value_pill);
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            group.addView(chip);
        }
        body.addView(group, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void buildMotivation(String text) {
        if (text == null || text.trim().isEmpty()) return;
        LinearLayout body = addSection("💬  Keep going");
        TextView t = new TextView(this);
        t.setText(text.trim());
        t.setTextSize(14);
        t.setTextColor(Color.parseColor("#3A3A3C"));
        t.setLineSpacing(dp(3), 1f);
        body.addView(t);
    }

    private String joinArray(JSONArray arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            String s = arr.optString(i, "").trim();
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(s);
        }
        return sb.toString();
    }

    private void shareReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("My CareerMitra knowledge gap report\n");
        if (!domain.isEmpty()) sb.append("Domain: ").append(domain).append("\n");
        sb.append("Score: ").append(scorePct).append("%\n");
        sb.append("Knowledge gap: ").append(gapPct).append("%\n");
        if (!weakAreasText.isEmpty()) sb.append("Weak areas: ").append(weakAreasText).append("\n");

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "Share report"));
    }

    // =====================================================================
    // UI ANIMATIONS
    // =====================================================================

    private void playReportAnimation() {
        View[] header = new View[]{
                findViewById(R.id.tvBrand),
                findViewById(R.id.tvReportTitle),
                tvReportSubtitle
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

        slideUp(cardHero, 350L, 120f);

        // Gauge starts once the hero card is visible
        gaugeView.postDelayed(() -> gaugeView.setValues(gapPct, scorePct), 700L);

        // Sections appear one after another after the gauge (stagger capped at 8)
        int count = sectionsContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            slideUp(sectionsContainer.getChildAt(i), 1500L + Math.min(i, 8) * 120L, 50f);
        }
        slideUp(layoutActions, 1500L + Math.min(count, 8) * 120L, 50f);

        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobReport), "translationY", 0f, 35f);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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