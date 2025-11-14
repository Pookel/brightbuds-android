package com.example.brightbuds_app.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.services.ProgressService;
import com.example.brightbuds_app.ui.games.FeedTheMonsterFragment;

/*
 Hosts the FeedTheMonsterFragment inside a FragmentContainerView.
 Activity contains no game logic to keep responsibilities separate.
*/
public class FeedMonsterActivity extends AppCompatActivity {
    private static final String TAG = "FeedMonsterActivity";

    private ProgressService progressService;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_monster);
        setTitle("Feed the Monster");

        // Get childId from Intent
        childId = getIntent().getStringExtra("childId");
        progressService = new ProgressService(this);

        // ADDED: Log game start to ensure plays are counted
        logGameStart();

        if (savedInstanceState == null) {
            // Create fragment
            FeedTheMonsterFragment fragment = new FeedTheMonsterFragment();

            // Pass childId to fragment
            Bundle args = new Bundle();
            args.putString("childId", childId);
            fragment.setArguments(args);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.feedMonsterFragmentContainer, fragment)
                    .commit();
        }
    }

    // ADDED: Log game start to ensure plays are counted
    private void logGameStart() {
        if (childId != null && !childId.isEmpty()) {
            progressService.logSimpleGamePlay(
                    childId,
                    "game_feed_monster",
                    new DataCallbacks.GenericCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Log.d(TAG, "✅ Feed Monster game start logged: " + message);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "❌ Failed to log Feed Monster game start", e);
                        }
                    }
            );
        } else {
            Log.w(TAG, "⚠️ Cannot log game start - childId is null or empty");
        }
    }
}