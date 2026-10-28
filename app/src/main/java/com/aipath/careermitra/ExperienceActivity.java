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
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String exp = role + " | " + duration + " | " + company;
            experienceList.add(exp);
            adapter.notifyItemInserted(experienceList.size() - 1);

            editTextRole.setText("");
            editTextDuration.setText("");
            editTextCompany.setText("");
        });

        buttonNext.setOnClickListener(v -> {
            if (experienceList.isEmpty()) {
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
        });
    }
}
