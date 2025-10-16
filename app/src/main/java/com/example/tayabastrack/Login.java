package com.example.tayabastrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout emailLayout;
    private Button signInButton;
    private TextView forgotPasswordText, registerButton;
    private CheckBox rememberMeCheckbox;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "TayabasTrackPrefs";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_REMEMBER = "remember_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        emailLayout = findViewById(R.id.emailLayout);
        signInButton = findViewById(R.id.signInButton);
        forgotPasswordText = findViewById(R.id.forgotPassword);
        registerButton = findViewById(R.id.registerButton);
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

        // Handle Forgot Password
        forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, forgotpassword.class);
            startActivity(intent);
        });

        // Handle Register
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, register.class);
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
            // Clear saved email if remember me is unchecked
            editor.putString(KEY_EMAIL, "");
            editor.putBoolean(KEY_REMEMBER, false);
        }
        editor.apply();
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save email if remember me is checked
                        if (rememberMeCheckbox.isChecked()) {
                            saveEmail(email, true);
                        } else {
                            saveEmail("", false);
                        }

                        Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        // Get the current user ID
                        String userId = mAuth.getCurrentUser().getUid();

                        // Check user status from Firestore
                        checkUserStatus(userId);
                    } else {
                        Toast.makeText(Login.this, "Login Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserStatus(String userId) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");

                        if ("pending".equals(status)) {
                            // User is pending - go to verification screen
                            Toast.makeText(Login.this, "Your account is pending verification. Please check your email.",
                                    Toast.LENGTH_LONG).show();

                            String email = documentSnapshot.getString("email");
                            Intent intent = new Intent(Login.this, Verification.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else if ("approved".equals(status) || "active".equals(status)) {
                            // User is approved or active - go to dashboard
                            Intent intent = new Intent(Login.this, dashboard.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Any other status
                            Toast.makeText(Login.this, "Account status: " + status, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Login.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Login.this, "Error checking user status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}