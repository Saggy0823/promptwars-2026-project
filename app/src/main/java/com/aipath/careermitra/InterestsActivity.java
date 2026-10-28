package com.aipath.careermitra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class InterestsActivity extends AppCompatActivity {
    private EditText editTextInterest;
    private Button buttonAddInterest, buttonNext;
    private RecyclerView recyclerViewInterests;
    private InterestsAdapter adapter;
    private ArrayList<String> interestsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        editTextInterest = findViewById(R.id.editTextInterest);
        buttonAddInterest = findViewById(R.id.buttonAddInterest);
        buttonNext = findViewById(R.id.buttonNext);
        recyclerViewInterests = findViewById(R.id.recyclerViewInterests);

        interestsList = new ArrayList<>();
        adapter = new InterestsAdapter(interestsList);
        recyclerViewInterests.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewInterests.setAdapter(adapter);

        buttonAddInterest.setOnClickListener(v -> {
            String interest = editTextInterest.getText().toString().trim();
            if (!interest.isEmpty()) {
                interestsList.add(interest);
                adapter.notifyItemInserted(interestsList.size() - 1);
                editTextInterest.setText("");
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (interestsList.isEmpty()) {
                Toast.makeText(this, "Add at least one interest", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(InterestsActivity.this, ExperienceActivity.class);

            // Pass previous data
            Intent prevIntent = getIntent();
            intent.putExtra("name", prevIntent.getStringExtra("name"));
            intent.putStringArrayListExtra("education", prevIntent.getStringArrayListExtra("education"));
            intent.putStringArrayListExtra("skills", prevIntent.getStringArrayListExtra("skills"));
            intent.putStringArrayListExtra("interests", interestsList);

            startActivity(intent);
        });
    }
}
