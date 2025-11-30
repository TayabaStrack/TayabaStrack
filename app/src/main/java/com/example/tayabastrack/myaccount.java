package com.example.tayabastrack;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class myaccount extends AppCompatActivity {

    private TextView fullNameText, positionText;
    private EditText etFirstName, etMiddleName, etSurname, etEmail, etPosition, etPhoneNumber;
    private Spinner spinnerBarangay;
    private Button btnEditProfile, btnCancel, btnSaveChanges;
    private LinearLayout buttonLayout;
    private ImageButton backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    private boolean isEditMode = false;

    // Store original values for cancel functionality
    private String originalFirstName, originalMiddleName, originalSurname;
    private String originalPhoneNumber, originalBarangay;

    private String[] barangayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_myaccount);

        // Set status bar color to match layout background and use dark icons for contrast
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom)
            );

            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserData();
        } else {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(myaccount.this, Login.class));
            finish();
            return;
        }

        // Setup event listeners
        setupEventListeners();
        setupNavigationListeners();
    }

    private void initializeViews() {
        fullNameText = findViewById(R.id.fullNameText);
        positionText = findViewById(R.id.positionText);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etPosition = findViewById(R.id.etPosition);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnCancel = findViewById(R.id.btnCancel);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        buttonLayout = findViewById(R.id.buttonLayout);
        backButton = findViewById(R.id.backButton);

        // Get barangay list from resources
        barangayList = getResources().getStringArray(R.array.tayabas_barangays);

        // Spinner is already populated via XML with android:entries
        spinnerBarangay.setEnabled(false);
    }

    private void loadUserData() {
        fullNameText.setText("Loading...");
        positionText.setText("");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            etEmail.setText(currentUser.getEmail());
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String middleName = documentSnapshot.getString("middleName");
                        String surname = documentSnapshot.getString("surname");
                        String position = documentSnapshot.getString("position");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String barangay = documentSnapshot.getString("barangay");
                        String fullName = documentSnapshot.getString("fullName");

                        // Store original values
                        originalFirstName = firstName != null ? firstName : "";
                        originalMiddleName = middleName != null ? middleName : "";
                        originalSurname = surname != null ? surname : "";
                        originalPhoneNumber = phoneNumber != null ? phoneNumber : "";
                        originalBarangay = barangay != null ? barangay : "";

                        // Update EditTexts
                        if (firstName != null) etFirstName.setText(firstName);
                        if (middleName != null) etMiddleName.setText(middleName);
                        if (surname != null) etSurname.setText(surname);
                        if (position != null) etPosition.setText(position);
                        if (phoneNumber != null) etPhoneNumber.setText(phoneNumber);

                        // Set barangay spinner
                        if (barangay != null && !barangay.isEmpty()) {
                            for (int i = 0; i < barangayList.length; i++) {
                                if (barangayList[i].equals(barangay)) {
                                    spinnerBarangay.setSelection(i);
                                    break;
                                }
                            }
                        }

                        // Update header display
                        if (fullName != null && !fullName.isEmpty()) {
                            fullNameText.setText(fullName);
                        } else {
                            String displayName = buildFullName(firstName, middleName, surname);
                            fullNameText.setText(displayName.isEmpty() ? "No name available" : displayName);
                        }

                        if (position != null && !position.isEmpty()) {
                            positionText.setText(position);
                        } else {
                            positionText.setText("No position set");
                        }

                        Log.d("MyAccount", "User data loaded successfully");
                    } else {
                        fullNameText.setText("User data not found");
                        positionText.setText("");
                        Log.w("MyAccount", "User document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    fullNameText.setText("Error loading data");
                    positionText.setText("");
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("MyAccount", "Error loading user data", e);
                });
    }

    private String buildFullName(String firstName, String middleName, String surname) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            name.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(middleName);
        }
        if (surname != null && !surname.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(surname);
        }
        return name.toString();
    }

    private void setupEventListeners() {
        backButton.setOnClickListener(v -> {
            if (isEditMode) {
                showDiscardChangesDialog();
            } else {
                startActivity(new Intent(myaccount.this, dashboard.class));
                finish();
            }
        });

        btnEditProfile.setOnClickListener(v -> enableEditMode());

        btnCancel.setOnClickListener(v -> showCancelDialog());

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void setupNavigationListeners() {
        ImageButton btnNavHome = findViewById(R.id.nav_home);
        ImageButton btnNavContacts = findViewById(R.id.nav_contacts);
        ImageButton btnNavSubmit = findViewById(R.id.nav_submit);
        ImageButton btnNavHistory = findViewById(R.id.nav_history);
        ImageButton btnNavProfile = findViewById(R.id.nav_profile);

        btnNavHome.setOnClickListener(v -> {
            if (isEditMode) {
                showDiscardChangesDialog();
            } else {
                startActivity(new Intent(this, dashboard.class));
                finish();
            }
        });

        btnNavProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show();
        });

        btnNavSubmit.setOnClickListener(v -> {
            if (isEditMode) {
                showDiscardChangesDialog();
            } else {
                startActivity(new Intent(this, submitreport.class));
                finish();
            }
        });

        btnNavContacts.setOnClickListener(v -> {
            if (isEditMode) {
                showDiscardChangesDialog();
            } else {
                startActivity(new Intent(this, contacts.class));
                finish();
            }
        });

        btnNavHistory.setOnClickListener(v -> {
            if (isEditMode) {
                showDiscardChangesDialog();
            } else {
                startActivity(new Intent(this, myreports.class));
                finish();
            }
        });
    }

    private void enableEditMode() {
        isEditMode = true;

        // Enable editing for editable fields
        setFieldEditable(etFirstName, true);
        setFieldEditable(etMiddleName, true);
        setFieldEditable(etSurname, true);
        setFieldEditable(etPhoneNumber, true);

        // Enable spinner
        spinnerBarangay.setEnabled(true);

        // Show Cancel and Save buttons, hide Edit Profile button
        btnEditProfile.setVisibility(View.GONE);
        buttonLayout.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
    }

    private void disableEditMode() {
        isEditMode = false;

        // Disable editing
        setFieldEditable(etFirstName, false);
        setFieldEditable(etMiddleName, false);
        setFieldEditable(etSurname, false);
        setFieldEditable(etPhoneNumber, false);

        // Disable spinner
        spinnerBarangay.setEnabled(false);

        // Show Edit Profile button, hide Cancel and Save buttons
        btnEditProfile.setVisibility(View.VISIBLE);
        buttonLayout.setVisibility(View.GONE);
    }

    private void setFieldEditable(EditText editText, boolean editable) {
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setClickable(editable);
        editText.setCursorVisible(editable);
        editText.setLongClickable(editable);
    }

    private void showDiscardChangesDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("You have unsaved changes. Discard them?")
                .setPositiveButton("Discard", (d, w) -> {
                    restoreOriginalValues();
                    disableEditMode();
                })
                .setNegativeButton("Keep Editing", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getWindow().setBackgroundDrawableResource(R.color.blue_004aad);
            int white = ContextCompat.getColor(this, android.R.color.white);

            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) message.setTextColor(white);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(white);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(white);
        });

        dialog.show();
    }

    private void showCancelDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Discard changes?")
                .setPositiveButton("Yes", (d, w) -> {
                    restoreOriginalValues();
                    disableEditMode();
                    Toast.makeText(this, "Changes discarded", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getWindow().setBackgroundDrawableResource(R.color.blue_004aad);
            int white = ContextCompat.getColor(this, android.R.color.white);

            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) message.setTextColor(white);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(white);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(white);
        });

        dialog.show();
    }

    private void restoreOriginalValues() {
        etFirstName.setText(originalFirstName);
        etMiddleName.setText(originalMiddleName);
        etSurname.setText(originalSurname);
        etPhoneNumber.setText(originalPhoneNumber);

        // Restore barangay spinner
        if (originalBarangay != null && !originalBarangay.isEmpty()) {
            for (int i = 0; i < barangayList.length; i++) {
                if (barangayList[i].equals(originalBarangay)) {
                    spinnerBarangay.setSelection(i);
                    break;
                }
            }
        } else {
            spinnerBarangay.setSelection(0);
        }
    }

    private void saveChanges() {
        // Get updated values
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String barangay = spinnerBarangay.getSelectedItem().toString();

        // Validate required fields
        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }

        if (surname.isEmpty()) {
            etSurname.setError("Surname is required");
            etSurname.requestFocus();
            return;
        }

        // Validate barangay selection
        if (barangay.equals("Select Barangay")) {
            Toast.makeText(this, "Please select a barangay", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build full name
        String fullName = buildFullName(firstName, middleName, surname);

        // Create update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("middleName", middleName);
        updates.put("surname", surname);
        updates.put("fullName", fullName);
        updates.put("phoneNumber", phoneNumber);
        updates.put("barangay", barangay);

        // Show loading state
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        // Update Firestore
        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Update original values
                    originalFirstName = firstName;
                    originalMiddleName = middleName;
                    originalSurname = surname;
                    originalPhoneNumber = phoneNumber;
                    originalBarangay = barangay;

                    // Update header display
                    fullNameText.setText(fullName);

                    // Reset button
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");

                    // Disable edit mode
                    disableEditMode();

                    Log.d("MyAccount", "Profile updated successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Reset button
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");

                    Log.e("MyAccount", "Error updating profile", e);
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isEditMode) {
            showDiscardChangesDialog();
        } else {
            startActivity(new Intent(myaccount.this, dashboard.class));
            finish();
        }
    }
}