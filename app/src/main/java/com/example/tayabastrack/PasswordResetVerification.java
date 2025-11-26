package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PasswordResetVerification extends AppCompatActivity {

    private EditText otpBox1, otpBox2, otpBox3, otpBox4;
    private Button verifyButton;
    private TextView resendCode, emailDisplay;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private String userEmail;
    private long resendTimer = 0;
    private static final long RESEND_TIMEOUT = 60000; // 60 seconds
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset_verification);
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        androidx.core.view.WindowInsetsControllerCompat controller =
                new androidx.core.view.WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get email from intent
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("email");

        if (userEmail == null) {
            Toast.makeText(this, "Error: No email provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views
        otpBox1 = findViewById(R.id.otpBox1);
        otpBox2 = findViewById(R.id.otpBox2);
        otpBox3 = findViewById(R.id.otpBox3);
        otpBox4 = findViewById(R.id.otpBox4);
        verifyButton = findViewById(R.id.verifyButton);
        resendCode = findViewById(R.id.resendCode);
        emailDisplay = findViewById(R.id.emailDisplay);
        backButton = findViewById(R.id.backButton);

        // Display email
        emailDisplay.setText("Code sent to " + userEmail);

        // Setup OTP input boxes
        setupOTPInputs();

        // Start countdown timer
        startResendCountdown();

        // Handle Verify button
        verifyButton.setOnClickListener(v -> verifyOTP());

        // Handle Resend Code
        resendCode.setOnClickListener(v -> {
            if (System.currentTimeMillis() > resendTimer) {
                resendPasswordResetCode();
            } else {
                Toast.makeText(PasswordResetVerification.this,
                        "Please wait before requesting a new code",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Back button
        backButton.setOnClickListener(v -> {
            finish();
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

        // Disable verify button
        verifyButton.setEnabled(false);
        verifyButton.setText("Verifying...");

        // Verify OTP from Firestore
        db.collection("password_reset_codes").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedCode = documentSnapshot.getString("code");
                        Long timestamp = documentSnapshot.getLong("timestamp");
                        Boolean used = documentSnapshot.getBoolean("used");

                        // Check if code is valid
                        long currentTime = System.currentTimeMillis();
                        long expiryTime = 10 * 60 * 1000; // 10 minutes

                        if (used != null && used) {
                            Toast.makeText(PasswordResetVerification.this,
                                    "This code has already been used. Please request a new one.",
                                    Toast.LENGTH_LONG).show();
                            clearOTPBoxes();
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify Code");
                            return;
                        }

                        if (timestamp != null && (currentTime - timestamp) > expiryTime) {
                            Toast.makeText(PasswordResetVerification.this,
                                    "Code expired. Please request a new one.",
                                    Toast.LENGTH_LONG).show();
                            clearOTPBoxes();
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify Code");
                            return;
                        }

                        if (storedCode != null && storedCode.equals(enteredOTP)) {
                            // Mark as used
                            db.collection("password_reset_codes").document(userEmail)
                                    .update("used", true);

                            Toast.makeText(PasswordResetVerification.this,
                                    "Code verified! Enter your new password.",
                                    Toast.LENGTH_SHORT).show();

                            // Navigate to new password screen
                            Intent intent = new Intent(PasswordResetVerification.this, NewPassword.class);
                            intent.putExtra("email", userEmail);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(PasswordResetVerification.this,
                                    "Invalid code. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                            clearOTPBoxes();
                            verifyButton.setEnabled(true);
                            verifyButton.setText("Verify Code");
                        }
                    } else {
                        Toast.makeText(PasswordResetVerification.this,
                                "No verification code found. Please request a new one.",
                                Toast.LENGTH_LONG).show();
                        verifyButton.setEnabled(true);
                        verifyButton.setText("Verify Code");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PasswordResetVerification.this,
                            "Error verifying code: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    verifyButton.setEnabled(true);
                    verifyButton.setText("Verify Code");
                });
    }

    private void resendPasswordResetCode() {
        // Generate new OTP
        String verificationCode = String.format("%04d", new Random().nextInt(10000));

        // Store new OTP
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("code", verificationCode);
        otpData.put("timestamp", System.currentTimeMillis());
        otpData.put("email", userEmail);
        otpData.put("used", false);
        otpData.put("type", "password_reset");

        db.collection("password_reset_codes").document(userEmail)
                .set(otpData)
                .addOnSuccessListener(aVoid -> {
                    // Send email
                    sendResetEmailViaFirebase(userEmail, verificationCode);

                    // Set resend timer
                    resendTimer = System.currentTimeMillis() + RESEND_TIMEOUT;

                    // Start countdown
                    startResendCountdown();

                    Toast.makeText(PasswordResetVerification.this,
                            "New code sent to " + userEmail,
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PasswordResetVerification.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendResetEmailViaFirebase(String email, String code) {
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

        db.collection("mail").add(emailData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Email", "Password reset email queued successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("Email", "Failed to queue password reset email", e);
                });
    }

    private void clearOTPBoxes() {
        otpBox1.setText("");
        otpBox2.setText("");
        otpBox3.setText("");
        otpBox4.setText("");
        otpBox1.requestFocus();
    }

    private void startResendCountdown() {
        // Cancel existing timer if any
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Set initial timer
        resendTimer = System.currentTimeMillis() + RESEND_TIMEOUT;

        // Disable resend button and show countdown
        resendCode.setEnabled(false);
        resendCode.setAlpha(0.5f);

        countDownTimer = new CountDownTimer(RESEND_TIMEOUT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                resendCode.setText("RESEND CODE (" + secondsRemaining + "s)");
            }

            @Override
            public void onFinish() {
                resendCode.setText("RESEND NEW CODE");
                resendCode.setEnabled(true);
                resendCode.setAlpha(1.0f);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}