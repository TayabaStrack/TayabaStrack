package com.example.tayabastrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class Email_Verification extends AppCompatActivity {

    private EditText otpBox1, otpBox2, otpBox3, otpBox4;
    private Button verifyButton, cancelButton;
    private TextView resendCode;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private String userEmail;
    private String verificationCode;
    private long resendTimer = 0;
    private static final long RESEND_TIMEOUT = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Get email from intent
        userEmail = getIntent().getStringExtra("email");

        // Initialize views
        otpBox1 = findViewById(R.id.otpBox1);
        otpBox2 = findViewById(R.id.otpBox2);
        otpBox3 = findViewById(R.id.otpBox3);
        otpBox4 = findViewById(R.id.otpBox4);
        verifyButton = findViewById(R.id.verifyButton);
        cancelButton = findViewById(R.id.cancelButton);
        resendCode = findViewById(R.id.resendCode);
        backButton = findViewById(R.id.backButton);

        // Generate and send OTP on activity creation
        sendOTPEmail();

        // Setup OTP input boxes to auto-move to next box
        setupOTPInputs();

        // Handle Verify button
        verifyButton.setOnClickListener(v -> verifyOTP());

        // Handle Cancel button
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        // Handle Resend Code
        resendCode.setOnClickListener(v -> {
            if (System.currentTimeMillis() > resendTimer) {
                sendOTPEmail();
            } else {
                Toast.makeText(Email_Verification.this,
                        "Please wait before requesting a new code",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Back button
        backButton.setOnClickListener(v -> finish());
    }

    private void sendOTPEmail() {
        // Generate random 4-digit OTP
        verificationCode = String.format("%04d", (int)(Math.random() * 10000));

        // Send email with OTP using Firebase Realtime Database or custom backend
        sendEmailOTP(userEmail, verificationCode);

        // Set resend timer
        resendTimer = System.currentTimeMillis() + RESEND_TIMEOUT;

        Toast.makeText(this, "OTP sent to " + userEmail, Toast.LENGTH_SHORT).show();
    }

    private void sendEmailOTP(String email, String code) {
        // Option 1: Using Firebase Cloud Functions or Custom Backend
        // You'll need to call your backend API to send the email

        // Example: Using Retrofit or Volley to call your backend
        // sendOTPViaBackend(email, code);

        // Option 2: If using Firebase Email/Password with custom logic
        // Store the OTP temporarily (in Firestore or Realtime DB) with expiry
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("otpCodes").document(email)
                .set(new OTPData(code, System.currentTimeMillis()))
                .addOnSuccessListener(aVoid -> {
                    // OTP stored in database
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Email_Verification.this,
                            "Error sending OTP: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupOTPInputs() {
        otpBox1.setOnKeyListener((v, keyCode, event) -> {
            if (otpBox1.getText().length() == 1) {
                otpBox2.requestFocus();
            }
            return false;
        });

        otpBox2.setOnKeyListener((v, keyCode, event) -> {
            if (otpBox2.getText().length() == 1) {
                otpBox3.requestFocus();
            }
            return false;
        });

        otpBox3.setOnKeyListener((v, keyCode, event) -> {
            if (otpBox3.getText().length() == 1) {
                otpBox4.requestFocus();
            }
            return false;
        });
    }

    private void verifyOTP() {
        String otp = otpBox1.getText().toString() +
                otpBox2.getText().toString() +
                otpBox3.getText().toString() +
                otpBox4.getText().toString();

        if (otp.length() < 4) {
            Toast.makeText(this, "Please enter all 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.equals(verificationCode)) {
            // OTP is correct
            Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();

            // Go to Dashboard or next activity
            Intent intent = new Intent(Email_Verification.this, dashboard.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
            clearOTPBoxes();
        }
    }

    private void clearOTPBoxes() {
        otpBox1.setText("");
        otpBox2.setText("");
        otpBox3.setText("");
        otpBox4.setText("");
        otpBox1.requestFocus();
    }

    // Helper class for OTP data
    public static class OTPData {
        public String code;
        public long timestamp;

        public OTPData(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }
    }
}