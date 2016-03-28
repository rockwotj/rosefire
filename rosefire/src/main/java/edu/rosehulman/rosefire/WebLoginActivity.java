package edu.rosehulman.rosefire;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class WebLoginActivity extends AppCompatActivity {

    private WebView mLoginScreen;

    public static final String REGISTRY_TOKEN = "registry";
    public static final String JWT_TOKEN = "token";
    public static final String ERROR = "error";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoginScreen = new WebView(this);
        this.setContentView(mLoginScreen);
        String token = getIntent().getStringExtra(REGISTRY_TOKEN);
        try {
            mLoginScreen.loadUrl("https://rosefire.csse.rose-hulman.edu/webview/login?registryToken=" + URLEncoder.encode(token, "UTF-8") + "&platform=android");
        } catch (UnsupportedEncodingException e) {
            // TODO: Also catch loading errors
            onLoginFail("Invalid registryToken");
        }
        WebSettings webSettings = mLoginScreen.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mLoginScreen.addJavascriptInterface(new JavascriptFunctions(), "Android");
    }


    private void onLoginSuccess(String token) {
        if (Rosefire.DEBUG) Log.d("RFA", token);
        Intent data = new Intent();
        data.putExtra(JWT_TOKEN, token);
        setResult(RESULT_OK, data);
        finish();
    }

    private void onLoginFail(String message) {
        if (Rosefire.DEBUG) Log.d("RFA", message);
        Intent data = new Intent();
        data.putExtra(ERROR, message);
        setResult(RESULT_CANCELED, data);
        finish();
    }

    class JavascriptFunctions {

        @JavascriptInterface
        public void finish(String token) {
            onLoginSuccess(token);
        }
    }

}
