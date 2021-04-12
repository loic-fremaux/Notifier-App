package fr.lfremaux.notifier;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static String DEVICE_UUID = null;
    public static String API_KEY = null;

    private static final String PREF_UNIQUE_ID = "PREF_UUID";
    private static final String API_KEY_KEY = "API_KEY";

    public static Intent getIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        setVisibleMode(false);

        final FirebaseApp app = FirebaseApp.initializeApp(this);

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false);

        id(this);
        setApiKey(this, getApiKey(this));

        if (getApiKey(this) != null) {
            // verify api access
            final OkHttpClient client = new OkHttpClient();

            final RequestBody reqBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("api_key", getApiKey(this))
                    .build();

            final Request req = new Request.Builder()
                    .url("https://notifier.lfremaux.fr/api/login")
                    .post(reqBody)
                    .build();

            final CompletableFuture<Integer> requestFuture = new CompletableFuture<>();

            new Thread(() -> {
                try {
                    final Response resp = client.newCall(req).execute();
                    if (!resp.isSuccessful()) {
                        Log.e("NETWORK RESPONSE AFTER LOGIN", "Request failed " + resp.code() + " " + resp.body().string());
                        requestFuture.complete(500);
                        return;
                    }

                    requestFuture.complete(resp.code());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            requestFuture.thenAccept(resp -> {
                if (resp != 200) {
                    setVisibleMode(true);
                    return;
                }

                final Intent intent = new Intent(this, WebViewActivity.class);
                final Bundle extras = new Bundle();

                extras.putString("api_key", getApiKey(this));

                intent.putExtras(extras);

                startActivity(intent);
            });
        } else {
            setVisibleMode(true);
        }

        final Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(onClick -> {
            final EditText emailForm = findViewById(R.id.login_email);
            final EditText passwordForm = findViewById(R.id.login_password);

            final String email = emailForm.getText().toString();
            final String password = passwordForm.getText().toString();

            final String deviceName = Settings.Global.getString(getContentResolver(), "device_name");
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(firebaseKey -> {
                setVisibleMode(false);
                final OkHttpClient client = new OkHttpClient();

                final RequestBody reqBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("email", email)
                        .addFormDataPart("password", password)
                        .addFormDataPart("firebase_key", firebaseKey)
                        .addFormDataPart("device_name", deviceName)
                        .addFormDataPart("device_uuid", DEVICE_UUID)
                        .build();

                final Request req = new Request.Builder()
                        .url("https://notifier.lfremaux.fr/api/register-device")
                        .post(reqBody)
                        .build();

                final CompletableFuture<String> requestFuture = new CompletableFuture<>();

                new Thread(() -> {
                    try {
                        final Response resp = client.newCall(req).execute();
                        if (!resp.isSuccessful()) {
                            Log.e("NETWORK RESPONSE AFTER LOGIN", "Request failed " + resp.code() + " " + resp.body().string());
                            requestFuture.complete("{}");
                            setVisibleMode(true);
                            return;
                        }

                        requestFuture.complete(resp.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                requestFuture.thenAccept(resp -> {
                    final JSONObject jsonResp;
                    try {
                        jsonResp = new JSONObject(resp);
                        setApiKey(MainActivity.this, jsonResp.getString("api_token"));
                    } catch (JSONException e) {
                        Log.e("ERROR IN API TOKEN", resp);
                        e.printStackTrace();
                        setVisibleMode(true);
                        return;
                    }

                    final Intent intent = new Intent(this, WebViewActivity.class);
                    final Bundle extras = new Bundle();

                    extras.putString("api_key", getApiKey(this));

                    intent.putExtras(extras);

                    startActivity(intent);
                });
            });
        });
    }

    private void setVisibleMode(boolean bool) {
        runOnUiThread(() -> {
            final Button loginButton = findViewById(R.id.login_button);
            final FrameLayout progressBarHolder = findViewById(R.id.progressBarHolder);

            Log.e("VISIBLE MODE ", bool + " ");

            if (bool) {
                loginButton.setEnabled(true);
                progressBarHolder.setVisibility(View.GONE);
            } else {
                loginButton.setEnabled(false);

                AlphaAnimation inAnimation = new AlphaAnimation(0f, 1f);
                inAnimation.setDuration(200);
                progressBarHolder.setAnimation(inAnimation);
                progressBarHolder.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Get or create an unique identifier for this device
     *
     * @param context ApplicationContext {@link Context}
     * @return {@link String}
     */
    public synchronized static String id(Context context) {
        if (DEVICE_UUID != null) {
            return DEVICE_UUID;
        }

        final SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
        DEVICE_UUID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
        if (DEVICE_UUID == null) {
            DEVICE_UUID = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_UNIQUE_ID, DEVICE_UUID);
            editor.apply();
        }

        return DEVICE_UUID;
    }

    public synchronized static void setApiKey(Context context, String key) {
        API_KEY = key;
        final SharedPreferences sharedPrefs = context.getSharedPreferences(API_KEY_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(API_KEY_KEY, key);
        editor.apply();
    }

    public synchronized static String getApiKey(Context context) {
        if (API_KEY != null) {
            return API_KEY;
        }

        final SharedPreferences sharedPrefs = context.getSharedPreferences(API_KEY_KEY, Context.MODE_PRIVATE);
        return API_KEY = sharedPrefs.getString(API_KEY_KEY, null);
    }
}