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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class forgotpassword extends AppCompatActivity {

    private TextInputEditText emailInput;
    private Button sendCodeButton;
    private TextView backToLoginText;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private String verificationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        backToLoginText = findViewById(R.id.backToLoginText);
        backButton = findViewById(R.id.backButton);

        // Handle Send Code button
        sendCodeButton.setOnClickListener(v -> checkEmailAndSendCode());

        // Handle Back to Login
        backToLoginText.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(forgotpassword.this, Login.class));
        });

        // Handle Back button
        backButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(forgotpassword.this, Login.class));
        });
    }

    private void checkEmailAndSendCode() {
        String email = emailInput.getText().toString().trim();

        // Validate email
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

        // Disable button to prevent multiple clicks
        sendCodeButton.setEnabled(false);
        sendCodeButton.setText("Checking...");

        // Check if email exists in the database
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Email not found - show error message
                        Toast.makeText(forgotpassword.this,
                                "This email is not registered. Please check and try again.",
                                Toast.LENGTH_LONG).show();
                        sendCodeButton.setEnabled(true);
                        sendCodeButton.setText("Send Code");
                    } else {
                        // Email exists - automatically generate and send OTP
                        sendCodeButton.setText("Sending code...");
                        generateAndSendOTP(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(forgotpassword.this,
                            "Error checking email. Please try again.",
                            Toast.LENGTH_SHORT).show();
                    sendCodeButton.setEnabled(true);
                    sendCodeButton.setText("Send Code");
                    Log.e("ForgotPassword", "Error checking email", e);
                });
    }

    private void generateAndSendOTP(String email) {
        // Generate random 4-digit OTP
        verificationCode = String.format("%04d", new Random().nextInt(10000));

        // Store OTP in Firestore with timestamp
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("code", verificationCode);
        otpData.put("timestamp", System.currentTimeMillis());
        otpData.put("email", email);
        otpData.put("used", false);
        otpData.put("type", "password_reset");

        db.collection("password_reset_codes").document(email)
                .set(otpData)
                .addOnSuccessListener(aVoid -> {
                    // OTP stored, now send email
                    sendResetEmailViaFirebase(email, verificationCode);

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
                    sendCodeButton.setEnabled(true);
                    sendCodeButton.setText("Send Code");
                    Log.e("ForgotPassword", "Error storing OTP", e);
                });
    }

    private void sendResetEmailViaFirebase(String email, String code) {
        // Create email document for Firebase Extension to process
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("to", email);
        emailData.put("message", new HashMap<String, Object>() {{
            put("subject", "TayabasTrack - Password Reset Code");
            put("text", "Your TayabasTrack password reset code is: " + code +
                    "\n\nThis code will expire in 10 minutes." +
                    "\n\nIf you didn't request this code, please ignore this email and your password will remain unchanged." +
                    "\n\nBest regards," +
                    "\nTayabasTrack Team");
            put("html", "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px;'>" +
                    "<h2 style='color: #004AAD;'>TayabasTrack Password Reset</h2>" +
                    "<p>You requested to reset your password. Your verification code is:</p>" +
                    "<div style='background: #f0f0f0; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 10px; color: #004AAD; margin: 20px 0;'>" +
                    code +
                    "</div>" +
                    "<p style='color: #666;'>This code will expire in 10 minutes.</p>" +
                    "<p style='color: #666;'>If you didn't request this code, please ignore this email and your password will remain unchanged.</p>" +
                    "<hr style='margin: 20px 0; border: none; border-top: 1px solid #ddd;'/>" +
                    "<p style='color: #999; font-size: 12px;'>Best regards,<br/>TayabasTrack Team<br/>tayabastrack@gmail.com</p>" +
                    "</div>");
        }});

        // Add to 'mail' collection (Trigger Email Extension)
        db.collection("mail").add(emailData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Email", "Password reset email queued successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("Email", "Failed to queue password reset email", e);
                });
    }
}