package com.example.familymodulegame;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

// declaration of variables
    private ImageView imageView;
    private TextView nameText;
    private ImageButton nextButton, prevButton;
    private MediaPlayer twinkleSound;

    private int currentIndex = 0;

    private int[] imageIds = {
            R.drawable.mother,
            R.drawable.father,
            R.drawable.brother
    };

    private String[] names = {
            "Mom", "Dad", "Brother"
    };

    private String[] relations = {
            "Mother", "Father", "Sibling"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        imageView = findViewById(R.id.imageView);
        nameText = findViewById(R.id.nameText);
        nextButton = findViewById(R.id.nextButton);
        prevButton = findViewById(R.id.prevButton);

        twinkleSound = MediaPlayer.create(this, R.raw.twinkle);

        updateContent();

        nextButton.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % imageIds.length;
            playTwinkle();
            updateContent();
        });

        prevButton.setOnClickListener(v -> {
            currentIndex = (currentIndex - 1 + imageIds.length) % imageIds.length;
            playTwinkle();
            updateContent();
        });
    }

    private void playTwinkle() {
        if (twinkleSound != null) {
            twinkleSound.start();
        }
    }

    private void updateContent() {
        imageView.setImageResource(imageIds[currentIndex]);
        nameText.setText(names[currentIndex]);
    }

    @Override
    protected void onDestroy() {
        if (twinkleSound != null) {
            twinkleSound.release();
        }
        super.onDestroy();
    }
}


