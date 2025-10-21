package com.example.tayabastrack;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class usermanual extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_usermanual);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public static class MyFirebaseMessagingService extends FirebaseMessagingService {

        private static final String TAG = "FCMService";
        private static final String CHANNEL_ID = "report_updates_channel";

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();
        }

        @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
            super.onMessageReceived(remoteMessage);

            Log.d(TAG, "From: " + remoteMessage.getFrom());

            // Check if message contains a notification payload
            if (remoteMessage.getNotification() != null) {
                String title = remoteMessage.getNotification().getTitle();
                String body = remoteMessage.getNotification().getBody();

                showNotification(title, body);
            }

            // Check if message contains data payload
            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessage.getData());

                String title = remoteMessage.getData().get("title");
                String body = remoteMessage.getData().get("body");
                String reportId = remoteMessage.getData().get("reportId");
                String status = remoteMessage.getData().get("status");

                showNotification(title, body);
            }
        }

        @Override
        public void onNewToken(String token) {
            super.onNewToken(token);
            Log.d(TAG, "New FCM Token: " + token);

            // Send token to Firestore
            sendTokenToFirestore(token);
        }

        private void sendTokenToFirestore(String token) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("users").document(userId)
                        .update("fcmToken", token)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "FCM Token updated in Firestore"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to update FCM Token", e));
            }
        }

        private void showNotification(String title, String body) {
            Intent intent = new Intent(this, myreports.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(0, notificationBuilder.build());
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Report Updates",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for report status updates");

                NotificationManager notificationManager =
                        getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static class NotificationHelper {

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
                if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE
                    );
                }
            }
        }
    }
}