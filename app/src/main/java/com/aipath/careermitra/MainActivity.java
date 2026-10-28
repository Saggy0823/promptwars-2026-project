package com.aipath.careermitra;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private EditText inputEditText;
    private Button saveButton, loadButton;
    private TextView displayTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputEditText = findViewById(R.id.inputEditText);
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);
        displayTextView = findViewById(R.id.displayTextView);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputEditText.getText().toString();
                saveToFile(text);
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String savedText = readFromFile();
                displayTextView.setText(savedText);
            }
        });
    }

    private void saveToFile(String text) {
        File file = new File(getFilesDir(), "input.txt");
        try (FileWriter writer = new FileWriter(file, true)) { // append mode
            writer.write(text + "\n");
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            inputEditText.setText(""); // clear input
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }

    private String readFromFile() {
        File file = new File(getFilesDir(), "input.txt");
        StringBuilder text = new StringBuilder();
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error reading file";
            }
        } else {
            return "File not found";
        }
        return text.toString();
    }
}
