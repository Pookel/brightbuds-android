package com.example.brightbuds_app.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.ui.games.MemoryMatchFragment;
import com.example.brightbuds_app.services.ProgressService;

public class MemoryMatchActivity extends AppCompatActivity {
    private static final String TAG = "MemoryMatchActivity";

    private ProgressService progressService;
    private String childId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the activity layout that contains the fragment container
        setContentView(R.layout.activity_memory_match);

        // Get childId passed from module selector
        childId = getIntent().getStringExtra("childId");
        progressService = new ProgressService(this);

        // LOG GAME START - This ensures plays count increments
        logGameStart();

        // Load MemoryMatchFragment when activity starts
        if (savedInstanceState == null) {

            MemoryMatchFragment fragment = new MemoryMatchFragment();

            // Pass childId to fragment using arguments
            Bundle args = new Bundle();
            args.putString("childId", childId);
            fragment.setArguments(args);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.memoryMatchFragmentContainer, fragment)
                    .commit();
        }
    }

    // NEW METHOD: Log game start to ensure plays are counted
    private void logGameStart() {
        if (childId != null && !childId.isEmpty()) {
            progressService.logSimpleGamePlay(
                    childId,
                    "game_memory_match",
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Memory Match game start logged: " + message);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to log Memory Match game start", e);
                        }
                    }
            );
        } else {
            Log.w(TAG, "⚠️ Cannot log game start - childId is null or empty");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // LOG MINIMAL PROGRESS IF USER LEAVES EARLY
        // This ensures at least 1 play is recorded even if they don't complete the game
        logMinimalProgress();
    }

    // NEW METHOD: Log minimal progress to ensure plays count
    private void logMinimalProgress() {
        if (childId != null && !childId.isEmpty()) {
            progressService.logGamePlay(
                    childId,
                    "game_memory_match",
                    50, // Default score for partial completion
                    1,   // At least 1 correct match
                    0,   // No wrong attempts logged for minimal progress
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Memory Match minimal progress logged: " + message);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to log minimal progress", e);
                        }
                    }
            );
        }
    }

    // METHOD TO BE CALLED FROM FRAGMENT WHEN GAME IS COMPLETED
    public void logGameCompletion(int score, int correctMatches, int incorrectMatches, int totalTime) {
        if (childId != null && !childId.isEmpty()) {
            progressService.logGamePlay(
                    childId,
                    "game_memory_match",
                    score,
                    correctMatches,
                    incorrectMatches,
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Memory Match completion logged - Score: " + score +
                                    " | Correct: " + correctMatches +
                                    " | Incorrect: " + incorrectMatches);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to log Memory Match completion", e);
                        }
                    }
            );
        }
    }

    // METHOD TO BE CALLED FROM FRAGMENT FOR PROGRESS UPDATES
    public void logGameProgress(int correctMatches, int incorrectMatches) {
        if (childId != null && !childId.isEmpty()) {
            // Calculate score based on performance
            int totalAttempts = correctMatches + incorrectMatches;
            int score = totalAttempts > 0 ? (correctMatches * 100) / totalAttempts : 0;

            progressService.logGamePlay(
                    childId,
                    "game_memory_match",
                    score,
                    correctMatches,
                    incorrectMatches,
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Memory Match progress updated - Correct: " +
                                    correctMatches + " | Incorrect: " + incorrectMatches);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to update Memory Match progress", e);
                        }
                    }
            );
        }
    }
}