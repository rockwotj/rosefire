package ede.rosehulman.auth.rosefire.test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class RosefireActivity extends AppCompatActivity {


    private WebView mLoginScreen;

    public static final String REGISTRY_TOKEN = "registry";
    public static final String JWT_TOKEN = "token";
    public static final String ERROR = "error";
    private static final boolean DEBUG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rosefire);
        String token = getIntent().getStringExtra(REGISTRY_TOKEN);
//        String token = "d1c2362b2bb9dff4f80c6fdb1e382be17db0a22b7c81baeada4f7bca8d0085dcf69a9bfd0b7ef71a2e83bc0a978b9ccdrF6s69O9u2GSjfQywK/DXZjWtngL1RWPHHVbaa65RPU=";
        mLoginScreen = (WebView) findViewById(R.id.webview);

        try {
            mLoginScreen.loadUrl("https://rosefire.csse.rose-hulman.edu/webview/login?registryToken=" + URLEncoder.encode(token, "UTF-8") + "&platform=android");
//            mLoginScreen.loadUrl("http://rockwotj-mac.wlan.rose-hulman.edu:8080/webview/login?registryToken=" + URLEncoder.encode(token, "UTF-8") + "&platform=android");
        } catch (UnsupportedEncodingException e) {
            onLoginFail("Invalid registryToken");
        }
        WebSettings webSettings = mLoginScreen.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mLoginScreen.addJavascriptInterface(new JavascriptFunctions(), "Android");
    }


    private void onLoginSuccess(String token) {
        if (DEBUG) Log.d("RFA", token);
        Intent data = new Intent();
        data.putExtra(JWT_TOKEN, token);
        setResult(RESULT_OK, data);
        finish();
    }

    private void onLoginFail(String message) {
        if (DEBUG) Log.d("RFA", message);
        Intent data = new Intent();
        data.putExtra(ERROR, message);
        setResult(RESULT_CANCELED, data);
        finish();
    }


    class JavascriptFunctions {

        @JavascriptInterface
        public void finish(String jsonResult) {
            if (DEBUG) Log.d("RFA", jsonResult);
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> authResult = mapper.readValue(jsonResult, Map.class);
                if (authResult.containsKey("token")) {
                    onLoginSuccess(authResult.get("token").toString());
                } else if (authResult.containsKey("error")) {
                    onLoginFail(authResult.get("error").toString());
                } else {
                    onLoginFail("Network Error");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
