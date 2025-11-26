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
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private CheckBox termsCheckBox, noMiddleNameCheckBox;
    private Button registerButton;
    private FrameLayout uploadImageButton;
    private ImageView profileImageView;
    private ImageButton backButton;
    private TextInputEditText emailField, passwordField, confirmPasswordField;
    private TextInputEditText firstNameField, middleNameField, surnameField, contactNumberField;
    private AutoCompleteTextView barangaySpinner, positionSpinner, suffixSpinner;
    private FirebaseAuth auth;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 102;

    private Uri imageUri;
    private Bitmap capturedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

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
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        // Initialize Views
        initializeViews();
        setupDropdowns();
        setupEventListeners();
        setupTermsAndConditions();
    }

    private void initializeViews() {
        termsCheckBox = findViewById(R.id.termsCheckBox);
        registerButton = findViewById(R.id.registerButton);
        uploadImageButton = findViewById(R.id.UploadID);
        profileImageView = findViewById(R.id.ID);
        backButton = findViewById(R.id.backButton);

        firstNameField = findViewById(R.id.fname);
        middleNameField = findViewById(R.id.mname);
        surnameField = findViewById(R.id.sname);
        contactNumberField = findViewById(R.id.contactNumber);
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        confirmPasswordField = findViewById(R.id.confirmpassword);

        noMiddleNameCheckBox = findViewById(R.id.no_mname);
        barangaySpinner = findViewById(R.id.spinnerBarangay);
        positionSpinner = findViewById(R.id.spinnerPosition);
        suffixSpinner = findViewById(R.id.spinnerSuffix);

        registerButton.setEnabled(false);
        registerButton.setAlpha(0.5f);

        setupNameFilters();
    }

    private void setupDropdowns() {
        String[] suffixes = getResources().getStringArray(R.array.name_suffixes);
        ArrayAdapter<String> suffixAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, suffixes);
        suffixSpinner.setAdapter(suffixAdapter);
        suffixSpinner.setText(suffixes[0], false);

        String[] barangays = getResources().getStringArray(R.array.tayabas_barangays);
        ArrayAdapter<String> barangayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, barangays);
        barangaySpinner.setAdapter(barangayAdapter);
        barangaySpinner.setText(barangays[0], false);

        String[] positions = getResources().getStringArray(R.array.barangay_positions);
        ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, positions);
        positionSpinner.setAdapter(positionAdapter);
        positionSpinner.setText(positions[0], false);
    }

    private void setupNameFilters() {
        TextWatcher nameWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                String filtered = text.replaceAll("[^a-zA-Z ]", "");
                if (!text.equals(filtered)) {
                    s.replace(0, s.length(), filtered);
                }
            }
        };

        firstNameField.addTextChangedListener(nameWatcher);
        middleNameField.addTextChangedListener(nameWatcher);
        surnameField.addTextChangedListener(nameWatcher);

        contactNumberField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                String filtered = text.replaceAll("[^0-9]", "");
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
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(register.this, Login.class));
            finish();
        });

        noMiddleNameCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            middleNameField.setEnabled(!isChecked);
            if (isChecked) {
                middleNameField.setText("");
            }
        });

        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerButton.setEnabled(isChecked);
            registerButton.setAlpha(isChecked ? 1f : 0.5f);
        });

        registerButton.setOnClickListener(v -> registerUser());
        uploadImageButton.setOnClickListener(v -> showImagePickerDialog());
    }

    private void setupTermsAndConditions() {
        String text = "I agree to the Terms and Conditions";
        SpannableString spannableString = new SpannableString(text);
        int start = text.indexOf("Terms and Conditions");
        int end = start + "Terms and Conditions".length();

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

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("^9\\d{9}$");
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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

    private void registerUser() {
        String firstName = firstNameField.getText().toString().trim();
        String middleName = middleNameField.getText().toString().trim();
        String surname = surnameField.getText().toString().trim();
        String suffix = suffixSpinner.getText().toString();
        String position = positionSpinner.getText().toString();
        String contactNumber = contactNumberField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();
        String barangay = barangaySpinner.getText().toString();

        // Validation
        if (firstName.isEmpty() || surname.isEmpty() ||
                contactNumber.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (position.equals("Select Position") || position.isEmpty()) {
            Toast.makeText(this, "Please select a position", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!noMiddleNameCheckBox.isChecked() && middleName.isEmpty()) {
            Toast.makeText(this, "Please enter middle name or check 'I don't have a middle name'",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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
        registerButton.setText("Processing...");

        // Build full name
        String fullName = firstName +
                (noMiddleNameCheckBox.isChecked() ? "" : " " + middleName) +
                " " + surname;

        if (!suffix.equals("None") && !suffix.isEmpty()) {
            fullName += " " + suffix;
        }

        String fullPhoneNumber = "+63" + contactNumber;

        // Prepare user data to pass to verification activity
        UserRegistrationData userData = new UserRegistrationData(
                email,
                password,
                fullName,
                firstName,
                middleName,
                surname,
                suffix,
                position,
                barangay,
                fullPhoneNumber,
                imageUri,
                capturedBitmap
        );

        // Go to Email Verification WITHOUT creating account yet
        try {
            Intent intent = new Intent(register.this, Email_Verification.class);
            intent.putExtra("userData", userData);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("Registration", "Error navigating to Email_Verification", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            registerButton.setEnabled(true);
            registerButton.setText("Register");
        }
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

    // Data class to pass registration info to Email Verification
    public static class UserRegistrationData implements Parcelable {
        public String email;
        public String password;
        public String fullName;
        public String firstName;
        public String middleName;
        public String surname;
        public String suffix;
        public String position;
        public String barangay;
        public String phoneNumber;
        public String imageUriString;
        public byte[] imageBytes;

        public UserRegistrationData(String email, String password, String fullName,
                                    String firstName, String middleName, String surname,
                                    String suffix, String position, String barangay,
                                    String phoneNumber, Uri imageUri, Bitmap capturedBitmap) {
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.firstName = firstName;
            this.middleName = middleName;
            this.surname = surname;
            this.suffix = suffix;
            this.position = position;
            this.barangay = barangay;
            this.phoneNumber = phoneNumber;

            this.imageUriString = imageUri != null ? imageUri.toString() : null;

            if (capturedBitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                this.imageBytes = baos.toByteArray();
            } else {
                this.imageBytes = null;
            }
        }

        protected UserRegistrationData(Parcel in) {
            email = in.readString();
            password = in.readString();
            fullName = in.readString();
            firstName = in.readString();
            middleName = in.readString();
            surname = in.readString();
            suffix = in.readString();
            position = in.readString();
            barangay = in.readString();
            phoneNumber = in.readString();
            imageUriString = in.readString();
            imageBytes = in.createByteArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(email);
            dest.writeString(password);
            dest.writeString(fullName);
            dest.writeString(firstName);
            dest.writeString(middleName);
            dest.writeString(surname);
            dest.writeString(suffix);
            dest.writeString(position);
            dest.writeString(barangay);
            dest.writeString(phoneNumber);
            dest.writeString(imageUriString);
            dest.writeByteArray(imageBytes);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<UserRegistrationData> CREATOR = new Creator<UserRegistrationData>() {
            @Override
            public UserRegistrationData createFromParcel(Parcel in) {
                return new UserRegistrationData(in);
            }

            @Override
            public UserRegistrationData[] newArray(int size) {
                return new UserRegistrationData[size];
            }
        };

        public Uri getImageUri() {
            return imageUriString != null ? Uri.parse(imageUriString) : null;
        }

        public Bitmap getCapturedBitmap() {
            if (imageBytes != null && imageBytes.length > 0) {
                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            }
            return null;
        }
    }
}