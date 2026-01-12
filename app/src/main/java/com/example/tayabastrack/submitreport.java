package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowInsetsControllerCompat;

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
import java.util.ArrayList;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.ViewGroup;
import android.view.LayoutInflater;

public class submitreport extends AppCompatActivity implements OnMapReadyCallback {

    private EditText description, width, height, depth;
    private Spinner spinnerBarangay, spinnerCategory, spinnerIssue;
    private LinearLayout imagesContainer;
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

    private List<ImageItem> imageItems = new ArrayList<>();
    private Location captureLocation = null;
    private int currentImageIndex = -1; // Track which image slot is being filled

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int CAMERA_PERMISSION_CODE = 200;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 201;
    private static final int MAX_IMAGES = 2;

    // Inner class to hold image data
    private static class ImageItem {
        Uri imageUri;
        Bitmap capturedImageBitmap;
        View imageViewContainer;
        int index;

        ImageItem(int index) {
            this.index = index;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_submitreport);
        // Set status bar color to match layout background and use dark icons for contrast
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#ffffff"));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);



        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Apply system bars padding (status bar, navigation bar)
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom)
            );

            return WindowInsetsCompat.CONSUMED;
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
        depth = findViewById(R.id.depth);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerIssue = findViewById(R.id.spinnerIssue);
        imagesContainer = findViewById(R.id.imagesContainer);
        btnUpload = findViewById(R.id.btnUpload);
        btnSubmit = findViewById(R.id.btnSubmit);
        backButton = findViewById(R.id.backButton);

        scrollView = findViewById(R.id.scrollContent);
        mapContainer = findViewById(R.id.mapContainer);

        selectedLocationText = findViewById(R.id.selectedLocationText);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        searchLocation = findViewById(R.id.searchLocation);
        btnSearch = findViewById(R.id.btnSearch);

        // Bottom Navigation Buttons
        ImageButton btnNavHome = findViewById(R.id.nav_home);
        ImageButton btnNavContacts = findViewById(R.id.nav_contacts);
        ImageButton btnNavSubmit = findViewById(R.id.nav_submit);
        ImageButton btnNavHistory = findViewById(R.id.nav_history);
        ImageButton btnNavProfile = findViewById(R.id.nav_profile);

        // Set click listeners for navigation
        btnNavHome.setOnClickListener(v -> {
            startActivity(new Intent(this, dashboard.class));
            finish();
        });

        btnNavContacts.setOnClickListener(v -> {
            startActivity(new Intent(this, contacts.class));
            finish();
        });

        btnNavSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Submit Report", Toast.LENGTH_SHORT).show();
        });

        btnNavHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, myreports.class));
            finish();
        });

        btnNavProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, myaccount.class));
            finish();
        });
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
                        locationQuery + ", Tayabas, Quezon, Philippines", 10);

                runOnUiThread(() -> {
                    searchProgress.dismiss();

                    if (addresses != null && !addresses.isEmpty()) {
                        double minLat = 14.00, maxLat = 14.06, minLng = 121.55, maxLng = 121.65;
                        Address validAddress = null;

                        for (Address address : addresses) {
                            double lat = address.getLatitude(), lng = address.getLongitude();
                            if (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng) {
                                validAddress = address;
                                break;
                            }
                        }

                        if (validAddress != null) {
                            LatLng location = new LatLng(validAddress.getLatitude(), validAddress.getLongitude());
                            if (selectedLocationMarker != null) selectedLocationMarker.remove();
                            selectedLocationMarker = mMap.addMarker(new MarkerOptions().position(location)
                                    .title(validAddress.getFeatureName() != null ? validAddress.getFeatureName() : "Selected Location"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17));
                            selectedLocation = location;
                            updateLocationText(location);
                            Toast.makeText(this, "Location found in Tayabas!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Location not found in Tayabas.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Location not found.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    searchProgress.dismiss();
                    Toast.makeText(this, "Error searching location.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maplocation);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng tayabas = new LatLng(14.0167, 121.5931);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tayabas, 13));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(20.0f);
        enableMyLocation();

        mMap.setOnMapClickListener(latLng -> {
            if (selectedLocationMarker != null) selectedLocationMarker.remove();
            selectedLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            selectedLocation = latLng;
            updateLocationText(latLng);
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (selectedLocationMarker != null) selectedLocationMarker.remove();
                selectedLocationMarker = mMap.addMarker(new MarkerOptions()
                        .position(currentLocation).title("Current Location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));
                selectedLocation = currentLocation;
                updateLocationText(currentLocation);
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLocationText(LatLng latLng) {
        selectedLocationText.setText(String.format("Lat: %.6f, Lng: %.6f", latLng.latitude, latLng.longitude));
    }

    // ✅ IMPROVED: Add geo-tag overlay with date, time, and coordinates
    private Bitmap addGeoTagOverlay(Bitmap bitmap, Location location, String userName) {
        if (bitmap == null) return null;

        try {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            // Get date & time
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            String currentTime = timeFormat.format(new Date());

            // Get address
            String addressLine = "Tayabas City, Quezon";
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String barangay = address.getSubLocality() != null ? address.getSubLocality() : "";
                    String city = address.getLocality() != null ? address.getLocality() : "Tayabas City";
                    addressLine = (!barangay.isEmpty() ? barangay + ", " : "") + city;
                }
            } catch (Exception e) {
                Log.e("GeoTag", "Error getting address", e);
            }

            // Coordinates
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            String latDir = lat >= 0 ? "N" : "S";
            String lngDir = lng >= 0 ? "E" : "W";
            String coordinates = String.format(Locale.US, "%.4f°%s, %.4f°%s",
                    Math.abs(lat), latDir, Math.abs(lng), lngDir);

            // 🖋️ Text Paint (larger, readable)
            float textSize = mutableBitmap.getWidth() * 0.04f; // doubled from 2% → 4% of image width
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(textSize);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setAntiAlias(true);
            textPaint.setShadowLayer(2, 2, 2, Color.BLACK);

            // 🔲 Background Paint
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.parseColor("#66000000")); // semi-transparent black
            bgPaint.setStyle(Paint.Style.FILL);

            // Lines to display
            String[] lines;
            if (userName != null && !userName.isEmpty()) {
                lines = new String[]{currentDate + " " + currentTime, addressLine, coordinates, userName};
            } else {
                lines = new String[]{currentDate + " " + currentTime, addressLine, coordinates};
            }

            int padding = (int) (textSize * 0.6f);
            float lineSpacing = textSize * 1.4f;
            float textBlockHeight = lines.length * lineSpacing + padding * 2;
            float textBlockWidth = 0;

            for (String line : lines) {
                textBlockWidth = Math.max(textBlockWidth, textPaint.measureText(line));
            }

            // Keep overlay around 1/3 width of image
            textBlockWidth = Math.min(textBlockWidth, mutableBitmap.getWidth() / 1.8f);

            // 🧭 Position — lower-left corner
            float left = padding;
            float bottom = mutableBitmap.getHeight() - padding;
            float top = bottom - textBlockHeight;
            float right = left + textBlockWidth + padding * 2;

            canvas.drawRect(left, top, right, bottom, bgPaint);

            // Draw text lines
            float x = left + padding;
            float y = top + padding + textSize;

            for (String line : lines) {
                if (textPaint.measureText(line) > textBlockWidth) {
                    line = line.substring(0, Math.min(line.length(), 30)) + "...";
                }
                canvas.drawText(line, x, y, textPaint);
                y += lineSpacing;
            }

            return mutableBitmap;

        } catch (Exception e) {
            Log.e("GeoTag", "Error adding geo-tag overlay", e);
            return bitmap;
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int maxWidth = 1920;  // Increased for better quality
        int maxHeight = 1920;

        if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
            float scale = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() * scale),
                    Math.round(bitmap.getHeight() * scale), true);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);  // Better quality
        return baos.toByteArray();
    }

    private byte[] uriToByteArray(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            return bitmap == null ? null : bitmapToByteArray(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    private void showImagePickerDialog() {
        if (imageItems.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Take Photo", "Choose from Gallery"};
        new android.app.AlertDialog.Builder(this).setTitle("Upload Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else openCamera();
                    } else openGallery();
                }).show();
    }

    private void openCamera() {
        // Get current location before opening camera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                captureLocation = location;
                startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST);
            });
        } else {
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                try {
                    Uri selectedUri = data.getData();
                    getContentResolver().openInputStream(selectedUri).close();
                    
                    ImageItem item = new ImageItem(imageItems.size());
                    item.imageUri = selectedUri;
                    item.capturedImageBitmap = null;
                    addImageToContainer(item);
                    Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load selected image.", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null) {
                try {
                    Object photoData = data.getExtras().get("data");
                    if (photoData instanceof Bitmap) {
                        Bitmap originalBitmap = (Bitmap) photoData;
                        Bitmap processedBitmap = originalBitmap;

                        // ✅ Add geo-tag overlay if location is available
                        if (captureLocation != null) {
                            String userName = "";
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                userName = currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "User";
                            }
                            processedBitmap = addGeoTagOverlay(originalBitmap, captureLocation, userName);
                            Toast.makeText(this, "Photo captured with geo-tag info", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Photo captured (no location data)", Toast.LENGTH_SHORT).show();
                        }

                        if (processedBitmap != null && !processedBitmap.isRecycled()) {
                            ImageItem item = new ImageItem(imageItems.size());
                            item.capturedImageBitmap = processedBitmap;
                            item.imageUri = null;
                            addImageToContainer(item);
                        } else throw new Exception("Invalid bitmap");
                    } else throw new Exception("No image data");
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to capture photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        currentImageIndex = -1;
    }

    private void addImageToContainer(ImageItem item) {
        // Create image view container
        FrameLayout imageContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                (int) (200 * getResources().getDisplayMetrics().density),
                (int) (200 * getResources().getDisplayMetrics().density)
        );
        containerParams.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
        imageContainer.setLayoutParams(containerParams);
        imageContainer.setBackgroundResource(R.drawable.image_border_outline);

        // Create ImageView
        ImageView imageView = new ImageView(this);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        imageParams.setMargins((int) (8 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density));
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (item.imageUri != null) {
            imageView.setImageURI(item.imageUri);
        } else if (item.capturedImageBitmap != null) {
            imageView.setImageBitmap(item.capturedImageBitmap);
        }

        // Create delete button
        Button deleteButton = new Button(this);
        FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(
                (int) (36 * getResources().getDisplayMetrics().density),
                (int) (36 * getResources().getDisplayMetrics().density)
        );
        deleteParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        deleteParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density),
                (int) (4 * getResources().getDisplayMetrics().density), 0);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setText("×");
        deleteButton.setTextSize(24);
        deleteButton.setTextColor(android.graphics.Color.WHITE);
        deleteButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
        deleteButton.setPadding(0, 0, 0, 0);
        deleteButton.setMinWidth(0);
        deleteButton.setMinHeight(0);

        deleteButton.setOnClickListener(v -> removeImage(item));

        imageContainer.addView(imageView);
        imageContainer.addView(deleteButton);
        item.imageViewContainer = imageContainer;

        // Insert before upload button (upload button is at the end)
        int insertPosition = imageItems.size();
        imagesContainer.addView(imageContainer, insertPosition);
        imageItems.add(item);

        // Hide upload button if max images reached
        if (imageItems.size() >= MAX_IMAGES) {
            btnUpload.setVisibility(View.GONE);
        }
    }

    private void removeImage(ImageItem item) {
        if (item.imageViewContainer != null) {
            imagesContainer.removeView(item.imageViewContainer);
        }
        
        // Recycle bitmap if exists
        if (item.capturedImageBitmap != null && !item.capturedImageBitmap.isRecycled()) {
            item.capturedImageBitmap.recycle();
        }
        
        imageItems.remove(item);
        
        // Update indices for remaining items
        for (int i = 0; i < imageItems.size(); i++) {
            imageItems.get(i).index = i;
        }
        
        // Show upload button if less than max images
        if (imageItems.size() < MAX_IMAGES) {
            btnUpload.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera();
            else Toast.makeText(this, "Camera permission required!", Toast.LENGTH_SHORT).show();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) enableMyLocation();
        }
    }

    public LatLng getSelectedLocation() { return selectedLocation; }
    public boolean isLocationSelected() { return selectedLocation != null; }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void validateAndSubmit() {
        String desc = description.getText().toString().trim();
        String w = width.getText().toString().trim();
        String h = height.getText().toString().trim();
        String d = depth.getText().toString().trim();
        boolean isValid = true;

        description.setError(null);
        width.setError(null);
        height.setError(null);
        depth.setError(null);

        // Description is now OPTIONAL - no validation needed

        if (w.isEmpty()) {
            width.setError("Width required");
            if (isValid) { width.requestFocus(); isValid = false; }
        } else {
            try {
                if (Double.parseDouble(w) <= 0) {
                    width.setError("Must be > 0");
                    if (isValid) { width.requestFocus(); isValid = false; }
                }
            } catch (NumberFormatException e) {
                width.setError("Invalid number");
                if (isValid) { width.requestFocus(); isValid = false; }
            }
        }

        if (h.isEmpty()) {
            height.setError("Height required");
            if (isValid) { height.requestFocus(); isValid = false; }
        } else {
            try {
                if (Double.parseDouble(h) <= 0) {
                    height.setError("Must be > 0");
                    if (isValid) { height.requestFocus(); isValid = false; }
                }
            } catch (NumberFormatException e) {
                height.setError("Invalid number");
                if (isValid) { height.requestFocus(); isValid = false; }
            }
        }

        if (!isLocationSelected()) {
            Toast.makeText(this, "Select location on map", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerBarangay.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select a Barangay", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select a Category", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerIssue.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select an Issue", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate depth if provided (optional)
        if (!d.isEmpty()) {
            try {
                if (Double.parseDouble(d) <= 0) {
                    depth.setError("Must be > 0");
                    if (isValid) { depth.requestFocus(); isValid = false; }
                }
            } catch (NumberFormatException e) {
                depth.setError("Invalid number");
                if (isValid) { depth.requestFocus(); isValid = false; }
            }
        }

        if (imageItems.isEmpty()) {
            Toast.makeText(this, "Upload at least 1 image", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid || !isNetworkAvailable()) {
            if (!isNetworkAvailable()) Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show();
            signInAnonymously();
            new android.os.Handler().postDelayed(() -> {
                if (mAuth.getCurrentUser() != null) submitToFirestore(desc, w, h, d);
                else Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }, 2000);
            return;
        }

        submitToFirestore(desc, w, h, d);
    }

    private void submitToFirestore(String desc, String widthStr, String heightStr, String depthStr) {
        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImagesToStorage(UUID.randomUUID().toString(), currentUser.getUid(), desc, widthStr, heightStr, depthStr);
    }

    private void uploadImagesToStorage(String reportId, String userId, String desc,
                                      String widthStr, String heightStr, String depthStr) {
        List<byte[]> imageBytesList = new ArrayList<>();
        for (ImageItem item : imageItems) {
            byte[] bytes = item.imageUri != null ? uriToByteArray(item.imageUri) : bitmapToByteArray(item.capturedImageBitmap);
            if (bytes != null) {
                imageBytesList.add(bytes);
            }
        }

        if (imageBytesList.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to process images", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload all images
        List<String> imageUrls = new ArrayList<>();
        int totalImages = imageBytesList.size();
        final int[] uploadedCount = {0};

        for (int i = 0; i < imageBytesList.size(); i++) {
            final int imageIndex = i; // Create final copy for lambda
            String imageName = "incident_image_" + (imageIndex + 1) + ".jpg";
            StorageReference imageRef = storageRef.child("report_images/" + userId + "/" + reportId + "/" + imageName);
            byte[] imageBytes = imageBytesList.get(imageIndex);

            imageRef.putBytes(imageBytes)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                imageUrls.add(uri.toString());
                                uploadedCount[0]++;
                                
                                if (uploadedCount[0] == totalImages) {
                                    progressDialog.setMessage("Saving report...");
                                    saveReportData(reportId, userId, desc, widthStr, heightStr, depthStr, imageUrls);
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to upload image " + (imageIndex + 1) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void saveReportData(String reportId, String userId, String desc,
                                String widthStr, String heightStr, String depthStr, List<String> imageUrls) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    LatLng location = getSelectedLocation();

                    Map<String, Object> reportData = new HashMap<>();
                    reportData.put("reportId", reportId);
                    reportData.put("userId", userId);
                    reportData.put("firstName", documentSnapshot.getString("firstName"));
                    reportData.put("lastName", documentSnapshot.getString("lastName"));
                    reportData.put("email", documentSnapshot.getString("email"));
                    reportData.put("contact", documentSnapshot.getString("contact"));
                    reportData.put("userBarangay", documentSnapshot.getString("barangay"));
                    reportData.put("position", documentSnapshot.getString("position"));
                    reportData.put("category", spinnerCategory.getSelectedItem().toString());
                    reportData.put("issue", spinnerIssue.getSelectedItem().toString());
                    reportData.put("description", desc.isEmpty() ? "No description provided" : desc);
                    reportData.put("width", Double.parseDouble(widthStr));
                    reportData.put("height", Double.parseDouble(heightStr));
                    if (!depthStr.isEmpty()) {
                        reportData.put("depth", Double.parseDouble(depthStr));
                    }
                    reportData.put("barangay", spinnerBarangay.getSelectedItem().toString());
                    reportData.put("latitude", location.latitude);
                    reportData.put("longitude", location.longitude);
                    reportData.put("timestamp", timestamp);
                    reportData.put("status", "pending");
                    reportData.put("createdAt", new Date());
                    reportData.put("imageUrl", imageUrls.get(0)); // Primary image for backward compatibility
                    reportData.put("imageUrls", imageUrls); // All images

                    saveReportToFirestore(reportData);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to fetch user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveReportToFirestore(Map<String, Object> reportData) {
        String userId = mAuth.getCurrentUser().getUid();
        String reportId = (String) reportData.get("reportId");

        db.collection("users").document(userId).collection("reports").document(reportId).set(reportData)
                .addOnSuccessListener(aVoid -> saveToGlobalReports(reportData))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToGlobalReports(Map<String, Object> reportData) {
        db.collection("reports").document((String) reportData.get("reportId")).set(reportData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    startActivity(new Intent(this, dashboard.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    // Still consider it success if saved to user collection
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    startActivity(new Intent(this, dashboard.class));
                    finish();
                });
    }

    private void clearForm() {
        description.setText("");
        width.setText("");
        height.setText("");
        depth.setText("");
        searchLocation.setText("");
        
        // Clear all images
        for (ImageItem item : imageItems) {
            if (item.capturedImageBitmap != null && !item.capturedImageBitmap.isRecycled()) {
                item.capturedImageBitmap.recycle();
            }
        }
        imageItems.clear();
        imagesContainer.removeAllViews();
        btnUpload.setVisibility(View.VISIBLE);
        
        captureLocation = null;
        if (selectedLocationMarker != null) {
            selectedLocationMarker.remove();
            selectedLocationMarker = null;
        }
        selectedLocation = null;
        selectedLocationText.setText("Tap on map to select location");
        spinnerBarangay.setSelection(0);
        spinnerCategory.setSelection(0);
        spinnerIssue.setSelection(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, dashboard.class));
        finish();
    }
}