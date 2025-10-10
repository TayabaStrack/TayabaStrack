package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cardview.widget.CardView;

// Network connectivity imports
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

// Google Maps imports
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

// Firestore imports
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Blob;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.ProgressDialog;
import android.util.Log;

public class submitreport extends AppCompatActivity implements OnMapReadyCallback {

    // Existing variables
    private EditText description, width, height, involved;
    private Spinner spinnerBarangay;
    private ImageView previewImage;
    private FrameLayout btnUpload;
    private Button btnSubmit;
    private ImageButton backButton;
    private ScrollView scrollView;
    private CardView mapContainer;

    // Map-related variables
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedLocationMarker;
    private LatLng selectedLocation;
    private TextView selectedLocationText;
    private Button btnCurrentLocation;

    // Checkboxes for age groups
    private CheckBox checkbox_0_10, checkbox_11_18, checkbox_19_30, checkbox_31_59, checkbox_60_plus;

    // Firestore
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    private Uri imageUri = null;
    private Bitmap capturedImageBitmap = null;
    private byte[] imageBytes = null;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 200;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 201;
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB max for Firestore blob

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitreport);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        initializeFirebase();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        initializeViews();

        // Setup map touch handling (must be called after views are initialized)
        setupMapTouchHandling();

        // Initialize map
        initializeMap();

        // Set up event listeners
        setupEventListeners();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting report...");
        progressDialog.setCancelable(false);

        // Sign in anonymously if no user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initializeViews() {
        // Existing views
        description = findViewById(R.id.description);
        width = findViewById(R.id.width);
        height = findViewById(R.id.height);
        involved = findViewById(R.id.involed);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        previewImage = findViewById(R.id.previewImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnSubmit = findViewById(R.id.btnSubmit);
        backButton = findViewById(R.id.backButton);

        // ScrollView and Map container
        scrollView = findViewById(R.id.scrollContent);
        mapContainer = findViewById(R.id.mapContainer);

        // Map-related views
        selectedLocationText = findViewById(R.id.selectedLocationText);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        // Checkboxes
        checkbox_0_10 = findViewById(R.id.checkbox_0_10);
        checkbox_11_18 = findViewById(R.id.checkbox_11_18);
        checkbox_19_30 = findViewById(R.id.checkbox_19_30);
        checkbox_31_59 = findViewById(R.id.checkbox_31_59);
        checkbox_60_plus = findViewById(R.id.checkbox_60_plus);
    }

    private void setupMapTouchHandling() {
        if (mapContainer != null && scrollView != null) {
            mapContainer.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        // Disable parent ScrollView when touching map
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Re-enable parent ScrollView
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false; // Allow the map to handle the touch event
            });
        }
    }

    private void setupEventListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(submitreport.this, dashboard.class);
            startActivity(intent);
            finish();
        });

        // Upload image button
        btnUpload.setOnClickListener(v -> showImagePickerDialog());

        // Submit report
        btnSubmit.setOnClickListener(v -> validateAndSubmit());

        // Current location button
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maplocation);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set default location to Tayabas, Quezon
        LatLng tayabas = new LatLng(14.0167, 121.5931);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tayabas, 13));

        // Enable all gesture controls explicitly
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // We have our own button

        // Set minimum and maximum zoom levels
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(20.0f);

        // Enable location if permission is granted
        enableMyLocation();

        // Set map click listener
        mMap.setOnMapClickListener(latLng -> {
            // Clear previous marker
            if (selectedLocationMarker != null) {
                selectedLocationMarker.remove();
            }

            // Add new marker
            selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));

            // Store selected location
            selectedLocation = latLng;

            // Update location text
            updateLocationText(latLng);
        });

        // Set marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        // Add camera change listener to handle zoom changes
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                // User is interacting with map, disable ScrollView
                if (scrollView != null) {
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
            }
        });

        mMap.setOnCameraIdleListener(() -> {
            // Camera stopped moving, re-enable ScrollView
            if (scrollView != null) {
                scrollView.requestDisallowInterceptTouchEvent(false);
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            // Request permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Clear previous marker
                        if (selectedLocationMarker != null) {
                            selectedLocationMarker.remove();
                        }

                        // Add marker at current location
                        selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Current Location"));

                        // Move camera to current location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));

                        // Store selected location
                        selectedLocation = currentLocation;

                        // Update location text
                        updateLocationText(currentLocation);
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLocationText(LatLng latLng) {
        String locationText = String.format("Lat: %.6f, Lng: %.6f",
                latLng.latitude, latLng.longitude);
        selectedLocationText.setText(locationText);
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

        Log.d("SubmitReport", "Image compressed to " + baos.toByteArray().length + " bytes with quality " + quality);
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

            if (bitmap == null) {
                Log.e("SubmitReport", "Failed to decode bitmap from URI");
                return null;
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
                Log.d("SubmitReport", "Bitmap resized to " + newWidth + "x" + newHeight);
            }

            return bitmapToByteArray(bitmap);
        } catch (Exception e) {
            Log.e("SubmitReport", "Error converting URI to byte array", e);
            return null;
        }
    }

    // Show options for upload (Camera or Gallery)
    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Upload Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else {
                            openCamera();
                        }
                    } else { // Gallery
                        openGallery();
                    }
                }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Handle results from camera/gallery with better error handling
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                try {
                    imageUri = data.getData();
                    capturedImageBitmap = null; // Clear bitmap if using URI

                    // Validate the URI by trying to open it
                    getContentResolver().openInputStream(imageUri).close();

                    previewImage.setImageURI(imageUri);
                    previewImage.setVisibility(ImageView.VISIBLE);
                    Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    imageUri = null;
                    Toast.makeText(this, "Failed to load selected image. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null) {
                try {
                    Object photoData = data.getExtras().get("data");
                    if (photoData instanceof Bitmap) {
                        capturedImageBitmap = (Bitmap) photoData;
                        imageUri = null; // Clear URI if using bitmap

                        if (capturedImageBitmap != null && !capturedImageBitmap.isRecycled()) {
                            previewImage.setImageBitmap(capturedImageBitmap);
                            previewImage.setVisibility(ImageView.VISIBLE);
                            Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            throw new Exception("Invalid bitmap data");
                        }
                    } else {
                        throw new Exception("No image data received");
                    }
                } catch (Exception e) {
                    capturedImageBitmap = null;
                    Toast.makeText(this, "Failed to capture photo. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No image selected or captured", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Operation cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle permission results
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
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission is required to use this feature",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper methods for location validation
    public LatLng getSelectedLocation() {
        return selectedLocation;
    }

    public boolean isLocationSelected() {
        return selectedLocation != null;
    }

    // Network connectivity check
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Get selected age groups
    private String getSelectedAgeGroups() {
        StringBuilder ageGroups = new StringBuilder();

        if (checkbox_0_10.isChecked()) ageGroups.append("0-10, ");
        if (checkbox_11_18.isChecked()) ageGroups.append("11-18, ");
        if (checkbox_19_30.isChecked()) ageGroups.append("19-30, ");
        if (checkbox_31_59.isChecked()) ageGroups.append("31-59, ");
        if (checkbox_60_plus.isChecked()) ageGroups.append("60+, ");

        if (ageGroups.length() > 0) {
            // Remove trailing comma and space
            ageGroups.setLength(ageGroups.length() - 2);
        }

        return ageGroups.toString();
    }

    // Validate fields and submit to Firestore
    private void validateAndSubmit() {
        String desc = description.getText().toString().trim();
        String w = width.getText().toString().trim();
        String h = height.getText().toString().trim();
        String inv = involved.getText().toString().trim();
        String ageGroups = getSelectedAgeGroups();

        boolean isValid = true;

        // Clear all previous errors
        description.setError(null);
        width.setError(null);
        height.setError(null);
        involved.setError(null);

        // Description validation (REQUIRED)
        if (desc.isEmpty()) {
            description.setError("Description is required");
            if (isValid) {
                description.requestFocus();
                isValid = false;
            }
        } else if (desc.length() < 10) {
            description.setError("Description must be at least 10 characters");
            if (isValid) {
                description.requestFocus();
                isValid = false;
            }
        }

        // Width validation (REQUIRED)
        if (w.isEmpty()) {
            width.setError("Width is required");
            if (isValid) {
                width.requestFocus();
                isValid = false;
            }
        } else {
            try {
                double widthValue = Double.parseDouble(w);
                if (widthValue <= 0) {
                    width.setError("Width must be greater than 0");
                    if (isValid) {
                        width.requestFocus();
                        isValid = false;
                    }
                }
            } catch (NumberFormatException e) {
                width.setError("Please enter a valid number");
                if (isValid) {
                    width.requestFocus();
                    isValid = false;
                }
            }
        }

        // Height validation (REQUIRED)
        if (h.isEmpty()) {
            height.setError("Height is required");
            if (isValid) {
                height.requestFocus();
                isValid = false;
            }
        } else {
            try {
                double heightValue = Double.parseDouble(h);
                if (heightValue <= 0) {
                    height.setError("Height must be greater than 0");
                    if (isValid) {
                        height.requestFocus();
                        isValid = false;
                    }
                }
            } catch (NumberFormatException e) {
                height.setError("Please enter a valid number");
                if (isValid) {
                    height.requestFocus();
                    isValid = false;
                }
            }
        }

        // People Involved validation (REQUIRED)
        if (inv.isEmpty()) {
            involved.setError("Number of people involved is required");
            if (isValid) {
                involved.requestFocus();
                isValid = false;
            }
        } else {
            try {
                int peopleCount = Integer.parseInt(inv);
                if (peopleCount <= 0) {
                    involved.setError("Number of people must be greater than 0");
                    if (isValid) {
                        involved.requestFocus();
                        isValid = false;
                    }
                }
            } catch (NumberFormatException e) {
                involved.setError("Please enter a valid number");
                if (isValid) {
                    involved.requestFocus();
                    isValid = false;
                }
            }
        }

        // Age group validation (REQUIRED)
        if (ageGroups.isEmpty()) {
            Toast.makeText(this, "Please select at least one affected age group", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Location validation (REQUIRED)
        if (!isLocationSelected()) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Barangay validation (REQUIRED)
        if (spinnerBarangay.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a Barangay", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Image validation (REQUIRED)
        if (imageUri == null && capturedImageBitmap == null) {
            Toast.makeText(this, "Please upload an image of the incident", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            // Validate the image data
            if (imageUri != null) {
                try {
                    getContentResolver().openInputStream(imageUri).close();
                } catch (Exception e) {
                    Toast.makeText(this, "Selected image is invalid. Please choose another image.", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            } else if (capturedImageBitmap != null) {
                if (capturedImageBitmap.isRecycled()) {
                    Toast.makeText(this, "Captured image is invalid. Please take another photo.", Toast.LENGTH_SHORT).show();
                    isValid = false;
                }
            }
        }

        // Stop validation if any field is invalid
        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields correctly", Toast.LENGTH_LONG).show();
            return;
        }

        // Check network connectivity before submission
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authenticating user, please wait...", Toast.LENGTH_SHORT).show();
            signInAnonymously();
            // Retry submission after a delay
            new android.os.Handler().postDelayed(() -> {
                if (mAuth.getCurrentUser() != null) {
                    submitToFirestore(desc, w, h, inv, ageGroups);
                } else {
                    Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }, 2000);
            return;
        }

        // If all validations pass, submit to Firestore
        submitToFirestore(desc, w, h, inv, ageGroups);
    }

    private void submitToFirestore(String desc, String widthStr, String heightStr, String involvedStr, String ageGroups) {
        progressDialog.show();

        // Prepare image data if available
        if (imageUri != null) {
            imageBytes = uriToByteArray(imageUri);
            if (imageBytes == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to process image. Please try another image.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (capturedImageBitmap != null) {
            imageBytes = bitmapToByteArray(capturedImageBitmap);
            if (imageBytes == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to process captured photo. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : "anonymous";

        // Fetch user data from Firestore first
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Get user information
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String userEmail = documentSnapshot.getString("email");
                    String userContact = documentSnapshot.getString("contact");
                    String userBarangay = documentSnapshot.getString("barangay");
                    String position = documentSnapshot.getString("position");

                    // Generate unique report ID
                    String reportId = UUID.randomUUID().toString();

                    // Get current timestamp
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    // Get location coordinates
                    LatLng location = getSelectedLocation();

                    // Create report data map
                    Map<String, Object> reportData = new HashMap<>();
                    reportData.put("reportId", reportId);
                    reportData.put("userId", userId);

                    // Add user information to report
                    if (firstName != null) reportData.put("firstName", firstName);
                    if (lastName != null) reportData.put("lastName", lastName);
                    if (userEmail != null) reportData.put("email", userEmail);
                    if (userContact != null) reportData.put("contact", userContact);
                    if (userBarangay != null) reportData.put("userBarangay", userBarangay);
                    if (position != null) reportData.put("position", position);

                    reportData.put("description", desc);
                    reportData.put("width", Double.parseDouble(widthStr));
                    reportData.put("height", Double.parseDouble(heightStr));
                    reportData.put("peopleInvolved", Integer.parseInt(involvedStr));
                    reportData.put("affectedAgeGroups", ageGroups);
                    reportData.put("barangay", spinnerBarangay.getSelectedItem().toString());
                    reportData.put("latitude", location.latitude);
                    reportData.put("longitude", location.longitude);
                    reportData.put("timestamp", timestamp);
                    reportData.put("status", "pending");
                    reportData.put("createdAt", new Date());

                    // Add image as Blob if available
                    if (imageBytes != null) {
                        Blob imageBlob = Blob.fromBytes(imageBytes);
                        reportData.put("incidentImage", imageBlob);
                        Log.d("SubmitReport", "Image added as Blob, size: " + imageBytes.length + " bytes");
                    }

                    // Save report to Firestore
                    saveReportToFirestore(reportData);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("SubmitReport", "Failed to fetch user data", e);
                    Toast.makeText(this, "Failed to fetch user information", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveReportToFirestore(Map<String, Object> reportData) {
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String reportId = (String) reportData.get("reportId");

        // Save report under users/{userId}/reports/{reportId}
        db.collection("users")
                .document(userId)
                .collection("reports")
                .document(reportId)
                .set(reportData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SubmitReport", "Report saved to user collection successfully");
                    // Also save a copy in global reports collection for admin access
                    saveToGlobalReports(reportData);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("SubmitReport", "Failed to save report to user collection", e);
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToGlobalReports(Map<String, Object> reportData) {
        String reportId = (String) reportData.get("reportId");

        // Save to global reports collection for admin dashboard
        db.collection("reports")
                .document(reportId)
                .set(reportData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Log.d("SubmitReport", "Report saved to global collection successfully");
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();

                    // Clear form after successful submission
                    clearForm();

                    // After successful submission, go back to dashboard
                    Intent intent = new Intent(submitreport.this, dashboard.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("SubmitReport", "Failed to save report to global collection", e);
                    // Even if global save fails, user report was saved
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();

                    // Clear form
                    clearForm();

                    // After successful submission, go back to dashboard
                    Intent intent = new Intent(submitreport.this, dashboard.class);
                    startActivity(intent);
                    finish();
                });
    }

    private void clearForm() {
        // Clear text fields
        description.setText("");
        width.setText("");
        height.setText("");
        involved.setText("");

        // Clear checkboxes
        checkbox_0_10.setChecked(false);
        checkbox_11_18.setChecked(false);
        checkbox_19_30.setChecked(false);
        checkbox_31_59.setChecked(false);
        checkbox_60_plus.setChecked(false);

        // Clear image
        imageUri = null;
        capturedImageBitmap = null;
        imageBytes = null;
        previewImage.setVisibility(ImageView.GONE);

        // Clear map marker
        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
            selectedLocationMarker = null;
        }
        selectedLocation = null;
        selectedLocationText.setText("Tap on map to select location");

        // Reset spinner
        spinnerBarangay.setSelection(0);
    }

    @Override
    public void onBackPressed() {
        // Also go back to dashboard if physical back button is pressed
        super.onBackPressed();
        Intent intent = new Intent(submitreport.this, dashboard.class);
        startActivity(intent);
        finish();
    }
}