package com.example.brightbuds_app.services;

import android.content.Context;
import android.util.Log;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.CustomWord;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles fetching of parent-created or default words for Word Builder.
 * Supports both parent-level and child-specific queries.
 */
public class WordService {

    private static final String TAG = "WordService";
    private final FirebaseFirestore db;

    public WordService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public WordService(Context context) {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetch all custom words created by a parent (global for all their children)
     */
    public void fetchWordsForParent(String parentId, DataCallbacks.WordListCallback callback) {
        db.collection("users")
                .document(parentId)
                .collection("custom_words")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<CustomWord> words = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String word = doc.getString("word");
                        if (word != null && !word.trim().isEmpty()) {
                            CustomWord customWord = new CustomWord(word.trim(), parentId,
                                    System.currentTimeMillis(), 0);
                            customWord.setChildId(doc.getString("childId")); // Set childId if exists
                            words.add(customWord);
                        }
                    }
                    Log.d(TAG, "✅ Loaded " + words.size() + " parent-level custom words");
                    callback.onSuccess(words);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch parent custom words", e);
                    callback.onFailure(e);
                });
    }

    /**
     * SMART QUERY: Fetches words for child with multiple fallback strategies
     */
    public void fetchWordsForChild(String parentId, String childId, DataCallbacks.WordListCallback callback) {
        Log.d(TAG, "🔍 SMART QUERY: parent=" + parentId + ", child=" + childId);

        List<CustomWord> allWords = new ArrayList<>();

        // Strategy 1: Try child-specific words from custom_words collection
        db.collection("custom_words")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(childSnapshot -> {
                    Log.d(TAG, "🎯 Strategy 1: Found " + childSnapshot.size() + " child-specific words");

                    // Add child-specific words
                    for (QueryDocumentSnapshot doc : childSnapshot) {
                        CustomWord customWord = doc.toObject(CustomWord.class);
                        if (customWord != null && customWord.getWord() != null && !customWord.getWord().trim().isEmpty()) {
                            allWords.add(customWord);
                            Log.d(TAG, "📥 Child word: " + customWord.getWord());
                        }
                    }

                    // Strategy 2: Try parent-level words from custom_words collection (without childId filter)
                    db.collection("custom_words")
                            .whereEqualTo("parentId", parentId)
                            .get()
                            .addOnSuccessListener(parentSnapshot -> {
                                Log.d(TAG, "🎯 Strategy 2: Found " + parentSnapshot.size() + " parent-level words in custom_words");

                                // Add parent-level words that don't have childId or have matching childId
                                for (QueryDocumentSnapshot doc : parentSnapshot) {
                                    CustomWord customWord = doc.toObject(CustomWord.class);
                                    if (customWord != null && customWord.getWord() != null && !customWord.getWord().trim().isEmpty()) {
                                        String docChildId = customWord.getChildId();
                                        // Include if no childId specified (backward compatibility) OR matches current child
                                        if (docChildId == null || docChildId.isEmpty() || docChildId.equals(childId)) {
                                            // Avoid duplicates
                                            boolean alreadyExists = allWords.stream()
                                                    .anyMatch(w -> w.getWord().equalsIgnoreCase(customWord.getWord()));
                                            if (!alreadyExists) {
                                                allWords.add(customWord);
                                                Log.d(TAG, "📥 Parent word: " + customWord.getWord());
                                            }
                                        }
                                    }
                                }

                                // Strategy 3: Final fallback - try users/{parentId}/custom_words collection
                                db.collection("users")
                                        .document(parentId)
                                        .collection("custom_words")
                                        .get()
                                        .addOnSuccessListener(userSnapshot -> {
                                            Log.d(TAG, "🎯 Strategy 3: Found " + userSnapshot.size() + " words in users collection");

                                            // Add words from users collection
                                            for (QueryDocumentSnapshot doc : userSnapshot) {
                                                String word = doc.getString("word");
                                                if (word != null && !word.trim().isEmpty()) {
                                                    CustomWord customWord = new CustomWord(word.trim(), parentId,
                                                            System.currentTimeMillis(), 0);
                                                    customWord.setChildId(doc.getString("childId"));

                                                    // Avoid duplicates
                                                    boolean alreadyExists = allWords.stream()
                                                            .anyMatch(w -> w.getWord().equalsIgnoreCase(customWord.getWord()));
                                                    if (!alreadyExists) {
                                                        allWords.add(customWord);
                                                        Log.d(TAG, "📥 User collection word: " + customWord.getWord());
                                                    }
                                                }
                                            }

                                            Log.d(TAG, "✅ FINAL RESULT: Loaded " + allWords.size() + " total words for child: " + childId);
                                            callback.onSuccess(allWords);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ Strategy 3 failed, but returning collected words", e);
                                            Log.d(TAG, "✅ Returning " + allWords.size() + " words collected so far");
                                            callback.onSuccess(allWords);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Strategy 2 failed, trying final fallback", e);
                                // Try final fallback directly
                                tryFinalFallback(parentId, childId, allWords, callback);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Strategy 1 failed, trying alternatives", e);
                    // Start from strategy 2 if strategy 1 fails
                    tryAlternativeStrategies(parentId, childId, callback);
                });
    }

    private void tryAlternativeStrategies(String parentId, String childId, DataCallbacks.WordListCallback callback) {
        List<CustomWord> allWords = new ArrayList<>();

        db.collection("custom_words")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(parentSnapshot -> {
                    Log.d(TAG, "🔄 Alternative: Found " + parentSnapshot.size() + " parent-level words");

                    for (QueryDocumentSnapshot doc : parentSnapshot) {
                        CustomWord customWord = doc.toObject(CustomWord.class);
                        if (customWord != null && customWord.getWord() != null && !customWord.getWord().trim().isEmpty()) {
                            allWords.add(customWord);
                        }
                    }

                    tryFinalFallback(parentId, childId, allWords, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ All strategies failed, using final fallback", e);
                    tryFinalFallback(parentId, childId, allWords, callback);
                });
    }

    private void tryFinalFallback(String parentId, String childId, List<CustomWord> currentWords, DataCallbacks.WordListCallback callback) {
        db.collection("users")
                .document(parentId)
                .collection("custom_words")
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    Log.d(TAG, "🔄 Final Fallback: Found " + userSnapshot.size() + " words in users collection");

                    for (QueryDocumentSnapshot doc : userSnapshot) {
                        String word = doc.getString("word");
                        if (word != null && !word.trim().isEmpty()) {
                            CustomWord customWord = new CustomWord(word.trim(), parentId,
                                    System.currentTimeMillis(), 0);
                            customWord.setChildId(doc.getString("childId"));

                            // Avoid duplicates
                            boolean alreadyExists = currentWords.stream()
                                    .anyMatch(w -> w.getWord().equalsIgnoreCase(customWord.getWord()));
                            if (!alreadyExists) {
                                currentWords.add(customWord);
                            }
                        }
                    }

                    Log.d(TAG, "✅ FINAL: Loaded " + currentWords.size() + " total words for child: " + childId);
                    callback.onSuccess(currentWords);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Final fallback also failed", e);
                    Log.d(TAG, "✅ Returning " + currentWords.size() + " words collected");
                    callback.onSuccess(currentWords);
                });
    }
}