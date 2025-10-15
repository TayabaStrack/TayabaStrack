package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cardview.widget.CardView;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import android.app.ProgressDialog;
import android.util.Log;

public class submitreport extends AppCompatActivity implements OnMapReadyCallback {

    private EditText description, width, height;
    private Spinner spinnerBarangay;
    private ImageView previewImage;
    private FrameLayout btnUpload;
    private Button btnSubmit;
    private ImageButton backButton;
    private ScrollView scrollView;
    private CardView mapContainer;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker selectedLocationMarker;
    private LatLng selectedLocation;
    private TextView selectedLocationText;
    private Button btnCurrentLocation;

    private EditText searchLocation;
    private ImageButton btnSearch;
    private Geocoder geocoder;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private ProgressDialog progressDialog;

    private Uri imageUri = null;
    private Bitmap capturedImageBitmap = null;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 200;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitreport);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeFirebase();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        initializeViews();
        setupMapTouchHandling();
        initializeMap();
        setupEventListeners();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting report...");
        progressDialog.setCancelable(false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initializeViews() {
        description = findViewById(R.id.description);
        width = findViewById(R.id.width);
        height = findViewById(R.id.height);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        previewImage = findViewById(R.id.previewImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnSubmit = findViewById(R.id.btnSubmit);
        backButton = findViewById(R.id.backButton);

        scrollView = findViewById(R.id.scrollContent);
        mapContainer = findViewById(R.id.mapContainer);

        selectedLocationText = findViewById(R.id.selectedLocationText);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        searchLocation = findViewById(R.id.searchLocation);
        btnSearch = findViewById(R.id.btnSearch);
    }

    private void setupMapTouchHandling() {
        if (mapContainer != null && scrollView != null) {
            mapContainer.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            });
        }
    }

    private void setupEventListeners() {
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(submitreport.this, dashboard.class);
            startActivity(intent);
            finish();
        });

        btnUpload.setOnClickListener(v -> showImagePickerDialog());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnSearch.setOnClickListener(v -> searchLocation());

        searchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation();
                return true;
            }
            return false;
        });
    }

    private void searchLocation() {
        String locationQuery = searchLocation.getText().toString().trim();

        if (locationQuery.isEmpty()) {
            Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog searchProgress = new ProgressDialog(this);
        searchProgress.setMessage("Searching location...");
        searchProgress.setCancelable(false);
        searchProgress.show();

        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(
                        locationQuery + ", Tayabas, Quezon, Philippines",
                        10
                );

                runOnUiThread(() -> {
                    searchProgress.dismiss();

                    if (addresses != null && !addresses.isEmpty()) {
                        // Tayabas city boundaries (stricter)
                        double minLat = 14.00;
                        double maxLat = 14.06;
                        double minLng = 121.55;
                        double maxLng = 121.65;

                        Address validAddress = null;
                        for (Address address : addresses) {
                            double lat = address.getLatitude();
                            double lng = address.getLongitude();

                            Log.d("SearchLocation", "Found: " + address.getFeatureName() +
                                    " at Lat: " + lat + ", Lng: " + lng);

                            // Check if address is within Tayabas bounds
                            if (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng) {
                                validAddress = address;
                                Log.d("SearchLocation", "Valid address within Tayabas bounds");
                                break;
                            }
                        }

                        if (validAddress != null) {
                            LatLng location = new LatLng(validAddress.getLatitude(), validAddress.getLongitude());

                            if (selectedLocationMarker != null) {
                                selectedLocationMarker.remove();
                            }

                            selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title(validAddress.getFeatureName() != null ? validAddress.getFeatureName() : "Selected Location"));

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17));

                            selectedLocation = location;
                            updateLocationText(location);

                            Toast.makeText(submitreport.this, "Location found in Tayabas!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("SearchLocation", "No valid address found within Tayabas bounds");
                            Toast.makeText(submitreport.this, "Location not found in Tayabas. Please search for a street, barangay, or landmark within Tayabas city.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(submitreport.this, "Location not found. Try a different search term.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    searchProgress.dismiss();
                    Log.e("SearchLocation", "Error searching location", e);
                    Toast.makeText(submitreport.this, "Error searching location. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
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

        LatLng tayabas = new LatLng(14.0167, 121.5931);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tayabas, 13));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(20.0f);

        enableMyLocation();

        mMap.setOnMapClickListener(latLng -> {
            if (selectedLocationMarker != null) {
                selectedLocationMarker.remove();
            }

            selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));

            selectedLocation = latLng;
            updateLocationText(latLng);
        });

        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                if (scrollView != null) {
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
            }
        });

        mMap.setOnCameraIdleListener(() -> {
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

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        if (selectedLocationMarker != null) {
                            selectedLocationMarker.remove();
                        }

                        selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Current Location"));

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));

                        selectedLocation = currentLocation;
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
            Log.d("SubmitReport", "Bitmap resized to " + newWidth + "x" + newHeight);
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        Log.d("SubmitReport", "Image compressed to " + baos.toByteArray().length + " bytes with quality " + quality);
        return baos.toByteArray();
    }

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

            return bitmapToByteArray(bitmap);
        } catch (Exception e) {
            Log.e("SubmitReport", "Error converting URI to byte array", e);
            return null;
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Upload Image")
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
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                try {
                    imageUri = data.getData();
                    capturedImageBitmap = null;

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
                        imageUri = null;

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

    public LatLng getSelectedLocation() {
        return selectedLocation;
    }

    public boolean isLocationSelected() {
        return selectedLocation != null;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void validateAndSubmit() {
        String desc = description.getText().toString().trim();
        String w = width.getText().toString().trim();
        String h = height.getText().toString().trim();

        boolean isValid = true;

        description.setError(null);
        width.setError(null);
        height.setError(null);

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

        if (!isLocationSelected()) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerBarangay.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a Barangay", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (imageUri == null && capturedImageBitmap == null) {
            Toast.makeText(this, "Please upload an image of the incident", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
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

        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields correctly", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authenticating user, please wait...", Toast.LENGTH_SHORT).show();
            signInAnonymously();
            new android.os.Handler().postDelayed(() -> {
                if (mAuth.getCurrentUser() != null) {
                    submitToFirestore(desc, w, h);
                } else {
                    Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }, 2000);
            return;
        }

        submitToFirestore(desc, w, h);
    }

    private void submitToFirestore(String desc, String widthStr, String heightStr) {
        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String reportId = UUID.randomUUID().toString();

        uploadImageToStorage(reportId, userId, desc, widthStr, heightStr);
    }

    private void uploadImageToStorage(String reportId, String userId, String desc, String widthStr,
                                      String heightStr) {
        StorageReference imageRef = storageRef.child("report_images/" + userId + "/" + reportId + "/incident_image.jpg");

        byte[] imageBytes;
        if (imageUri != null) {
            imageBytes = uriToByteArray(imageUri);
        } else {
            imageBytes = bitmapToByteArray(capturedImageBitmap);
        }

        if (imageBytes == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SubmitReport", "Uploading image, size: " + imageBytes.length + " bytes");

        UploadTask uploadTask = imageRef.putBytes(imageBytes);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Log.d("SubmitReport", "Image uploaded successfully. URL: " + imageUrl);

                progressDialog.setMessage("Saving report...");

                saveReportData(reportId, userId, desc, widthStr, heightStr, imageUrl);
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e("SubmitReport", "Failed to get download URL", e);
                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Log.e("SubmitReport", "Failed to upload image", e);

            String errorMsg = "Failed to upload image: ";
            if (e.getMessage() != null && e.getMessage().contains("permission")) {
                errorMsg += "Permission denied. Please check Firebase Storage rules.";
            } else {
                errorMsg += e.getMessage();
            }

            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressDialog.setMessage("Uploading image: " + (int)progress + "%");
            Log.d("SubmitReport", "Upload progress: " + progress + "%");
        });
    }

    private void saveReportData(String reportId, String userId, String desc, String widthStr,
                                String heightStr, String imageUrl) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String userEmail = documentSnapshot.getString("email");
                    String userContact = documentSnapshot.getString("contact");
                    String userBarangay = documentSnapshot.getString("barangay");
                    String position = documentSnapshot.getString("position");

                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    LatLng location = getSelectedLocation();

                    Map<String, Object> reportData = new HashMap<>();
                    reportData.put("reportId", reportId);
                    reportData.put("userId", userId);

                    if (firstName != null) reportData.put("firstName", firstName);
                    if (lastName != null) reportData.put("lastName", lastName);
                    if (userEmail != null) reportData.put("email", userEmail);
                    if (userContact != null) reportData.put("contact", userContact);
                    if (userBarangay != null) reportData.put("userBarangay", userBarangay);
                    if (position != null) reportData.put("position", position);

                    reportData.put("description", desc);
                    reportData.put("width", Double.parseDouble(widthStr));
                    reportData.put("height", Double.parseDouble(heightStr));
                    reportData.put("barangay", spinnerBarangay.getSelectedItem().toString());
                    reportData.put("latitude", location.latitude);
                    reportData.put("longitude", location.longitude);
                    reportData.put("timestamp", timestamp);
                    reportData.put("status", "pending");
                    reportData.put("createdAt", new Date());
                    reportData.put("imageUrl", imageUrl);

                    saveReportToFirestore(reportData);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("SubmitReport", "Failed to fetch user data", e);
                    Toast.makeText(this, "Failed to fetch user information", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveReportToFirestore(Map<String, Object> reportData) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String reportId = (String) reportData.get("reportId");

        db.collection("users")
                .document(userId)
                .collection("reports")
                .document(reportId)
                .set(reportData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SubmitReport", "Report saved to user collection successfully");
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

        db.collection("reports")
                .document(reportId)
                .set(reportData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Log.d("SubmitReport", "Report saved to global collection successfully");
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();

                    clearForm();

                    Intent intent = new Intent(submitreport.this, dashboard.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("SubmitReport", "Failed to save report to global collection", e);
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();

                    clearForm();

                    Intent intent = new Intent(submitreport.this, dashboard.class);
                    startActivity(intent);
                    finish();
                });
    }

    private void clearForm() {
        description.setText("");
        width.setText("");
        height.setText("");
        searchLocation.setText("");

        imageUri = null;
        capturedImageBitmap = null;
        previewImage.setVisibility(ImageView.GONE);

        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
            selectedLocationMarker = null;
        }
        selectedLocation = null;
        selectedLocationText.setText("Tap on map to select location");

        spinnerBarangay.setSelection(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(submitreport.this, dashboard.class);
        startActivity(intent);
        finish();
    }
}