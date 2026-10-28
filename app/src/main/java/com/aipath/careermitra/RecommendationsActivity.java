package com.aipath.careermitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
            finish();
        });
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

                // Stop listening after success
                dbRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
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
    }
}
