package edu.rosehulman.rosefire;

import android.app.Activity;
import android.content.Intent;

public class Rosefire {

    public static int REQUEST_CODE = 5500;

    static boolean DEBUG = false;
    private Activity activity;

    private static Rosefire instance;

    public static Rosefire getInstance() {
        if (instance == null) {
            instance = new Rosefire();
        }
        return instance;
    }

    private Rosefire() {
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void signIn(String registryToken) {
        if (activity == null) {
            throw new RuntimeException("You MUST set the activity via `setActivity` before you can call `signIn`");
        }
        Intent intent = new Intent(activity, WebLoginActivity.class);
        intent.putExtra(WebLoginActivity.REGISTRY_TOKEN, registryToken);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    public String extractToken(Intent data) {
        return data.getStringExtra(WebLoginActivity.JWT_TOKEN);
    }

}
