package com.example.familymodulegame;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    private ImageView detailImageView, starView;
    private TextView detailNameText, detailRelationText;
    private ImageButton prevButton, nextButton, buttonHome;
    private Button buttonClose;

    private int currentIndex = 0;

    //  Placeholder images
    private int[] images = {
            R.drawable.father,
            R.drawable.mother,
            R.drawable.sister
    };

    // Names and relations
    private String[] names = {
            "Mom",
            "Dad",
            "Grandma"
    };

    private String[] relations = {
            "Mother",
            "Father",
            "Grandmother"
    };

    private MediaPlayer chimeSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Load sound
        chimeSound = MediaPlayer.create(this, R.raw.twinkle);

        //  Find views
        detailImageView = findViewById(R.id.detailImageView);
        detailNameText = findViewById(R.id.detailNameText);
        detailRelationText = findViewById(R.id.detailRelationText);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        buttonClose = findViewById(R.id.buttonClose);
        buttonHome = findViewById(R.id.buttonHome);
        starView = findViewById(R.id.starView);

        //  Home button action
        buttonHome.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, GameActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        //  Set initial image, name, and relation
        updateContent();

        //  Previous
        prevButton.setOnClickListener(v -> {
            currentIndex--;
            if (currentIndex < 0) {
                currentIndex = images.length - 1;
            }
            playChime();
            updateContent();
        });

        // Next
        nextButton.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= images.length) {
                currentIndex = 0;
            }
            playChime();
            updateContent();
        });

        // Close button
        buttonClose.setOnClickListener(v -> finish());
    }

    private void updateContent() {
        detailImageView.setImageResource(images[currentIndex]);
        detailNameText.setText(names[currentIndex]);
        detailRelationText.setText(relations[currentIndex]);
    }

    private void playChime() {
        if (chimeSound != null) {
            chimeSound.start();
        }
        animateStar();
    }

    private void animateStar() {
        starView.setVisibility(View.VISIBLE);
        starView.setAlpha(1f);
        starView.setScaleX(0.5f);
        starView.setScaleY(0.5f);
        starView.setTranslationY(0f);

        ObjectAnimator moveUp = ObjectAnimator.ofFloat(starView, "translationY", 0f, -250f);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(starView, "alpha", 1f, 0f);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(starView, "scaleX", 0.5f, 1.3f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(starView, "scaleY", 0.5f, 1.3f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveUp, fadeOut, scaleUpX, scaleUpY);
        set.setDuration(1000);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chimeSound != null) {
            chimeSound.release();
            chimeSound = null;
        }
    }
}




