package com.aipath.careermitra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class QuestionsActivity extends AppCompatActivity {

    private SeekBar seekBar1, seekBar2, seekBar3, seekBar4, seekBar5;
    private Button buttonGetRecommendations;

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

            outputsRef.removeValue().addOnCompleteListener(deleteTask -> {
                if (!deleteTask.isSuccessful()) {
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
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to upload data", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }
}
