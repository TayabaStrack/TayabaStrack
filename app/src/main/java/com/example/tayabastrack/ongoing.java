package com.example.tayabastrack;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.InputStream;
import com.google.firebase.Timestamp;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ongoing extends AppCompatActivity {

    private LinearLayout contentLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ongoing);

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

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        contentLayout = findViewById(R.id.contentFrame);

        // Find the tab TextViews
        TextView pendingTab = findViewById(R.id.tabPending);
        TextView completedTab = findViewById(R.id.completed);
        TextView declinedTab = findViewById(R.id.declined);

        // Load ongoing reports
        loadOngoingReports();

        // Click listener for PENDING (go back to myreports)
        pendingTab.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, myreports.class);
            startActivity(intent);
            finish();
        });

        // Click listener for COMPLETED
        completedTab.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, completed.class);
            startActivity(intent);
            finish();
        });

        // Click listener for DECLINED
        declinedTab.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, declined.class);
            startActivity(intent);
            finish();
        });

        // Back Button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, dashboard.class);
            startActivity(intent);
            finish();
        });

        // ✅ Bottom Navigation - Home
        ImageButton homeButton = findViewById(R.id.nav_home);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, dashboard.class);
            startActivity(intent);
            finish();
        });

        // ✅ Bottom Navigation - Contacts
        ImageButton contactsButton = findViewById(R.id.nav_contacts);
        contactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, contacts.class);
            startActivity(intent);
            finish();
        });

        // ✅ Bottom Navigation - Submit Report
        ImageButton submitReportButton = findViewById(R.id.nav_submit);
        submitReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, submitreport.class);
            startActivity(intent);
            finish();
        });

        // ✅ Bottom Navigation - History (My Reports)
        ImageButton historyButton = findViewById(R.id.nav_history);
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, myreports.class);
            startActivity(intent);
            finish();
        });

        // ✅ Bottom Navigation - Profile
        ImageButton profileButton = findViewById(R.id.nav_profile);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(ongoing.this, myaccount.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadOngoingReports() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Fetch ongoing reports from Firestore
        // Query from root reports collection where userId matches and status is "ongoing"
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .whereIn("status", java.util.Arrays.asList("ongoing", "Ongoing"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contentLayout.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyMessage = new TextView(this);
                        emptyMessage.setText("No ongoing reports");
                        emptyMessage.setTextSize(16);
                        emptyMessage.setPadding(16, 16, 16, 16);
                        emptyMessage.setGravity(android.view.Gravity.CENTER);
                        contentLayout.addView(emptyMessage);
                        return;
                    }

                    queryDocumentSnapshots.getDocuments().forEach(document -> {
                        CardView reportCard = createReportCard(document.getData());
                        contentLayout.addView(reportCard);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load reports: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private CardView createReportCard(java.util.Map<String, Object> reportData) {
        CardView cardView = new CardView(this);
        cardView.setRadius(12);
        cardView.setCardElevation(8);
        cardView.setMaxCardElevation(12);
        cardView.setUseCompatPadding(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(8, 8, 8, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(16, 16, 16, 16);
        cardContent.setBackgroundColor(0xFFFFFFFF);
        cardView.addView(cardContent);

        // Top section - Description and Barangay with Image
        LinearLayout topSection = new LinearLayout(this);
        topSection.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.setMargins(0, 0, 0, 16);
        topSection.setLayoutParams(topParams);
        cardContent.addView(topSection);

        // Left side - Description and Barangay Container
        LinearLayout leftContainer = new LinearLayout(this);
        leftContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.65f
        );
        leftContainer.setLayoutParams(leftParams);
        topSection.addView(leftContainer);

        TextView categoryLabel = new TextView(this);
        categoryLabel.setText("Category:");
        categoryLabel.setTextSize(15);
        categoryLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryLabel.setTextColor(0xFF004AAD);
        LinearLayout.LayoutParams categoryLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        categoryLabelParams.setMargins(0, 12, 0, 4);
        categoryLabel.setLayoutParams(categoryLabelParams);
        leftContainer.addView(categoryLabel);

        TextView categoryText = new TextView(this);
        String category = reportData.get("category") != null ?
                reportData.get("category").toString() : "No category";
        categoryText.setText(category);
        categoryText.setTextSize(14);
        categoryText.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        categoryParams.setMargins(0, 0, 0, 0);
        categoryText.setLayoutParams(categoryParams);
        leftContainer.addView(categoryText);

// Issue Section
        TextView issueLabel = new TextView(this);
        issueLabel.setText("Issue:");
        issueLabel.setTextSize(15);
        issueLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        issueLabel.setTextColor(0xFF004AAD);
        LinearLayout.LayoutParams issueLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        issueLabelParams.setMargins(0, 12, 0, 4);
        issueLabel.setLayoutParams(issueLabelParams);
        leftContainer.addView(issueLabel);

        TextView issueText = new TextView(this);
        String issue = reportData.get("issue") != null ?
                reportData.get("issue").toString() : "No issue";
        issueText.setText(issue);
        issueText.setTextSize(14);
        issueText.setTypeface(null, android.graphics.Typeface.BOLD);
        issueText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams issueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        issueParams.setMargins(0, 0, 0, 0);
        issueText.setLayoutParams(issueParams);
        leftContainer.addView(issueText);

        // Description Label
        TextView descriptionLabel = new TextView(this);
        descriptionLabel.setText("Description:");
        descriptionLabel.setTextSize(15);
        descriptionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        descriptionLabel.setTextColor(0xFF004AAD);
        leftContainer.addView(descriptionLabel);

        // Description Text
        TextView descriptionText = new TextView(this);
        String description = reportData.get("description") != null ?
                reportData.get("description").toString() : "No description";
        descriptionText.setText(description);
        descriptionText.setTextSize(22);
        descriptionText.setTypeface(null, android.graphics.Typeface.BOLD);
        descriptionText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, 4, 0, 12);
        descriptionText.setLayoutParams(descParams);
        leftContainer.addView(descriptionText);

        // Barangay Label
        TextView barangayLabel = new TextView(this);
        barangayLabel.setText("Barangay:");
        barangayLabel.setTextSize(15);
        barangayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        barangayLabel.setTextColor(0xFF004AAD);
        leftContainer.addView(barangayLabel);

        // Barangay Text
        TextView barangayText = new TextView(this);
        String barangay = reportData.get("barangay") != null ?
                reportData.get("barangay").toString() : "No barangay";
        barangayText.setText(barangay);
        barangayText.setTextSize(22);
        barangayText.setTypeface(null, android.graphics.Typeface.BOLD);
        barangayText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams barangayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        barangayParams.setMargins(0, 4, 0, 0);
        barangayText.setLayoutParams(barangayParams);
        leftContainer.addView(barangayText);

        // Right side - Image Section
        LinearLayout imageContainer = new LinearLayout(this);
        imageContainer.setOrientation(LinearLayout.VERTICAL);
        imageContainer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams imageContainerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.35f
        );
        imageContainer.setLayoutParams(imageContainerParams);
        topSection.addView(imageContainer);

        // FrameLayout to hold image and overlay
        android.widget.FrameLayout imageFrame = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(300, 300);
        imageFrame.setLayoutParams(frameParams);

        ImageView reportImage = new ImageView(this);
        reportImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        reportImage.setBackgroundColor(0xFFC0C0C0);
        android.widget.FrameLayout.LayoutParams imageParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        reportImage.setLayoutParams(imageParams);
        reportImage.setClickable(false);
        reportImage.setFocusable(false);

        // Get image URLs
        List<String> imageUrls = new ArrayList<>();
        if (reportData.containsKey("imageUrls") && reportData.get("imageUrls") != null) {
            Object urlsObj = reportData.get("imageUrls");
            if (urlsObj instanceof List) {
                List<?> urlsList = (List<?>) urlsObj;
                for (Object url : urlsList) {
                    if (url != null) {
                        imageUrls.add(url.toString());
                    }
                }
            }
        }
        
        // Fallback to single imageUrl if imageUrls not available
        if (imageUrls.isEmpty() && reportData.containsKey("imageUrl") && reportData.get("imageUrl") != null) {
            imageUrls.add(reportData.get("imageUrl").toString());
        }

        // Load image from Firebase Storage URL
        if (!imageUrls.isEmpty()) {
            loadImageFromUrl(imageUrls.get(0), reportImage);
        } else {
            // Try to load from old Blob format (for backward compatibility)
            if (reportData.containsKey("incidentImage")) {
                try {
                    com.google.firebase.firestore.Blob imageBlob =
                            (com.google.firebase.firestore.Blob) reportData.get("incidentImage");
                    byte[] imageBytes = imageBlob.toBytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        reportImage.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    android.util.Log.e("Ongoing", "Failed to load image from Blob", e);
                }
            }
        }

        imageFrame.addView(reportImage);

        // Add "+1 more photo" overlay if there are 2 images
        if (imageUrls.size() == 2) {
            TextView morePhotosText = new TextView(this);
            android.widget.FrameLayout.LayoutParams textParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            textParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            textParams.setMargins(0, 0, 0, 8);
            morePhotosText.setLayoutParams(textParams);
            morePhotosText.setText("+1 more photo");
            morePhotosText.setTextColor(android.graphics.Color.WHITE);
            morePhotosText.setTextSize(12);
            morePhotosText.setTypeface(null, android.graphics.Typeface.BOLD);
            morePhotosText.setBackgroundColor(0x80000000); // Semi-transparent black
            morePhotosText.setPadding(8, 4, 8, 4);
            morePhotosText.setGravity(android.view.Gravity.CENTER);
            morePhotosText.setClickable(false);
            morePhotosText.setFocusable(false);
            imageFrame.addView(morePhotosText);
        }


        imageContainer.addView(imageFrame);

        // Inspection Date Section
        if (reportData.containsKey("inspectionDate") && reportData.get("inspectionDate") != null) {
            TextView inspectionLabel = new TextView(this);
            inspectionLabel.setText("Inspection Date:");
            inspectionLabel.setTextSize(15);
            inspectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            inspectionLabel.setTextColor(0xFF004AAD);
            LinearLayout.LayoutParams inspectionLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionLabelParams.setMargins(0, 16, 0, 4);
            inspectionLabel.setLayoutParams(inspectionLabelParams);
            cardContent.addView(inspectionLabel);

            TextView inspectionText = new TextView(this);
            Object dateObj = reportData.get("inspectionDate");

            // Handle both Timestamp objects and String values
            String dateString;
            if (dateObj instanceof com.google.firebase.Timestamp) {
                com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) dateObj;
                dateString = new java.text.SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a", java.util.Locale.US)
                        .format(timestamp.toDate());
            } else {
                dateString = dateObj.toString();
            }

            inspectionText.setText(dateString);
            inspectionText.setTextSize(14);
            inspectionText.setTypeface(null, android.graphics.Typeface.BOLD);
            inspectionText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams inspectionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inspectionParams.setMargins(0, 0, 0, 12);
            inspectionText.setLayoutParams(inspectionParams);
            cardContent.addView(inspectionText);
        }

        // To Repair In Section (Date Range: startDate - endDate)
        if ((reportData.containsKey("startDate") && reportData.get("startDate") != null) ||
                (reportData.containsKey("endDate") && reportData.get("endDate") != null)) {

            TextView repairLabel = new TextView(this);
            repairLabel.setText("To Repair In:");
            repairLabel.setTextSize(15);
            repairLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            repairLabel.setTextColor(0xFF004AAD);
            LinearLayout.LayoutParams repairLabelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            repairLabelParams.setMargins(0, 0, 0, 4);
            repairLabel.setLayoutParams(repairLabelParams);
            cardContent.addView(repairLabel);

            TextView repairText = new TextView(this);

            // Format start date
            String startDateString = "";
            if (reportData.containsKey("startDate") && reportData.get("startDate") != null) {
                Object startDateObj = reportData.get("startDate");
                if (startDateObj instanceof com.google.firebase.Timestamp) {
                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) startDateObj;
                    startDateString = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                            .format(timestamp.toDate());
                } else {
                    startDateString = startDateObj.toString();
                }
            }

            // Format end date
            String endDateString = "";
            if (reportData.containsKey("endDate") && reportData.get("endDate") != null) {
                Object endDateObj = reportData.get("endDate");
                if (endDateObj instanceof com.google.firebase.Timestamp) {
                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) endDateObj;
                    endDateString = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                            .format(timestamp.toDate());
                } else {
                    endDateString = endDateObj.toString();
                }
            }

            // Combine dates with hyphen
            String dateRange = "";
            if (!startDateString.isEmpty() && !endDateString.isEmpty()) {
                dateRange = startDateString + " - " + endDateString;
            } else if (!startDateString.isEmpty()) {
                dateRange = startDateString;
            } else if (!endDateString.isEmpty()) {
                dateRange = endDateString;
            }

            repairText.setText(dateRange);
            repairText.setTextSize(14);
            repairText.setTypeface(null, android.graphics.Typeface.BOLD);
            repairText.setTextColor(0xFF333333);
            LinearLayout.LayoutParams repairParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            repairParams.setMargins(0, 0, 0, 0);
            repairText.setLayoutParams(repairParams);
            cardContent.addView(repairText);
        }

        return cardView;
    }

    private void loadImageFromUrl(String imageUrl, ImageView imageView) {
        // Load image in background thread
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                // Update UI on main thread
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("Ongoing", "Failed to load image from URL: " + imageUrl, e);
                runOnUiThread(() -> {
                    // Keep the gray placeholder background
                });
            }
        }).start();
    }
}