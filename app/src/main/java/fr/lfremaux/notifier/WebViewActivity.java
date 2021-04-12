package fr.lfremaux.notifier;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();

        final WebView webview = new WebView(getApplicationContext());

        final WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);

        setContentView(webview);

        try {
            if (getIntent().getExtras().containsKey("api_key")) {
                Log.i("LOGIN GIVEN API KEY", getIntent().getExtras().getString("api_key"));
                webview.postUrl("https://notifier.lfremaux.fr/api/login", (
                        "api_key=" + URLEncoder.encode(getIntent().getExtras().getString("api_key"), "UTF-8")
                ).getBytes());
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}