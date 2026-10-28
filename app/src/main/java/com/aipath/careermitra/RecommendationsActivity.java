package com.aipath.careermitra;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecommendationAdapter adapter;
    private List<Recommendation> recommendationList;
    private ProgressBar progressBar;
    private Button buttonRetry;

    private DatabaseReference dbRef;
    private ValueEventListener recommendationListener;

    private long screenOpenedAt;

    // UI: infinite animators, cancelled in onDestroy
    private final List<Animator> loopingAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        recyclerView = findViewById(R.id.recyclerViewRecommendations);
        progressBar = findViewById(R.id.progressBar);
        buttonRetry = findViewById(R.id.buttonRetry);

        recommendationList = new ArrayList<>();
        adapter = new RecommendationAdapter(recommendationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Username not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        screenOpenedAt = System.currentTimeMillis();

        dbRef = FirebaseDatabase.getInstance()
                .getReference("outputs")
                .child(username);

        startListening();

        buttonRetry.setOnClickListener(v -> {
            Intent intentRetry = new Intent(RecommendationsActivity.this, EducationActivity.class);
            intentRetry.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentRetry);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // UI
            finish();
        });

        // UI: animations (run after the original setup)
        playEntranceAnimation();
        addPressFeedback(buttonRetry);
    }

    private void startListening() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        buttonRetry.setVisibility(View.GONE);

        recommendationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                Long generatedAt = snapshot.child("generatedAt").getValue(Long.class);
                String output = snapshot.child("output").getValue(String.class);

                if (generatedAt == null || output == null || output.trim().isEmpty()) {
                    return;
                }

                // 🔥 CRITICAL FIX: seconds → milliseconds safety
                if (generatedAt < 1_000_000_000_000L) {
                    generatedAt = generatedAt * 1000;
                }

                if (generatedAt < screenOpenedAt) {
                    return;
                }

                recommendationList.clear();
                for (String rec : output.split("\n")) {
                    if (!rec.trim().isEmpty()) {
                        recommendationList.add(new Recommendation(rec.trim()));
                    }
                }

                adapter.notifyDataSetChanged();

                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                buttonRetry.setVisibility(View.VISIBLE);
                showResultsUi(); // UI

                // Stop listening after success
                dbRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                findViewById(R.id.tvLoading).setVisibility(View.GONE); // UI
                Toast.makeText(
                        RecommendationsActivity.this,
                        "Database Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        };

        dbRef.addValueEventListener(recommendationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && recommendationListener != null) {
            dbRef.removeEventListener(recommendationListener);
        }
        for (Animator a : loopingAnimators) { // UI
            a.cancel();
        }
        loopingAnimators.clear();
    }

    // ---------------- UI helpers (no effect on app logic) ----------------

    /** Hides the loading text and slides the Retry button in once results arrive. */
    private void showResultsUi() {
        View loading = findViewById(R.id.tvLoading);
        loading.animate().cancel();
        loading.setVisibility(View.GONE);

        buttonRetry.setAlpha(0f);
        buttonRetry.setTranslationY(80f);
        buttonRetry.animate().alpha(1f).translationY(0f)
                .setStartDelay(500L)
                .setDuration(600L)
                .setInterpolator(new DecelerateInterpolator(2.2f))
                .start();
    }

    private void playEntranceAnimation() {
        View[] header = new View[]{
                findViewById(R.id.tvBrand),
                findViewById(R.id.textHeader),
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

        // Pulsing "Analysing…" text while waiting
        ObjectAnimator pulse = ObjectAnimator.ofFloat(findViewById(R.id.tvLoading), "alpha", 1f, 0.35f);
        pulse.setDuration(1100L);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();
        loopingAnimators.add(pulse);

        // Slowly drifting header blob
        ObjectAnimator drift = ObjectAnimator.ofFloat(findViewById(R.id.blobRecommendations), "translationY", 0f, 35f);
        drift.setDuration(6000L);
        drift.setRepeatCount(ObjectAnimator.INFINITE);
        drift.setRepeatMode(ObjectAnimator.REVERSE);
        drift.setInterpolator(new AccelerateDecelerateInterpolator());
        drift.start();
        loopingAnimators.add(drift);
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