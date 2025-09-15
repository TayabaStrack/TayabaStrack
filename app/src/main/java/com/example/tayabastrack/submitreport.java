package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class submitreport extends AppCompatActivity {

    private EditText description, width, height, involved;
    private Spinner spinnerBarangay;
    private ImageView previewImage;
    private Button btnUpload, btnSubmit;
    private ImageButton backButton;

    private Uri imageUri = null;
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitreport);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 🔹 Initialize views
        description = findViewById(R.id.description);
        width = findViewById(R.id.width);
        height = findViewById(R.id.height);
        involved = findViewById(R.id.involed);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        previewImage = findViewById(R.id.previewImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnSubmit = findViewById(R.id.btnSubmit);
        backButton = findViewById(R.id.backButton);

        // 🔹 Back button
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(submitreport.this, dashboard.class);
            startActivity(intent);
            finish();
        });

        // 🔹 Upload image button
        btnUpload.setOnClickListener(v -> showImagePickerDialog());

        // 🔹 Submit report
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    // 🔹 Show options for upload (Camera or Gallery)
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Upload Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else {
                            openCamera();
                        }
                    } else { // Gallery
                        openGallery();
                    }
                }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 🔹 Handle results from camera/gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
                previewImage.setImageURI(imageUri);
                previewImage.setVisibility(ImageView.VISIBLE);
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                previewImage.setImageBitmap(photo);
                previewImage.setVisibility(ImageView.VISIBLE);
            }
        }
    }

    // 🔹 Handle camera permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🔹 Validate fields and show success message
    private void validateAndSubmit() {
        String desc = description.getText().toString().trim();
        String w = width.getText().toString().trim();
        String h = height.getText().toString().trim();
        String inv = involved.getText().toString().trim();

        if (desc.isEmpty()) {
            description.setError("Required");
            description.requestFocus();
            return;
        }
        if (w.isEmpty()) {
            width.setError("Required");
            width.requestFocus();
            return;
        }
        if (h.isEmpty()) {
            height.setError("Required");
            height.requestFocus();
            return;
        }
        if (inv.isEmpty()) {
            involved.setError("Required");
            involved.requestFocus();
            return;
        }
        if (spinnerBarangay.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a Barangay", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageUri == null && previewImage.getDrawable() == null) {
            Toast.makeText(this, "Please upload an image", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Report Submitted Successfully!", Toast.LENGTH_LONG).show();

        // After submitting, go back to dashboard
        Intent intent = new Intent(submitreport.this, dashboard.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Also go back to dashboard if physical back button is pressed
        Intent intent = new Intent(submitreport.this, dashboard.class);
        startActivity(intent);
        finish();
    }
}
