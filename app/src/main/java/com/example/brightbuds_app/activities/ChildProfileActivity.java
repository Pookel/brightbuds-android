package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.ChildProfile;
import com.example.brightbuds_app.services.ChildProfileService;
import com.example.brightbuds_app.utils.EncryptionUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChildProfileActivity extends AppCompatActivity {

    private EditText editTextChildName, editTextChildAge;
    private AutoCompleteTextView spinnerGender, spinnerLearningLevel;
    private Button buttonSaveChild;

    private ChildProfileService childProfileService;
    private FirebaseAuth auth;
    private static final String TAG = "ChildProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_profile);

        childProfileService = new ChildProfileService();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        setupDropdowns();
        setupSaveButton();

        Log.d(TAG, "✅ ChildProfileActivity started successfully");
    }

    /** Initialize UI components with null checks */
    private void initializeViews() {
        editTextChildName = findViewById(R.id.editTextChildName);
        editTextChildAge = findViewById(R.id.editTextChildAge);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerLearningLevel = findViewById(R.id.spinnerLearningLevel);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);
    }

    /** Configure toolbar navigation */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_add_child);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    /** Populate dropdown lists */
    private void setupDropdowns() {
        if (spinnerGender == null || spinnerLearningLevel == null) return;

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> levelAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.learning_levels,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerLearningLevel.setAdapter(levelAdapter);
    }

    /** Validate form and save securely to Firestore */
    private void setupSaveButton() {
        if (buttonSaveChild == null) return;

        buttonSaveChild.setOnClickListener(v -> {
            Log.d(TAG, "🎯 Save button clicked");

            String name = editTextChildName != null && editTextChildName.getText() != null
                    ? editTextChildName.getText().toString().trim() : "";
            String ageStr = editTextChildAge != null && editTextChildAge.getText() != null
                    ? editTextChildAge.getText().toString().trim() : "";
            String gender = spinnerGender != null ? spinnerGender.getText().toString().trim() : "";
            String learningLevel = spinnerLearningLevel != null ? spinnerLearningLevel.getText().toString().trim() : "";

            Log.d(TAG, "📝 Input values - Name: '" + name + "', Age: '" + ageStr + "', Gender: '" + gender + "', Level: '" + learningLevel + "'");

            if (!validateInputs(name, ageStr, gender, learningLevel)) return;

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                editTextChildAge.setError("Invalid age format");
                return;
            }

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "⚠️ Please log in first", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String parentId = currentUser.getUid();
            Log.d(TAG, "👤 Parent ID: " + parentId);

            // Encrypt sensitive fields before saving
            String encName = EncryptionUtil.encrypt(name);
            String encGender = EncryptionUtil.encrypt(gender);
            String encLevel = EncryptionUtil.encrypt(learningLevel);

            // Create encrypted ChildProfile object
            ChildProfile newChild = new ChildProfile(parentId, encName, age, encGender, encLevel);
            newChild.setDisplayName(encName);
            newChild.setActive(true);

            Log.d(TAG, "🔒 Encrypted Data - Name: " + encName + ", Gender: " + encGender + ", Level: " + encLevel);

            buttonSaveChild.setEnabled(false);
            buttonSaveChild.setText("Saving...");

            // Save to Firestore
            childProfileService.saveChildProfile(newChild, new DataCallbacks.GenericCallback() {
                @Override
                public void onSuccess(String childId) {
                    Log.i(TAG, "✅ Child saved successfully! ID: " + childId);
                    runOnUiThread(() -> {
                        Toast.makeText(ChildProfileActivity.this,
                                "✅ Child profile saved securely!",
                                Toast.LENGTH_SHORT).show();

                        buttonSaveChild.setEnabled(true);
                        buttonSaveChild.setText("Save Child Profile");

                        Intent intent = new Intent(ChildProfileActivity.this, RoleSelectionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "❌ Save failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(ChildProfileActivity.this,
                                "❌ Failed to save child: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        buttonSaveChild.setEnabled(true);
                        buttonSaveChild.setText("Save Child Profile");
                    });
                }
            });
        });
    }

    /** Validate all form fields */
    private boolean validateInputs(String name, String ageStr, String gender, String level) {
        if (TextUtils.isEmpty(name)) {
            editTextChildName.setError("Enter your child's name");
            return false;
        }

        if (name.length() < 2) {
            editTextChildName.setError("Name must be at least 2 characters");
            return false;
        }

        if (TextUtils.isEmpty(ageStr)) {
            editTextChildAge.setError("Enter your child's age");
            return false;
        }

        try {
            int age = Integer.parseInt(ageStr);
            if (age < 1 || age > 10) {
                editTextChildAge.setError("Age must be between 1 and 10");
                return false;
            }
        } catch (NumberFormatException e) {
            editTextChildAge.setError("Invalid age format");
            return false;
        }

        if (TextUtils.isEmpty(gender) || gender.equals("Select Gender")) {
            spinnerGender.setError("Select gender");
            return false;
        }

        if (TextUtils.isEmpty(level) || level.equals("Select Level")) {
            spinnerLearningLevel.setError("Select learning level");
            return false;
        }

        return true;
    }
}
