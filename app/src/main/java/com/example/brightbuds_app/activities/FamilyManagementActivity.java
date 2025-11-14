package com.example.brightbuds_app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.services.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FamilyManagementActivity extends AppCompatActivity {

    // Constants
    private static final int PICK_IMAGE_REQUEST = 100;

    // UI Elements
    private ImageView imgPreview;
    private EditText edtName;
    private EditText edtRelationship;
    private Button btnSave;

    // Variables
    private Uri imageUri;
    private ProgressDialog progressDialog;
    private FirebaseAuth auth;
    private DatabaseHelper dbHelper;

    // Activity Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_management);

        // Initialize UI Elements
        imgPreview = findViewById(R.id.imgPreview);
        edtName = findViewById(R.id.edtName);
        edtRelationship = findViewById(R.id.edtRelationship);
        btnSave = findViewById(R.id.btnUpload);

        // Initialize FirebaseAuth and local DB Helper
        auth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        // Set Click Listeners
        imgPreview.setOnClickListener(v -> openFileChooser());      // Select photo
        btnSave.setOnClickListener(v -> saveFamilyMemberLocally()); // Save entry
    }

    //  Step 1: Open Gallery to Choose Image
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");         // Only show images
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    //  Step 2: Receive Selected Image and Show Preview
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(imgPreview); // Display preview
        }
    }

    //  Step 3: Validate Inputs, Save Image Locally, Store Metadata in SQLite
    private void saveFamilyMemberLocally() {
        // Collect user inputs
        String name = edtName.getText().toString().trim();
        String relationship = edtRelationship.getText().toString().trim();

        // Validate required fields
        if (name.isEmpty() || relationship.isEmpty() || imageUri == null) {
            Toast.makeText(this, "⚠️ Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        progressDialog = ProgressDialog.show(this, "Saving", "Please wait...", true);

        try {

            //  1. Identify current parent (for folder separation)
            String parentId = auth.getCurrentUser() != null
                    ? auth.getCurrentUser().getUid()
                    : "local_parent";  // fallback if offline or not logged in

            //  2. Create internal directory for this parent’s family
            File dir = new File(getFilesDir(), "MyFamily/" + parentId);
            if (!dir.exists()) dir.mkdirs();   // Create folder if it doesn’t exist

            //  3. Copy selected image into internal storage
            String safeName = name.replaceAll("\\s+", "_");
            String fileName = safeName + "_" + System.currentTimeMillis() + ".jpg";
            File localFile = new File(dir, fileName);

            // Copy file stream
            try (InputStream in = getContentResolver().openInputStream(imageUri);
                 OutputStream out = new FileOutputStream(localFile)) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            }

            //  4. Insert metadata into SQLite database
            dbHelper.addFamilyMember(parentId, name, relationship, localFile.getAbsolutePath());

            //  5. Finish up
            progressDialog.dismiss();
            Toast.makeText(this, "✅ Family member saved locally!", Toast.LENGTH_SHORT).show();
            clearForm();

        } catch (Exception e) {
            // Handle any unexpected errors gracefully
            progressDialog.dismiss();
            Toast.makeText(this, "❌ Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //  Step 4: Reset Input Fields After Successful Save
    private void clearForm() {
        edtName.setText("");
        edtRelationship.setText("");
        imgPreview.setImageResource(R.drawable.ic_user_placeholder);
        imageUri = null;
    }
}
