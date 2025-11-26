package com.example.tayabastrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Email_Verification extends AppCompatActivity {

    private EditText otpBox1, otpBox2, otpBox3, otpBox4;
    private Button verifyButton, cancelButton;
    private TextView resendCode;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private register.UserRegistrationData userData;
    private String verificationCode;
    private long resendTimer = 0;
    private static final long RESEND_TIMEOUT = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        Log.d("EmailVerification", "onCreate called");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Get user data from intent
        Intent intent = getIntent();
        if (intent != null) {
            userData = (register.UserRegistrationData) intent.getParcelableExtra("userData");
            Log.d("EmailVerification", "UserData received: " + (userData != null ? "YES" : "NO"));

            if (userData != null) {
                Log.d("EmailVerification", "Email: " + userData.email);
            }
        }

        if (userData == null) {
            Log.e("EmailVerification", "No registration data found");
            Toast.makeText(this, "Error: No registration data found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views
        otpBox1 = findViewById(R.id.otpBox1);
        otpBox2 = findViewById(R.id.otpBox2);
        otpBox3 = findViewById(R.id.otpBox3);
        otpBox4 = findViewById(R.id.otpBox4);
        verifyButton = findViewById(R.id.verifyButton);
        cancelButton = findViewById(R.id.cancelButton);
        resendCode = findViewById(R.id.resendCode);
        backButton = findViewById(R.id.backButton);

        // Setup OTP input boxes to auto-move to next box
        setupOTPInputs();

        // Generate and send OTP on activity creation
        sendOTPEmail();

        // Handle Verify button
        verifyButton.setOnClickListener(v -> verifyOTP());

        // Handle Cancel button
        cancelButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(Email_Verification.this, Login.class));
        });

        // Handle Resend Code
        resendCode.setOnClickListener(v -> {
            if (System.currentTimeMillis() > resendTimer) {
                sendOTPEmail();
            } else {
                long remainingTime = (resendTimer - System.currentTimeMillis()) / 1000;
                Toast.makeText(Email_Verification.this,
                        "Please wait " + remainingTime + " seconds before requesting a new code",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Back button
        backButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(Email_Verification.this, Login.class));
        });
    }

    private void sendOTPEmail() {
        // Generate random 4-digit OTP
        verificationCode = String.format("%04d", new Random().nextInt(10000));

        // Store OTP in Firestore with timestamp
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("code", verificationCode);
        otpData.put("timestamp", System.currentTimeMillis());
        otpData.put("email", userData.email);
        otpData.put("used", false);

        db.collection("email_verifications").document(userData.email)
                .set(otpData)
                .addOnSuccessListener(aVoid -> {
                    // OTP stored, now trigger email sending
                    sendEmailViaFirebase(userData.email, verificationCode);

                    // Set resend timer
                    resendTimer = System.currentTimeMillis() + RESEND_TIMEOUT;

                    Toast.makeText(Email_Verification.this,
                            "Verification code sent to " + userData.email,
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Email_Verification.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmailViaFirebase(String email, String code) {
        // Create email document for Firebase Extension to process
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", email);
        emailData.put("message", new HashMap<String, Object>() {{
            put("subject", "TayabasTrack - Email Verification Code");
            put("text", "Your TayabasTrack verification code is: " + code +
                    "\n\nThis code will expire in 10 minutes." +
                    "\n\nIf you didn't request this code, please ignore this email." +
                    "\n\nBest regards," +
                    "\nTayabasTrack Team");
            put("html", "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px;'>" +
                    "<h2 style='color: #004AAD;'>TayabasTrack Email Verification</h2>" +
                    "<p>Your verification code is:</p>" +
                    "<div style='background: #f0f0f0; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 10px; color: #004AAD; margin: 20px 0;'>" +
                    code +
                    "</div>" +
                    "<p style='color: #666;'>This code will expire in 10 minutes.</p>" +
                    "<p style='color: #666;'>If you didn't request this code, please ignore this email.</p>" +
                    "<hr style='margin: 20px 0; border: none; border-top: 1px solid #ddd;'/>" +
                    "<p style='color: #999; font-size: 12px;'>Best regards,<br/>TayabasTrack Team<br/>tayabastrack@gmail.com</p>" +
                    "</div>");
        }});

        // Add to 'mail' collection (Trigger Email Extension)
        db.collection("mail").add(emailData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Email", "Email queued successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Email_Verification.this,
                            "Failed to send email: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupOTPInputs() {
        otpBox1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    otpBox2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        otpBox2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    otpBox3.requestFocus();
                } else if (s.length() == 0) {
                    otpBox1.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        otpBox3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    otpBox4.requestFocus();
                } else if (s.length() == 0) {
                    otpBox2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        otpBox4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    otpBox3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void verifyOTP() {
        String enteredOTP = otpBox1.getText().toString() +
                otpBox2.getText().toString() +
                otpBox3.getText().toString() +
                otpBox4.getText().toString();

        if (enteredOTP.length() < 4) {
            Toast.makeText(this, "Please enter all 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable verify button to prevent multiple clicks
        verifyButton.setEnabled(false);
        verifyButton.setText("Verifying...");

        // Verify OTP from Firestore
        db.collection("email_verifications").document(userData.email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedCode = documentSnapshot.getString("code");
                        Long timestamp = documentSnapshot.getLong("timestamp");
                        Boolean used = documentSnapshot.getBoolean("used");

                        // Check if code is valid (not expired and not used)
                        long currentTime = System.currentTimeMillis();
                        long expiryTime = 10 * 60 * 1000; // 10 minutes

                        if (used != null && used) {
                            Toast.makeText(Email_Verification.this,
                                    "This code has already been used. Please request a new one.",
                                    Toast.LENGTH_LONG).show();
                            clearOTPBoxes();
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify");
                            return;
                        }

                        if (timestamp != null && (currentTime - timestamp) > expiryTime) {
                            Toast.makeText(Email_Verification.this,
                                    "Code expired. Please request a new one.",
                                    Toast.LENGTH_LONG).show();
                            clearOTPBoxes();
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify");
                            return;
                        }

                        if (storedCode != null && storedCode.equals(enteredOTP)) {
                            // Mark as used
                            db.collection("email_verifications").document(userData.email)
                                    .update("used", true);

                            Toast.makeText(Email_Verification.this,
                                    "Email verified! Creating your account...",
                                    Toast.LENGTH_SHORT).show();

                            // NOW CREATE THE ACCOUNT
                            createFirebaseAccount();
                        } else {
                            Toast.makeText(Email_Verification.this,
                                    "Invalid code. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                            clearOTPBoxes();
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify");
                        }
                    } else {
                        Toast.makeText(Email_Verification.this,
                                "No verification code found. Please request a new one.",
                                Toast.LENGTH_LONG).show();
                        verifyButton.setEnabled(true);
                        verifyButton.setText("Verify");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Email_Verification.this,
                            "Error verifying code: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    verifyButton.setEnabled(true);
                    verifyButton.setText("Verify");
                });
    }

    private void createFirebaseAccount() {
        // Create Firebase Auth account
        mAuth.createUserWithEmailAndPassword(userData.email, userData.password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("Register", "Firebase Auth successful, User ID: " + user.getUid());

                            // Upload image first if available, then save user data
                            if (userData.getImageUri() != null || userData.getCapturedBitmap() != null) {
                                uploadImageToStorage(user.getUid());
                            } else {
                                // No image, save user data directly
                                saveUserToFirestore(user.getUid(), null);
                            }
                        }
                    } else {
                        verifyButton.setEnabled(true);
                        verifyButton.setText("Verify");

                        String errorMessage = "Failed to create account";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }

                        Toast.makeText(Email_Verification.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("Register", "Firebase Auth failed", task.getException());
                    }
                });
    }

    private void uploadImageToStorage(String userId) {
        StorageReference imageRef = storageRef.child("user_images/" + userId + "/id_image.jpg");

        byte[] imageBytes;

        Uri imageUri = userData.getImageUri();
        Bitmap capturedBitmap = userData.getCapturedBitmap();

        if (imageUri != null) {
            imageBytes = uriToByteArray(imageUri);
        } else if (capturedBitmap != null) {
            imageBytes = bitmapToByteArray(capturedBitmap);
        } else {
            saveUserToFirestore(userId, null);
            return;
        }

        if (imageBytes == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            saveUserToFirestore(userId, null);
            return;
        }

        Log.d("Storage", "Uploading image, size: " + imageBytes.length + " bytes");

        UploadTask uploadTask = imageRef.putBytes(imageBytes);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Log.d("Storage", "Image uploaded successfully. URL: " + imageUrl);

                saveUserToFirestore(userId, imageUrl);
            }).addOnFailureListener(e -> {
                Log.e("Storage", "Failed to get download URL", e);
                Toast.makeText(Email_Verification.this, "Failed to get image URL: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                saveUserToFirestore(userId, null);
            });
        }).addOnFailureListener(e -> {
            Log.e("Storage", "Failed to upload image", e);
            Toast.makeText(Email_Verification.this, "Failed to upload image: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            saveUserToFirestore(userId, null);
        });
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 80;

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

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    private byte[] uriToByteArray(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            return bitmapToByteArray(bitmap);
        } catch (Exception e) {
            Log.e("Register", "Error converting URI to byte array", e);
            return null;
        }
    }

    private void saveUserToFirestore(String userId, String imageUrl) {
        Map<String, Object> userDataMap = new HashMap<>();
        userDataMap.put("userId", userId);
        userDataMap.put("fullName", userData.fullName);
        userDataMap.put("firstName", userData.firstName);
        userDataMap.put("middleName", userData.middleName);
        userDataMap.put("surname", userData.surname);
        userDataMap.put("suffix", userData.suffix.equals("None") ? "" : userData.suffix);
        userDataMap.put("position", userData.position);
        userDataMap.put("barangay", userData.barangay);
        userDataMap.put("phoneNumber", userData.phoneNumber);
        userDataMap.put("email", userData.email);
        userDataMap.put("createdAt", System.currentTimeMillis());
        userDataMap.put("status", "pending");
        userDataMap.put("emailVerified", true);

        if (imageUrl != null) {
            userDataMap.put("idImageUrl", imageUrl);
            Log.d("Firestore", "Image URL added: " + imageUrl);
        }

        Log.d("Firestore", "Attempting to save user data");

        db.collection("users").document(userId)
                .set(userDataMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User data saved successfully");

                    Toast.makeText(Email_Verification.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();

                    // Navigate to dashboard
                    Intent intent = new Intent(Email_Verification.this, dashboard.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to save user data", e);
                    Toast.makeText(Email_Verification.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void clearOTPBoxes() {
        otpBox1.setText("");
        otpBox2.setText("");
        otpBox3.setText("");
        otpBox4.setText("");
        otpBox1.requestFocus();
    }
}