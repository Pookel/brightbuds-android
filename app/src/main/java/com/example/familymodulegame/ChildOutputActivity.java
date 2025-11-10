package com.example.familymodulegame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildOutputActivity extends AppCompatActivity {

    private ImageView childImage;
    private TextView childName;
    private Button startGameBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_output);

        // Initialize views
        childImage = findViewById(R.id.childImage);
        childName = findViewById(R.id.childName);
        startGameBtn = findViewById(R.id.startGameBtn);

        // Set title
        childName.setText("Family Tree");

        // Set main image
        childImage.setImageResource(R.drawable.familymodule_icon);

        // Click listener for Start Game button
        startGameBtn.setOnClickListener(v -> {
            Toast.makeText(ChildOutputActivity.this, "Starting game...", Toast.LENGTH_SHORT).show();

            // Launch GameActivity
            Intent gameIntent = new Intent(ChildOutputActivity.this, GameActivity.class);
            startActivity(gameIntent);
        });
    }
}



