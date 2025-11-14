package com.example.brightbuds_app.services;

import android.content.Context;
import android.util.Log;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.interfaces.ProgressListCallback;
import com.example.brightbuds_app.models.Progress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgressService {

    private static final String TAG = "ProgressService";
    private static final int TOTAL_MODULES = 7;

    private final FirebaseFirestore db;
    private final DatabaseHelper localDb;

    public ProgressService(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.localDb = new DatabaseHelper(context);
    }

    // FETCH PROGRESS
    public void getAllProgressForParentWithChildren(String parentId,
                                                    List<String> childIds,
                                                    ProgressListCallback callback) {
        if (childIds == null || childIds.isEmpty()) {
            Log.w(TAG, "⚠️ No child IDs for parent: " + parentId);
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("child_progress")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Progress> result = new ArrayList<>();
                    Set<String> foundChildIds = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot) {
                        Progress p = doc.toObject(Progress.class);
                        if (p != null) {
                            p.setProgressId(doc.getId());
                            result.add(p);
                            foundChildIds.add(p.getChildId());
                            // from server → mark as synced locally
                            cacheProgressLocally(p, true);
                        }
                    }

                    validateChildProgressConsistency(childIds, foundChildIds);
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore fetch failed", e);
                    callback.onFailure(e);
                });
    }

    // MODULE COMPLETED
    public void markModuleCompleted(String childId,
                                    String moduleId,
                                    int score,
                                    DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🎯 markModuleCompleted child=" + childId +
                " module=" + moduleId + " score=" + score);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }

        final String parentId = user.getUid();
        Map<String, Object> data = createProgressData(parentId, childId, moduleId, score);

        db.collection("child_progress")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Log.i(TAG, "✅ Progress saved online: " + docRef.getId());
                    cacheProgressRecord(docRef.getId(), parentId, childId, moduleId,
                            score, "completed", true);
                    updateChildProgressStats(childId);
                    callback.onSuccess("Progress saved!");
                })
                .addOnFailureListener(e ->
                        handleProgressSaveFailure(e, parentId, childId, moduleId, score, callback));
    }

    // VIDEO PLAY - Standardized for chart compatibility
    public void logVideoPlay(String childId,
                             String moduleId,
                             DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🎥 logVideoPlay child=" + childId + " module=" + moduleId);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }
        if (childId == null || moduleId == null) {
            callback.onFailure(new IllegalArgumentException("Missing childId/moduleId"));
            return;
        }

        final String parentId = user.getUid();
        final String docId = childId + "_" + moduleId;

        // STANDARDIZED field names
        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("type", "video");
        data.put("status", "completed");
        data.put("completionStatus", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("plays", FieldValue.increment(1));
        data.put("score", 100);

        db.collection("child_progress")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "✅ Play logged online: " + docId);
                    cacheProgressRecord(docId, parentId, childId, moduleId,
                            100, "completed", true);
                    updateChildProgressStats(childId);
                    if (callback != null) callback.onSuccess("Play recorded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to log play online, caching", e);
                    cacheProgressRecord(docId, parentId, childId, moduleId,
                            100, "completed", false);
                    if (callback != null) callback.onSuccess("Saved locally (offline mode)");
                });
    }

    // UNIFIED GAME PLAY LOGGING - Use this for ALL games
    public void logGamePlay(String childId, String moduleId, int score, int correct, int incorrect,
                            DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🎮 logGamePlay: " + moduleId + " child=" + childId +
                " score=" + score + " correct=" + correct + " incorrect=" + incorrect);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }

        final String parentId = user.getUid();
        final String docId = childId + "_" + moduleId;

        // STANDARDIZED field names that match Progress.java model
        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("type", "game");
        data.put("status", "completed");
        data.put("score", score);
        data.put("correct", correct);
        data.put("incorrect", incorrect);
        data.put("completionStatus", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("plays", FieldValue.increment(1));
        data.put("timeSpent", 0L);

        // Push to Firestore
        db.collection("child_progress")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "✅ Game progress saved: " + moduleId +
                            " | plays=" + 1 + " | score=" + score);

                    // Cache locally
                    cacheProgressRecord(docId, parentId, childId, moduleId, score, "completed", true);
                    updateChildProgressStats(childId);

                    if (callback != null) callback.onSuccess("Game play recorded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save game progress, caching offline", e);
                    cacheProgressRecord(docId, parentId, childId, moduleId, score, "completed", false);
                    if (callback != null) callback.onSuccess("Saved locally (offline mode)");
                });
    }

    // SIMPLE GAME PLAY (when you don't need score/correct/incorrect)
    public void logSimpleGamePlay(String childId, String moduleId, DataCallbacks.GenericCallback callback) {
        logGamePlay(childId, moduleId, 100, 1, 0, callback);
    }

    // SET COMPLETION %
    public void setCompletionPercentage(String parentId,
                                        String childId,
                                        String moduleId,
                                        int percentage,
                                        DataCallbacks.GenericCallback callback) {

        db.collection("child_progress")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("childId", childId)
                .whereEqualTo("moduleId", moduleId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        updateExistingProgress(snapshot, percentage, callback);
                    } else {
                        markModuleCompleted(childId, moduleId, percentage, callback);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // FIXED RECORD GAME SESSION - Standardized field names
    public void recordGameSession(String childId, String moduleId, int score, long timeSpent,
                                  int stars, int totalCorrect, int totalIncorrect, int plays,
                                  DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🎯 recordGameSession: " + moduleId + " child=" + childId +
                " score=" + score + " plays=" + plays);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }

        final String parentId = user.getUid();
        final String docId = childId + "_" + moduleId;

        // STANDARDIZED field names
        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("type", "game");
        data.put("status", "completed");
        data.put("score", score);
        data.put("correct", totalCorrect);
        data.put("incorrect", totalIncorrect);
        data.put("completionStatus", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("timeSpent", timeSpent);
        data.put("stars", stars);
        data.put("plays", FieldValue.increment(plays));

        db.collection("child_progress")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "✅ Game session recorded: " + moduleId + " | plays=" + plays);
                    cacheProgressRecord(docId, parentId, childId, moduleId, score, "completed", true);
                    updateChildProgressStats(childId);
                    if (callback != null) callback.onSuccess("Game session recorded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to record game session, caching offline", e);
                    cacheProgressRecord(docId, parentId, childId, moduleId, score, "completed", false);
                    if (callback != null) callback.onSuccess("Saved locally (offline mode)");
                });
    }

    // WORD BUILDER - Uses standardized field names
    public void logWordBuilderPlay(String childId, int correctCount, int wrongCount,
                                   DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🧩 logWordBuilderPlay child=" + childId +
                " correct=" + correctCount + " wrong=" + wrongCount);

        final String moduleId = "game_word_builder";
        int totalAttempts = Math.max(1, correctCount + wrongCount);
        int score = (correctCount * 100) / totalAttempts;

        // Use the unified game play method with standardized fields
        logGamePlay(childId, moduleId, score, correctCount, wrongCount, callback);
    }

    // FAMILY MODULE - Standardized field names
    public void incrementFamilyModulePlays(String childId, DataCallbacks.GenericCallback callback) {
        Log.d(TAG, "👨‍👩‍👧‍👦 incrementFamilyModulePlays child=" + childId);
        logSimpleGamePlay(childId, "family_module", callback);
    }

    // INTERNAL HELPERS
    private Map<String, Object> createProgressData(String parentId,
                                                   String childId,
                                                   String moduleId,
                                                   int score) {
        Map<String, Object> m = new HashMap<>();
        m.put("parentId", parentId);
        m.put("childId", childId);
        m.put("moduleId", moduleId);
        m.put("score", score);
        m.put("status", "completed");
        m.put("completionStatus", score >= 70);
        m.put("timestamp", System.currentTimeMillis());
        m.put("timeSpent", 0L);
        m.put("plays", 1);
        m.put("type", "game");
        return m;
    }

    private void cacheProgressLocally(Progress p, boolean isSynced) {
        localDb.insertOrUpdateProgress(
                p.getProgressId(),
                p.getParentId(),
                p.getChildId(),
                p.getModuleId(),
                (int) p.getScore(),
                p.getStatus(),
                p.getTimestamp(),
                p.getTimeSpent(),
                isSynced
        );
    }

    private void cacheProgressRecord(String id,
                                     String parentId,
                                     String childId,
                                     String moduleId,
                                     int score,
                                     String status,
                                     boolean isSynced) {
        localDb.insertOrUpdateProgress(
                id,
                parentId,
                childId,
                moduleId,
                score,
                status,
                System.currentTimeMillis(),
                0L,
                isSynced
        );
    }

    private void handleProgressSaveFailure(Exception e,
                                           String parentId,
                                           String childId,
                                           String moduleId,
                                           int score,
                                           DataCallbacks.GenericCallback callback) {

        Log.e(TAG, "❌ Firestore unavailable, caching offline", e);
        String localId = "offline_" + System.currentTimeMillis();
        cacheProgressRecord(localId, parentId, childId, moduleId,
                score, "completed", false);
        callback.onSuccess("Saved locally (offline mode)");
    }

    private void validateChildProgressConsistency(List<String> expected, Set<String> found) {
        for (String id : expected) {
            if (!found.contains(id)) {
                Log.w(TAG, "⚠️ No progress found for child: " + id);
            }
        }
    }

    private void updateExistingProgress(QuerySnapshot snapshot,
                                        int percentage,
                                        DataCallbacks.GenericCallback callback) {

        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        String docId = doc.getId();
        String childId = doc.getString("childId");
        String parentId = doc.getString("parentId");
        String moduleId = doc.getString("moduleId");

        String status = percentage >= 100 ? "completed" : "in_progress";

        db.collection("child_progress").document(docId)
                .update(
                        "score", percentage,
                        "status", status,
                        "completionStatus", percentage >= 70,
                        "timestamp", System.currentTimeMillis()
                )
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "✅ Updated progress online: " + docId);
                    if (childId != null && parentId != null && moduleId != null) {
                        cacheProgressRecord(docId, parentId, childId, moduleId,
                                percentage, status, true);
                        updateChildProgressStats(childId);
                    }
                    callback.onSuccess("Progress updated!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update online, caching", e);
                    if (childId != null && parentId != null && moduleId != null) {
                        cacheProgressRecord(docId, parentId, childId, moduleId,
                                percentage, status, false);
                    }
                    callback.onFailure(e);
                });
    }

    /**
     * Recalculate child-level progress & stars from child_progress
     */
    private void updateChildProgressStats(String childId) {
        db.collection("child_progress")
                .whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;

                    int completedModules = 0;

                    for (DocumentSnapshot doc : snapshot) {
                        Progress p = doc.toObject(Progress.class);
                        if (p == null) continue;

                        boolean completed =
                                p.getScore() >= 70 ||
                                        "completed".equalsIgnoreCase(p.getStatus()) ||
                                        p.isCompletionStatus();

                        if (completed) completedModules++;
                    }

                    double ratio = completedModules / (double) TOTAL_MODULES;
                    int progressPercent = (int) Math.round(Math.min(1.0, ratio) * 100.0);
                    int stars = (int) Math.round(Math.min(1.0, ratio) * 5.0);

                    Log.d(TAG, "🌟 Stats child=" + childId +
                            " completed=" + completedModules +
                            " progress=" + progressPercent +
                            "% stars=" + stars);

                    db.collection("child_profiles").document(childId)
                            .update("progress", progressPercent, "stars", stars)
                            .addOnSuccessListener(unused ->
                                    Log.i(TAG, "✅ Child profile updated"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "❌ Failed to update child profile", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Failed to fetch child progress", e));
    }

    // Analytics helper
    public double calculateAverageScore(List<Progress> list) {
        if (list == null || list.isEmpty()) return 0;
        double total = 0;
        for (Progress p : list) total += p.getScore();
        return total / list.size();
    }

    // Legacy no-arg
    public void logVideoPlay() {
        Log.d(TAG, "⚠️ Deprecated logVideoPlay() called with no parameters.");
    }

    public void autoSyncOfflineProgress() {
        // Implementation for syncing offline progress
    }

    // INCREMENT MODULE PLAYS - Standardized field names
    public void incrementModulePlays(String childId, String moduleId, DataCallbacks.GenericCallback callback) {
        try {
            // Get current parent (offline-safe fallback)
            String parentId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "local_parent";

            // Build unique progress ID for this child/module
            String progressId = parentId + "_" + childId + "_" + moduleId;
            long timestamp = System.currentTimeMillis();

            // Read current score (acts as "plays" count)
            int currentScore = getCurrentLocalScore(childId, moduleId);
            int newScore = currentScore + 1;

            // Store locally (and mark unsynced = false)
            localDb.insertOrUpdateProgress(progressId, parentId, childId, moduleId,
                    newScore, "completed", timestamp, 0L, false);

            Log.d(TAG, "📊 incrementModulePlays → " + moduleId + " now at " + newScore);
            if (callback != null) callback.onSuccess("Incremented plays to " + newScore);
        } catch (Exception e) {
            Log.e(TAG, "❌ incrementModulePlays failed", e);
            if (callback != null) callback.onFailure(e);
        }
    }

    // Helper: Retrieve current local score (play count) from SQLite
    private int getCurrentLocalScore(String childId, String moduleId) {
        int score = 0;
        try (var db = localDb.getReadableDatabase();
             var cursor = db.query(DatabaseHelper.TABLE_CHILD_PROGRESS,
                     new String[]{DatabaseHelper.COLUMN_SCORE},
                     DatabaseHelper.COLUMN_CHILD_ID + "=? AND " + DatabaseHelper.COLUMN_MODULE_ID + "=?",
                     new String[]{childId, moduleId}, null, null, null)) {
            if (cursor.moveToFirst()) {
                score = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCORE));
            }
        } catch (Exception e) {
            Log.e(TAG, "⚠️ getCurrentLocalScore failed", e);
        }
        return score;
    }

    // DEPRECATED - Use logGamePlay instead
    @Deprecated
    public void recordWordBuilderStats(String childId, int correctCount, int wrongCount,
                                       DataCallbacks.GenericCallback callback) {
        Log.w(TAG, "⚠️ recordWordBuilderStats is deprecated. Use logWordBuilderPlay instead.");
        logWordBuilderPlay(childId, correctCount, wrongCount, callback);
    }

    // DEPRECATED - Use logGamePlay instead
    @Deprecated
    public void logModulePlay(String childId, String moduleId, int correctCount, int wrongCount,
                              DataCallbacks.GenericCallback callback) {
        Log.w(TAG, "⚠️ logModulePlay is deprecated. Use logGamePlay instead.");
        int score = (correctCount * 100) / Math.max(1, correctCount + wrongCount);
        logGamePlay(childId, moduleId, score, correctCount, wrongCount, callback);
    }
}