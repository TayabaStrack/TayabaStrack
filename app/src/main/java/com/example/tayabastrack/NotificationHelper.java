package com.example.tayabastrack;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.Manifest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    // Call this when user logs in
    public static void initializeNotifications() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        saveTokenToFirestore(token);
                        Log.d(TAG, "FCM Token: " + token);
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                    }
                });
    }

    private static void saveTokenToFirestore(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "FCM Token saved successfully"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to save FCM Token", e));
        }
    }

    // Request notification permission for Android 13+
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }
}