package fr.lfremaux.notifier.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import fr.lfremaux.notifier.MainActivity;
import fr.lfremaux.notifier.R;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        sendNotification(remoteMessage);
    }

    private void sendNotification(RemoteMessage messageBody) {
        Map<String, String> data = messageBody.getData();

        Intent backIntent = MainActivity.getIntent(this);

        Intent intent = MainActivity.getIntent(this);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[]{backIntent, intent},
                PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, messageBody.getNotification().getChannelId())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setVibrate(new long[]{500, 1000, 500, 1000})
                .setPriority(2)
                .setContentTitle(messageBody.getNotification().getTitle())
                .setContentText(messageBody.getNotification().getBody())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    // token management

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        sendFirebaseToken(s);
    }

    public void sendFirebaseToken(String token) {
        Log.e("FIREBASE", "FIREBASE TOKEN SENT " + token);

        if (MainActivity.getApiKey(this) == null) {
            return;
        }

        final OkHttpClient client = new OkHttpClient();

        final RequestBody reqBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", MainActivity.getApiKey(this))
                .addFormDataPart("device_uuid", MainActivity.id(this))
                .addFormDataPart("firebase_key", token)
                .build();

        final Request req = new Request.Builder()
                .url("https://notifier.lfremaux.fr/api/update-device")
                .post(reqBody)
                .build();


        new Thread(() -> {
            try {
                final Response resp = client.newCall(req).execute();
                if (!resp.isSuccessful()) {
                    Log.e("NETWORK RESPONSE AFTER UPDATE TOKEN", "UPDATE TOKEN REQUEST FAILED " + resp.code() + " " + resp.body().string());
                    return;
                }


                Log.e("NETWORK RESPONSE AFTER UPDATE TOKEN", "TOKEN SUCCESSFULLY UPDATED " + resp.code() + " " + resp.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
