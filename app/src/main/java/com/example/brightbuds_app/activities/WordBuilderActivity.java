package com.example.brightbuds_app.activities;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.CustomWord;
import com.example.brightbuds_app.services.DatabaseHelper;
import com.example.brightbuds_app.services.ProgressService;
import com.example.brightbuds_app.services.WordService;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class WordBuilderActivity extends AppCompatActivity {

    private static final String TAG = "WordBuilderActivity";

    // UI
    private LinearLayout llWordContainer;
    private FlexboxLayout llLetters;
    private Button btnRestart;
    private TextView tvPlayAgain;

    private ImageView characterView;
    private ImageView homeButton;
    private ImageView closeButton;
    private KonfettiView konfettiView;

    // Services
    private ProgressService progressService;
    private WordService wordService;
    private DatabaseHelper dbHelper;
    private FirebaseAuth auth;

    // Game state
    private final List<CustomWord> words = new ArrayList<>();
    private int currentWordIndex = 0;
    private String childId;
    private boolean isAnimating = false;

    private int correctCount = 0;
    private int wrongCount = 0;
    private int totalPlays = 0; // Track total game sessions

    // Sounds
    private MediaPlayer correctSound;
    private MediaPlayer wrongSound;
    private MediaPlayer pointSound;

    // 🔥 TTS
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_builder);

        childId = getIntent().getStringExtra("childId");
        auth = FirebaseAuth.getInstance();
        progressService = new ProgressService(this);
        dbHelper = new DatabaseHelper(this);
        wordService = new WordService();

        // Core views
        llWordContainer = findViewById(R.id.llWordContainer);
        llLetters = findViewById(R.id.llLetters);
        btnRestart = findViewById(R.id.btnRestart);
        tvPlayAgain = findViewById(R.id.tvPlayAgain);

        characterView = findViewById(R.id.characterView);
        konfettiView = findViewById(R.id.konfettiView);
        homeButton = findViewById(R.id.homeButton);
        closeButton = findViewById(R.id.closeButton);

        btnRestart.setVisibility(View.GONE);
        tvPlayAgain.setVisibility(View.GONE);

        resetCharacter();

        // Back buttons
        View.OnClickListener backListener = v -> finish();
        homeButton.setOnClickListener(backListener);
        closeButton.setOnClickListener(backListener);

        // Sounds
        try { correctSound = MediaPlayer.create(this, R.raw.well_done_sound); } catch (Exception ignored) {}
        try { wrongSound = MediaPlayer.create(this, R.raw.wrong); } catch (Exception ignored) {}
        try { pointSound = MediaPlayer.create(this, R.raw.point_sound); } catch (Exception ignored) {}

        btnRestart.setOnClickListener(v -> restartGame());

        initTTS();
        loadWords();

        // ADDED: Log game start to ensure plays are counted
        logGameStart();
    }

    // ADDED: Log game start to ensure plays are counted
    private void logGameStart() {
        if (childId != null && !childId.isEmpty()) {
            progressService.logSimpleGamePlay(
                    childId,
                    "game_word_builder",
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Word Builder game start logged: " + message);
                            totalPlays++;
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to log game start", e);
                        }
                    }
            );
        }
    }

    // TTS SETUP
    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED) {

                    isTtsReady = true;

                    // FIRST MESSAGE
                    String welcomeMessage = "Let's build words! Drag the letters to complete the word.";
                    speak(welcomeMessage);
                }
            }
        });
    }

    private void speak(String text) {
        if (tts != null && isTtsReady) {
            try {
                tts.stop();
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_speak");
            } catch (Exception ignored) {}
        }
    }

    // DATA LOADING
    private void loadWords() {
        String parentId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "local_parent";

        wordService.fetchWordsForChild(parentId, childId, new DataCallbacks.WordListCallback() {
            @Override
            public void onSuccess(List<CustomWord> customWords) {
                words.clear();
                words.addAll(customWords);

                if (words.isEmpty()) loadDefaultWords();
                else dbHelper.saveWordsToCache(customWords);

                setupRound();

                // 🔥 ADD THIS: Ensure game start is logged after words are loaded
                logGameStart();
            }

            @Override
            public void onFailure(Exception e) {
                words.addAll(dbHelper.getCachedWords());
                if (words.isEmpty()) loadDefaultWords();
                setupRound();

                // ADD THIS: Ensure game start is logged even if cache is used
                logGameStart();
            }
        });
    }

    private void loadDefaultWords() {
        words.clear();
        words.add(new CustomWord("cat", "default", 0, 0));
        words.add(new CustomWord("sun", "default", 0, 0));
        words.add(new CustomWord("dog", "default", 0, 0));
        words.add(new CustomWord("hat", "default", 0, 0));
        words.add(new CustomWord("ball", "default", 0, 0));
    }

    // UI SETUP
    private void setupRound() {
        llWordContainer.removeAllViews();
        llLetters.removeAllViews();
        llWordContainer.setVisibility(View.VISIBLE);
        llLetters.setVisibility(View.VISIBLE);
        resetCharacter();
        isAnimating = false;

        if (currentWordIndex >= words.size()) {
            showGameCompletion();
            return;
        }

        CustomWord currentWord = words.get(currentWordIndex);
        String word = currentWord.getWord().toLowerCase();

        // SAY THE WORD
        if (isTtsReady) speak("Build " + word);

        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) chars.add(c);

        List<Integer> missingIndices = new ArrayList<>();
        for (int i = 0; i < chars.size(); i++) missingIndices.add(i);

        Collections.shuffle(missingIndices);
        int missingCount = Math.min(3, Math.max(1, word.length() / 2));
        missingIndices = missingIndices.subList(0, missingCount);

        List<Character> missingChars = new ArrayList<>();

        int spacing = (int) getResources().getDimension(R.dimen.tile_margin);
        float weight = 1f;

        int[] circleColors = {
                R.drawable.circle_green,
                R.drawable.circle_orange,
                R.drawable.circle_yellow,
                R.drawable.circle_purple,
                R.drawable.circle_red
        };

        // TOP ROW
        for (int i = 0; i < chars.size(); i++) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            params.setMargins(spacing, spacing, spacing, spacing);
            tv.setLayoutParams(params);

            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(36f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(getColor(R.color.white));
            tv.setPadding(0, 24, 0, 24);

            tv.setBackgroundResource(circleColors[new Random().nextInt(circleColors.length)]);

            if (missingIndices.contains(i)) {
                missingChars.add(chars.get(i));
                tv.setText("_");
                tv.setTag("drop_target");
                tv.setOnDragListener(dragListener);
            } else {
                tv.setText(String.valueOf(chars.get(i)).toUpperCase());
            }

            llWordContainer.addView(tv);
        }

        setupLetterChoices(missingChars);
    }

    private void setupLetterChoices(List<Character> missingChars) {

        List<Character> choices = new ArrayList<>(missingChars);
        while (choices.size() < 4) {
            char rand = (char) ('a' + new Random().nextInt(26));
            if (!choices.contains(rand)) choices.add(rand);
        }
        Collections.shuffle(choices);

        int tileSize = (int) getResources().getDimension(R.dimen.tile_choice_size);
        int margin = (int) getResources().getDimension(R.dimen.tile_margin);

        int[] circleColors = {
                R.drawable.circle_green,
                R.drawable.circle_orange,
                R.drawable.circle_yellow,
                R.drawable.circle_purple,
                R.drawable.circle_red
        };

        int colorIndex = 0;

        for (char c : choices) {
            TextView choice = new TextView(this);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(tileSize, tileSize);
            params.setMargins(margin, margin, margin, margin);

            choice.setLayoutParams(params);
            choice.setGravity(Gravity.CENTER);
            choice.setText(String.valueOf(c).toUpperCase());
            choice.setTextColor(getColor(R.color.white));
            choice.setTypeface(null, Typeface.BOLD);
            choice.setTextSize(30f);

            choice.setBackgroundResource(circleColors[colorIndex]);
            colorIndex = (colorIndex + 1) % circleColors.length;

            choice.setOnLongClickListener(v -> {
                ClipData.Item item = new ClipData.Item(choice.getText());
                ClipData data = new ClipData(choice.getText(),
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    v.startDragAndDrop(data, shadow, v, 0);
                }
                v.setVisibility(View.INVISIBLE);
                return true;
            });

            llLetters.addView(choice);
        }
    }

    // DRAG + CHECK
    private final View.OnDragListener dragListener = (view, event) -> {

        switch (event.getAction()) {
            case DragEvent.ACTION_DROP:
                TextView target = (TextView) view;
                String letter = event.getClipData().getItemAt(0).getText().toString();

                target.setText(letter);
                playPointSound();
                checkCompletion();
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                View dragged = (View) event.getLocalState();
                if (dragged != null && !event.getResult()) dragged.setVisibility(View.VISIBLE);
                return true;

            default:
                return true;
        }
    };

    private void checkCompletion() {
        if (isAnimating) return;

        StringBuilder built = new StringBuilder();
        boolean allFilled = true;

        for (int i = 0; i < llWordContainer.getChildCount(); i++) {
            TextView tv = (TextView) llWordContainer.getChildAt(i);

            if (tv.getText().toString().equals("_")) {
                allFilled = false;
                break;
            }
            built.append(tv.getText());
        }

        if (allFilled) {
            String target = words.get(currentWordIndex).getWord().toLowerCase();
            String builtWord = built.toString().toLowerCase();

            if (builtWord.equals(target)) {

                correctCount++;
                playCorrectFeedback();

                isAnimating = true;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    currentWordIndex++;
                    isAnimating = false;
                    setupRound();
                }, 1000);

            } else {

                wrongCount++;
                playWrongFeedback();

                isAnimating = true;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    isAnimating = false;
                    resetRound();
                }, 800);
            }
        }
    }

    private void resetRound() {
        // Reset the current round but keep progress
        setupRound();
    }

    // FEEDBACK
    private void playCorrectFeedback() {
        if (correctSound != null) {
            try {
                if (correctSound.isPlaying()) {
                    correctSound.pause();
                    correctSound.seekTo(0);
                }
                correctSound.start();
            } catch (Exception ignored) {}
        }
        showHappyCharacter();
        showConfetti();

        // Speak success message
        if (isTtsReady) speak("Great job! Correct!");
    }

    private void playWrongFeedback() {
        if (wrongSound != null) {
            try {
                if (wrongSound.isPlaying()) {
                    wrongSound.pause();
                    wrongSound.seekTo(0);
                }
                wrongSound.start();
            } catch (Exception ignored) {}
        }
        showSadCharacter();

        // Speak encouragement
        if (isTtsReady) speak("Try again! You can do it!");
    }

    private void playPointSound() {
        if (pointSound != null) {
            try {
                if (pointSound.isPlaying()) {
                    pointSound.pause();
                    pointSound.seekTo(0);
                }
                pointSound.start();
            } catch (Exception ignored) {}
        }
    }

    private void showHappyCharacter() {
        Glide.with(this).load(R.drawable.character_happy).into(characterView);
    }

    private void showSadCharacter() {
        Glide.with(this).load(R.drawable.character_sad).into(characterView);
    }

    private void resetCharacter() {
        showHappyCharacter();
    }

    private void showConfetti() {
        Party party = new PartyFactory(new Emitter(3, TimeUnit.SECONDS).perSecond(60))
                .spread(360)
                .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .position(0.5, 0.0, 0.5, 0.0)
                .build();
        konfettiView.start(party);
    }

    // GAME END
    private void showGameCompletion() {
        showConfetti();
        showHappyCharacter();

        llWordContainer.setVisibility(View.GONE);
        llLetters.setVisibility(View.GONE);
        tvPlayAgain.setVisibility(View.VISIBLE);
        btnRestart.setVisibility(View.VISIBLE);

        // SPEAK COMPLETION MESSAGE
        if (isTtsReady) {
            speak("Congratulations! You completed all words! You got " + correctCount + " correct!");
        }

        // Use the new standardized method
        if (childId != null && !childId.isEmpty()) {
            // Calculate score based on performance
            int totalAttempts = correctCount + wrongCount;
            int score = totalAttempts > 0 ? (correctCount * 100) / totalAttempts : 100;

            progressService.logWordBuilderPlay(
                    childId,
                    correctCount,
                    wrongCount,
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Word Builder progress recorded: " + message);
                            Log.d(TAG, "📊 Stats - Correct: " + correctCount +
                                    " | Wrong: " + wrongCount +
                                    " | Total Plays: " + totalPlays);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to record Word Builder stats", e);
                        }
                    }
            );
        }
    }

    private void restartGame() {
        currentWordIndex = 0;
        correctCount = 0;
        wrongCount = 0;

        tvPlayAgain.setVisibility(View.GONE);
        btnRestart.setVisibility(View.GONE);

        llWordContainer.setVisibility(View.VISIBLE);
        llLetters.setVisibility(View.VISIBLE);

        resetCharacter();

        //LOG NEW GAME SESSION WHEN RESTARTING
        logGameStart();

        setupRound();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ADDED: LOG PROGRESS IF USER LEAVES MID-GAME
        if (correctCount > 0 || wrongCount > 0) {
            logPartialProgress();
        }
    }

    // ADDED: Log progress if user exits before completing all words
    private void logPartialProgress() {
        if (childId != null && !childId.isEmpty() && (correctCount > 0 || wrongCount > 0)) {
            int totalAttempts = correctCount + wrongCount;
            int score = totalAttempts > 0 ? (correctCount * 100) / totalAttempts : 0;

            progressService.logWordBuilderPlay(
                    childId,
                    correctCount,
                    wrongCount,
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Partial progress recorded: " + message);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to record partial progress", e);
                        }
                    }
            );
        }
    }

    @Override
    protected void onDestroy() {
        if (correctSound != null) correctSound.release();
        if (wrongSound != null) wrongSound.release();
        if (pointSound != null) pointSound.release();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}