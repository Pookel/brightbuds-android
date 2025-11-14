package com.example.brightbuds_app.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.adapters.FamilyMembersAdapter;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.FamilyMember;
import com.example.brightbuds_app.services.DatabaseHelper;
import com.example.brightbuds_app.services.ProgressService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

public class FamilyModuleActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    // Constants
    private static final String TAG = "FamilyModuleActivity";

    // UI Elements
    private RecyclerView recyclerView;

    // Core Objects
    private FamilyMembersAdapter adapter;
    private List<FamilyMember> familyMembers;
    private TextToSpeech textToSpeech;
    private ProgressService progressService;
    private DatabaseHelper dbHelper;
    private FirebaseAuth auth;

    // Variables
    private String childId;   // For metrics tracking

    //  Activity Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_module);

        // Retrieve childId from intent (optional for metrics)
        childId = getIntent().getStringExtra("childId");

        // Initialize services
        auth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);
        progressService = new ProgressService(this);

        initializeViews();
        setupTextToSpeech();
        loadLocalFamilyMembers(); // Main data loader
    }

    //  Step 1 – Set up UI
    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvInstructions = findViewById(R.id.tvInstructions);

        btnBack.setOnClickListener(v -> finish());
        tvTitle.setText("My Family");
        tvTitle.setTypeface(tvTitle.getTypeface(), Typeface.BOLD);

//        tvInstructions.setText("👆 Tap on a family member to hear their name!");

        recyclerView = findViewById(R.id.recyclerViewFamily);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    //  Step 2 – Initialize Text-to-Speech
    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
    }

    //  Step 3 – Load Family Members from Local Database
    private void loadLocalFamilyMembers() {
        String parentId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : "local_parent";

        // Fetch stored family members from SQLite
        familyMembers = dbHelper.getFamilyMembers(parentId);

        if (familyMembers == null || familyMembers.isEmpty()) {
            Log.d(TAG, "ℹ️ No local family members found — showing default placeholders.");
            addDefaultFamilyMembers();
        } else {
            Log.d(TAG, "📂 Loaded " + familyMembers.size() + " local family members for " + parentId);
        }

        setupRecyclerView();
    }

    //  Step 4 – Fallback placeholders (shown if no stored entries)
    private void addDefaultFamilyMembers() {
        familyMembers = new java.util.ArrayList<>();
        familyMembers.add(new FamilyMember("Mom", "mother", R.drawable.default_mom, ""));
        familyMembers.add(new FamilyMember("Dad", "father", R.drawable.default_dad, ""));
        familyMembers.add(new FamilyMember("Grandma", "grandmother", R.drawable.default_grandma, ""));
        familyMembers.add(new FamilyMember("Grandpa", "grandfather", R.drawable.default_grandpa, ""));
        familyMembers.add(new FamilyMember("Sister", "sibling", R.drawable.default_sister, ""));
        familyMembers.add(new FamilyMember("Brother", "sibling", R.drawable.default_brother, ""));
    }

    //  Step 5 – Bind data to RecyclerView + handle tap-to-speak
    private void setupRecyclerView() {
        adapter = new FamilyMembersAdapter(familyMembers, member -> {
            if (textToSpeech != null) {
                // Build the spoken phrase using the relationship
                String relation = member.getRelationship();

                // Optional: make it sound more natural for siblings or unknown types
                if (relation == null || relation.trim().isEmpty()) {
                    relation = "family member";
                } else if (relation.equalsIgnoreCase("sibling")) {
                    relation = "brother or sister";
                }

                String phrase = "This is your " + relation;

                // Speak the relationship instead of the name
                textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null);

                // Track progress with the relationship label
                trackFamilyModuleProgress(relation);
            }

            // Display friendly feedback
            Toast.makeText(this, "👂 " + "This is your " + member.getRelationship(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "🔊 Spoken relationship: " + member.getRelationship());
        });

        recyclerView.setAdapter(adapter);
    }


    //  Step 6 – Track Child Progress (optional metrics)
    // Track family module plays and completion
    private void trackFamilyModuleProgress(String memberName) {
        if (childId == null) return;

        // Log one play per module entry
        progressService.logVideoPlay(childId, "family_module", new DataCallbacks.GenericCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "✅ Family module play recorded for: " + memberName);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "⚠️ Failed to mark family module complete", e);
            }
        });
    }


    //  Step 7 – Text-to-Speech Lifecycle
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "⚠️ TTS language not supported");
            }
        } else {
            Log.e(TAG, "❌ TTS initialization failed");
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
