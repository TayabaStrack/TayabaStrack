package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private CheckBox termsCheckBox, noMiddleNameCheckBox;
    private Button registerButton, uploadImageButton;
    private ImageView profileImageView;
    private ImageButton backButton;
    private TextInputEditText emailField, passwordField, confirmPasswordField;
    private TextInputEditText firstNameField, middleNameField, surnameField, contactNumberField;
    private Spinner barangaySpinner, positionSpinner, suffixSpinner;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB max for Firestore blob

    private Uri imageUri;
    private Bitmap capturedBitmap;
    private byte[] imageBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Handle system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        initializeViews();

        // Setup event listeners
        setupEventListeners();

        // Make Terms clickable
        setupTermsAndConditions();
    }

    private void initializeViews() {
        // Basic views
        termsCheckBox = findViewById(R.id.termsCheckBox);
        registerButton = findViewById(R.id.registerButton);
        uploadImageButton = findViewById(R.id.UploadID);
        profileImageView = findViewById(R.id.ID);
        backButton = findViewById(R.id.backButton);

        // Form fields
        firstNameField = findViewById(R.id.fname);
        middleNameField = findViewById(R.id.mname);
        surnameField = findViewById(R.id.sname);
        contactNumberField = findViewById(R.id.contactNumber);
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        confirmPasswordField = findViewById(R.id.confirmpassword);

        // Checkbox and spinners
        noMiddleNameCheckBox = findViewById(R.id.no_mname);
        barangaySpinner = findViewById(R.id.spinnerBarangay);
        positionSpinner = findViewById(R.id.spinnerPosition);
        suffixSpinner = findViewById(R.id.spinnerSuffix);

        // Disable register until terms accepted
        registerButton.setEnabled(false);
        registerButton.setAlpha(0.5f);

        // Add input filters for names (letters only)
        setupNameFilters();
    }

    private void setupNameFilters() {
        // Add TextWatcher to ensure only letters and spaces are entered
        TextWatcher nameWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                // Remove any non-letter characters except spaces
                String filtered = text.replaceAll("[^a-zA-Z ]", "");
                if (!text.equals(filtered)) {
                    s.replace(0, s.length(), filtered);
                }
            }
        };

        // Apply to all name fields
        firstNameField.addTextChangedListener(nameWatcher);
        middleNameField.addTextChangedListener(nameWatcher);
        surnameField.addTextChangedListener(nameWatcher);

        // Add TextWatcher for contact number to ensure only numbers
        contactNumberField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                // Remove any non-digit characters
                String filtered = text.replaceAll("[^0-9]", "");
                // Limit to 10 digits
                if (filtered.length() > 10) {
                    filtered = filtered.substring(0, 10);
                }
                if (!text.equals(filtered)) {
                    s.replace(0, s.length(), filtered);
                }
            }
        });
    }

    private void setupEventListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(register.this, Login.class));
            finish();
        });

        // No middle name checkbox
        noMiddleNameCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            middleNameField.setEnabled(!isChecked);
            if (isChecked) {
                middleNameField.setText("");
            }
        });

        // Terms checkbox
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerButton.setEnabled(isChecked);
            registerButton.setAlpha(isChecked ? 1f : 0.5f);
        });

        // Register button
        registerButton.setOnClickListener(v -> registerUser());

        // Upload image button
        uploadImageButton.setOnClickListener(v -> showImagePickerDialog());
    }

    private void setupTermsAndConditions() {
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
                termsTextView.setTextColor(Color.WHITE);

                ScrollView scrollView = new ScrollView(register.this);
                scrollView.setBackgroundColor(Color.parseColor("#004aad"));
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
    }

    // Secure password hashing method
    private String hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Create MessageDigest instance for SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);

            // Hash the password
            byte[] hashedPassword = md.digest(password.getBytes("UTF-8"));

            // Convert salt and hash to hex strings
            String saltHex = bytesToHex(salt);
            String hashHex = bytesToHex(hashedPassword);

            // Return salt:hash format
            return saltHex + ":" + hashHex;
        } catch (Exception e) {
            Log.e("Register", "Error hashing password", e);
            return null;
        }
    }

    // Helper method to convert bytes to hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Validate Philippine mobile number (10 digits after +63)
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Check if it's exactly 10 digits and starts with 9
        return phoneNumber.matches("^9\\d{9}$");
    }

    // Convert bitmap to byte array with compression
    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 80; // Start with 80% quality

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

        // Reduce quality if size exceeds 1MB
        while (baos.toByteArray().length > MAX_IMAGE_SIZE && quality > 10) {
            baos.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        return baos.toByteArray();
    }

    // Convert URI to byte array
    private byte[] uriToByteArray(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            // Resize if too large
            int maxWidth = 1024;
            int maxHeight = 1024;

            if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                float scale = Math.min(
                        (float) maxWidth / bitmap.getWidth(),
                        (float) maxHeight / bitmap.getHeight()
                );

                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);

                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            return bitmapToByteArray(bitmap);
        } catch (Exception e) {
            Log.e("Register", "Error converting URI to byte array", e);
            return null;
        }
    }

    private void registerUser() {
        // Get form data
        String firstName = firstNameField.getText().toString().trim();
        String middleName = middleNameField.getText().toString().trim();
        String surname = surnameField.getText().toString().trim();
        String suffix = suffixSpinner.getSelectedItem().toString();
        String position = positionSpinner.getSelectedItem().toString();
        String contactNumber = contactNumberField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();
        String barangay = barangaySpinner.getSelectedItem().toString();

        // Validation
        if (firstName.isEmpty() || surname.isEmpty() ||
                contactNumber.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if position is selected
        if (position.equals("Select Position")) {
            Toast.makeText(this, "Please select a position", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!noMiddleNameCheckBox.isChecked() && middleName.isEmpty()) {
            Toast.makeText(this, "Please enter middle name or check 'I don't have a middle name'",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number
        if (!isValidPhoneNumber(contactNumber)) {
            Toast.makeText(this, "Please enter a valid Philippine mobile number (10 digits starting with 9)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");

        // Prepare image data if available
        if (imageUri != null) {
            imageBytes = uriToByteArray(imageUri);
        } else if (capturedBitmap != null) {
            imageBytes = bitmapToByteArray(capturedBitmap);
        }

        // Create user with Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Log.d("Register", "Firebase Auth successful, saving user data");
                            // Create full name with suffix
                            String fullName = firstName +
                                    (noMiddleNameCheckBox.isChecked() ? "" : " " + middleName) +
                                    " " + surname;

                            // Add suffix if not "None"
                            if (!suffix.equals("None")) {
                                fullName += " " + suffix;
                            }

                            // Format complete phone number with country code
                            String fullPhoneNumber = "+63" + contactNumber;

                            // Save user data to Firestore
                            saveUserToFirestore(user.getUid(), fullName, email, firstName,
                                    middleName, surname, suffix, position, barangay, fullPhoneNumber);
                        }
                    } else {
                        registerButton.setEnabled(true);
                        registerButton.setText("Register");
                        Toast.makeText(register.this, "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("Register", "Firebase Auth failed", task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String email, String firstName,
                                     String middleName, String surname, String suffix, String position,
                                     String barangay, String phoneNumber) {
        // Get the plain text password and hash it
        String password = passwordField.getText().toString().trim();
        String hashedPassword = hashPassword(password);

        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("fullName", fullName);
        userData.put("firstName", firstName);
        userData.put("middleName", noMiddleNameCheckBox.isChecked() ? "" : middleName);
        userData.put("surname", surname);
        userData.put("suffix", suffix.equals("None") ? "" : suffix);
        userData.put("position", position);
        userData.put("barangay", barangay);
        userData.put("phoneNumber", phoneNumber);
        userData.put("email", email);
        userData.put("passwordHash", hashedPassword);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("status", "pending");

        // Add image as Blob if available
        if (imageBytes != null) {
            Blob imageBlob = Blob.fromBytes(imageBytes);
            userData.put("idImage", imageBlob);
            Log.d("Firestore", "Image added as Blob, size: " + imageBytes.length + " bytes");
        }

        // Add debug logging
        Log.d("Firestore", "Attempting to save user data: " + userData.toString());
        Log.d("Firestore", "Current user ID: " + userId);
        Log.d("Firestore", "Phone number: " + phoneNumber);

        // Save to Firestore
        firestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User data saved successfully");
                    resetRegisterButton();
                    Toast.makeText(register.this, "Registration Successful! Please log in.",
                            Toast.LENGTH_SHORT).show();

                    // Sign out and redirect to login
                    auth.signOut();
                    startActivity(new Intent(register.this, Login.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to save user data", e);
                    resetRegisterButton();
                    Toast.makeText(register.this, "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetRegisterButton() {
        registerButton.setEnabled(true);
        registerButton.setText("Register");
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
                }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        }
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
                capturedBitmap = null;
                profileImageView.setImageURI(imageUri);
                profileImageView.setVisibility(ImageView.VISIBLE);
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                capturedBitmap = (Bitmap) data.getExtras().get("data");
                imageUri = null;
                profileImageView.setImageBitmap(capturedBitmap);
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