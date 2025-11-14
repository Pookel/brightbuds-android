package com.example.brightbuds_app.activities;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.CustomWord;
import com.example.brightbuds_app.services.WordService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomWordsActivity
 * --------------------
 * Allows parents to add, edit, and delete up to 8 learning words
 * stored under their Firebase user ID for child games.
 */
public class CustomWordsActivity extends AppCompatActivity {

    private LinearLayout wordsContainer;
    private MaterialButton btnAddWord, btnSaveAll;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private WordService wordService;
    private List<CustomWord> customWords = new ArrayList<>();

    private static final int MAX_WORDS = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_words);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        wordService = new WordService(this);

        wordsContainer = findViewById(R.id.wordsContainer);
        btnAddWord = findViewById(R.id.btnAddWord);
        btnSaveAll = findViewById(R.id.btnSaveAll);

        loadExistingWords();

        btnAddWord.setOnClickListener(v -> showAddWordDialog());
        btnSaveAll.setOnClickListener(v -> saveAllWords());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadExistingWords() {
        String parentId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : "local_parent";

        wordService.fetchWordsForParent(parentId, new com.example.brightbuds_app.interfaces.DataCallbacks.WordListCallback() {
            @Override
            public void onSuccess(List<CustomWord> list) {
                customWords.clear();
                customWords.addAll(list);
                refreshWordList();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CustomWordsActivity.this, "Failed to load words", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddWordDialog() {
        if (customWords.size() >= MAX_WORDS) {
            Toast.makeText(this, "You can only add up to 8 words", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add a new word");

        EditText input = new EditText(this);
        input.setHint("Enter word (e.g. apple)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newWord = input.getText().toString().trim().toLowerCase();
            if (!newWord.isEmpty()) {
                customWords.add(new CustomWord(newWord, "", System.currentTimeMillis(), 0));
                refreshWordList();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void refreshWordList() {
        wordsContainer.removeAllViews();

        for (int i = 0; i < customWords.size(); i++) {
            CustomWord word = customWords.get(i);

            MaterialButton btnWord = new MaterialButton(this);
            btnWord.setText(word.getWord());
            btnWord.setTextSize(18f);
            btnWord.setAllCaps(false);
            btnWord.setBackgroundTintList(getColorStateList(R.color.primary));
            btnWord.setTextColor(getColor(R.color.white));
            btnWord.setPadding(24, 16, 24, 16);

            int finalI = i;
            btnWord.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Remove word?")
                        .setMessage("Do you want to delete \"" + word.getWord() + "\"?")
                        .setPositiveButton("Yes", (dialog, w) -> {
                            customWords.remove(finalI);
                            refreshWordList();
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            });

            wordsContainer.addView(btnWord);
        }
    }

    private void saveAllWords() {
        String parentId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : "local_parent";

        if (customWords.isEmpty()) {
            Toast.makeText(this, "No words to save", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(parentId)
                .collection("custom_words")
                .get()
                .addOnSuccessListener(snapshot -> {
                    // delete existing ones first
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        snapshot.getDocuments().forEach(doc -> doc.getReference().delete());
                    }

                    for (CustomWord cw : customWords) {
                        db.collection("users")
                                .document(parentId)
                                .collection("custom_words")
                                .add(cw);
                    }

                    Toast.makeText(this, "Words saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
