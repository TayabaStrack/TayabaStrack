package com.example.tayabastrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class Login extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout emailLayout;
    private Button signInButton;
    private TextView registerButton, forgotPasswordButton;
    private CheckBox rememberMeCheckbox;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "TayabasTrackPrefs";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String TAG = "Login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase first
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Show login screen
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        emailLayout = findViewById(R.id.emailLayout);
        signInButton = findViewById(R.id.signInButton);
        registerButton = findViewById(R.id.registerButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);

        // Load saved email if "Remember Me" was checked
        loadSavedEmail();

        // Handle Login button
        signInButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Handle Register button
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, register.class);
            startActivity(intent);
        });

        // Handle Forgot Password button
        forgotPasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, forgotpassword.class);
            startActivity(intent);
        });
    }

    private void loadSavedEmail() {
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (rememberMe) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            if (!savedEmail.isEmpty()) {
                emailEditText.setText(savedEmail);
                rememberMeCheckbox.setChecked(true);
            }
        }
    }

    private void saveEmail(String email, boolean rememberMe) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (rememberMe) {
            editor.putString(KEY_EMAIL, email);
            editor.putBoolean(KEY_REMEMBER, true);
        } else {
            editor.putString(KEY_EMAIL, "");
            editor.putBoolean(KEY_REMEMBER, false);
        }
        editor.apply();
    }

    private void loginUser(String email, String password) {
        signInButton.setEnabled(false);
        signInButton.setText("Logging in...");

        Log.d(TAG, "Attempting login");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful, checking user status");

                        // Save email if "Remember Me" is checked
                        saveEmail(email, rememberMeCheckbox.isChecked());

                        // Check user status before proceeding
                        checkUserStatus();
                    } else {
                        signInButton.setEnabled(true);
                        signInButton.setText("Login");

                        Log.e(TAG, "Login failed", task.getException());
                        Toast.makeText(Login.this, "Login Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ✅ NEW: Check if user status is "active"
    private void checkUserStatus() {
        String userId = mAuth.getCurrentUser().getUid();

        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    signInButton.setEnabled(true);
                    signInButton.setText("Login");

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String status = document.getString("status");

                            if ("active".equalsIgnoreCase(status)) {
                                Log.d(TAG, "User status is active, proceeding to dashboard");
                                goToDashboard();
                            } else {
                                Log.w(TAG, "User status is not active: " + status);
                                // Don't sign out - redirect to verification screen
                                goToVerification();
                            }
                        } else {
                            Log.e(TAG, "User document not found");
                            mAuth.signOut();
                            Toast.makeText(Login.this,
                                    "User data not found. Please contact support.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Failed to check user status", task.getException());
                        Toast.makeText(Login.this,
                                "Failed to verify account status. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ✅ NEW: Redirect to verification screen
    private void goToVerification() {
        Intent intent = new Intent(Login.this, Verification.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        try {
            NotificationHelper.initializeNotifications();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize notifications", e);
        }

        Intent intent = new Intent(Login.this, dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}