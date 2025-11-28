package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class forgotpassword extends AppCompatActivity {

    private TextInputEditText emailInput;
    private Button sendCodeButton;
    private TextView backToLoginText;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationCode;

    private static final String TAG = "ForgotPassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);

        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        androidx.core.view.WindowInsetsControllerCompat controller =
                new androidx.core.view.WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        backToLoginText = findViewById(R.id.backToLoginText);
        backButton = findViewById(R.id.backButton);

        sendCodeButton.setOnClickListener(v -> checkEmailAndSendCode());

        backToLoginText.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(forgotpassword.this, Login.class));
        });

        backButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(forgotpassword.this, Login.class));
        });
    }

    private void checkEmailAndSendCode() {
        String email = emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }

        sendCodeButton.setEnabled(false);
        sendCodeButton.setText("Checking...");

        checkEmailInFirestore(email);
    }

    private void checkEmailInFirestore(String email) {
        Log.d(TAG, "Checking email in Firestore: " + email);

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Email not found in Firestore");
                        Toast.makeText(forgotpassword.this,
                                "This email is not registered. Please check and try again.",
                                Toast.LENGTH_LONG).show();
                        resetButton();
                    } else {
                        Log.d(TAG, "Email found in Firestore");
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String userId = userDoc.getId();
                        String firestorePassword = userDoc.getString("password");

                        // Ensure user exists in Firebase Auth, then send OTP
                        ensureUserInFirebaseAuth(email, firestorePassword, userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(forgotpassword.this,
                            "Error checking email. Please try again.",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error checking email", e);
                    resetButton();
                });
    }

    private void ensureUserInFirebaseAuth(String email, String firestorePassword, String userId) {
        sendCodeButton.setText("Setting up...");

        // Check if user exists in Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean userExists = task.getResult().getSignInMethods() != null &&
                                !task.getResult().getSignInMethods().isEmpty();

                        if (userExists) {
                            Log.d(TAG, "User exists in Firebase Auth");
                            generateAndSendOTP(email);
                        } else {
                            Log.d(TAG, "User doesn't exist in Firebase Auth - creating account");
                            createUserInFirebaseAuth(email, firestorePassword, userId);
                        }
                    } else {
                        Log.e(TAG, "Error checking Firebase Auth", task.getException());
                        // Continue anyway
                        generateAndSendOTP(email);
                    }
                });
    }

    private void createUserInFirebaseAuth(String email, String password, String userId) {
        String authPassword = (password != null && !password.isEmpty()) ? password : "TempPass123456";

        mAuth.createUserWithEmailAndPassword(email, authPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth account created");
                        mAuth.signOut();
                        generateAndSendOTP(email);
                    } else {
                        Log.e(TAG, "Failed to create account", task.getException());
                        // Continue anyway - might already exist
                        generateAndSendOTP(email);
                    }
                });
    }

    private void generateAndSendOTP(String email) {
        verificationCode = String.format("%04d", new Random().nextInt(10000));
        Log.d(TAG, "Generated OTP: " + verificationCode + " for: " + email);

        Map<String, Object> otpData = new HashMap<>();
        otpData.put("code", verificationCode);
        otpData.put("timestamp", System.currentTimeMillis());
        otpData.put("email", email);
        otpData.put("used", false);
        otpData.put("type", "password_reset");

        sendCodeButton.setText("Sending code...");

        db.collection("password_reset_codes").document(email)
                .set(otpData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "OTP stored in Firestore");

                    // Send email via Firebase Extension
                    sendResetEmailViaExtension(email, verificationCode);

                    Toast.makeText(forgotpassword.this,
                            "Verification code sent to " + email,
                            Toast.LENGTH_LONG).show();

                    // Navigate to OTP verification screen
                    Intent intent = new Intent(forgotpassword.this, PasswordResetVerification.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(forgotpassword.this,
                            "Failed to send code. Please try again.",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error storing OTP", e);
                    resetButton();
                });
    }

    private void sendResetEmailViaExtension(String email, String code) {
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", email);
        emailData.put("message", new HashMap<String, Object>() {{
            put("subject", "TayabasTrack - Password Reset Code");
            put("text", "Your TayabasTrack password reset code is: " + code +
                    "\n\nThis code will expire in 10 minutes." +
                    "\n\nIf you didn't request this code, please ignore this email." +
                    "\n\nBest regards," +
                    "\nTayabasTrack Team");
            put("html", "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px;'>" +
                    "<h2 style='color: #004AAD;'>TayabasTrack Password Reset</h2>" +
                    "<p>You requested to reset your password. Your verification code is:</p>" +
                    "<div style='background: #f0f0f0; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 10px; color: #004AAD; margin: 20px 0;'>" +
                    code +
                    "</div>" +
                    "<p style='color: #666;'>This code will expire in 10 minutes.</p>" +
                    "<p style='color: #666;'>If you didn't request this code, please ignore this email.</p>" +
                    "<hr style='margin: 20px 0; border: none; border-top: 1px solid #ddd;'/>" +
                    "<p style='color: #999; font-size: 12px;'>Best regards,<br/>TayabasTrack Team<br/>tayabastrack@gmail.com</p>" +
                    "</div>");
        }});

        db.collection("mail").add(emailData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Email queued successfully in 'mail' collection");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to queue email", e);
                });
    }

    private void resetButton() {
        sendCodeButton.setEnabled(true);
        sendCodeButton.setText("Send Code");
    }
}