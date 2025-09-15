package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton; // ✅ Import this
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class register extends AppCompatActivity {

    private CheckBox termsCheckBox;
    private Button registerButton, uploadImageButton;
    private ImageView profileImageView;
    private ImageButton backButton; // ✅ Add this
//change 1
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        termsCheckBox = findViewById(R.id.termsCheckBox);
        registerButton = findViewById(R.id.registerButton);
        uploadImageButton = findViewById(R.id.UploadID);
        profileImageView = findViewById(R.id.ID);
        backButton = findViewById(R.id.backButton); // ✅ Reference back button

        // ✅ Back button functionality
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(register.this, Login.class);
            startActivity(intent);
            finish(); // optional: closes the register screen so user can't go back to it using back
        });

        // Disable register button until checkbox is checked
        registerButton.setEnabled(false);
        registerButton.setAlpha(0.5f);

        // Full text for checkbox
        String text = "I agree to the Terms and Conditions";
        SpannableString spannableString = new SpannableString(text);

        int start = text.indexOf("Terms and Conditions");
        int end = start + "Terms and Conditions".length();

        // Clickable "Terms and Conditions"
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                TextView termsTextView = new TextView(register.this);
                termsTextView.setText("📜 Terms and Conditions\n\n" +
                        "1. You agree to provide accurate and truthful information during registration.\n\n" +
                        "2. You consent to the use of your personal data for the purposes of this application.\n\n" +
                        "3. You must not share your account credentials with others.\n\n" +
                        "4. Misuse of the application may result in account suspension or termination.\n\n" +
                        "5. The developers reserve the right to update these terms without prior notice.\n\n" +
                        "⚠ Please read carefully before agreeing.");
                termsTextView.setPadding(40, 40, 40, 40);
                termsTextView.setTextSize(14f);
                termsTextView.setTextColor(Color.parseColor("#004aad"));

                ScrollView scrollView = new ScrollView(register.this);
                scrollView.setBackgroundColor(Color.WHITE);
                scrollView.addView(termsTextView);

                AlertDialog dialog = new AlertDialog.Builder(register.this, R.style.CustomAlertDialog)
                        .setView(scrollView)
                        .setPositiveButton("OK", null)
                        .create();

                dialog.setOnShowListener(dialogInterface -> {
                    Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    okButton.setBackgroundColor(Color.parseColor("#004aad"));
                    okButton.setTextColor(Color.WHITE);
                });

                dialog.show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(Color.parseColor("#004aad"));
            }
        };

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsCheckBox.setText(spannableString);
        termsCheckBox.setMovementMethod(LinkMovementMethod.getInstance());
        termsCheckBox.setHighlightColor(Color.TRANSPARENT);

        // Enable register button only if checked
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerButton.setEnabled(isChecked);
            registerButton.setAlpha(isChecked ? 1f : 0.5f);
        });

        // Register button
        registerButton.setOnClickListener(v -> {
            new AlertDialog.Builder(register.this)
                    .setTitle("Success")
                    .setMessage("You have successfully registered!")
                    .setPositiveButton("OK", null)
                    .show();
        });

        // Upload Image button -> choose Camera or Gallery
        uploadImageButton.setOnClickListener(v -> showImagePickerDialog());
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else {
                            openCamera();
                        }
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
                profileImageView.setImageURI(imageUri);
                profileImageView.setVisibility(ImageView.VISIBLE);
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                profileImageView.setImageBitmap(photo);
                profileImageView.setVisibility(ImageView.VISIBLE);
            }
        }
    }

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
}
