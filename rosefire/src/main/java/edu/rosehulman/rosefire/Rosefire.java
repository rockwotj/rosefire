package edu.rosehulman.rosefire;

import android.content.Context;
import android.content.Intent;

public class Rosefire {

    public static int REQUEST_CODE = 5500;

    static boolean DEBUG = false;

    public static Intent getSignInIntent(Context context, String registryToken) {
        Intent intent = new Intent(context, WebLoginActivity.class);
        intent.putExtra(WebLoginActivity.REGISTRY_TOKEN, registryToken);
        return intent;
    }

    public static String getSignInResultFromIntent(Intent data) {
        return data.getStringExtra(WebLoginActivity.JWT_TOKEN);
    }

}
