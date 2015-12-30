package edu.rosehulman.rosefire;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthResultHandler;
import com.firebase.client.FirebaseError;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * <p>The class that authenticates a Rose-Hulman User with Firebase for you.</p>
 * <p/>
 * <code>
 * RosefireAuth roseAuth = new RosefireAuth(fb, "REGISTRY_TOKEN");
 * roseAuth.authWithRoseHulman("rockwotj@rose-hulman.ed", "Pa$sW0rd", new Firebase.AuthResultHandler() {
 *
 * @Override public void onAuthenticated(AuthData authData) {
 * // Show logged in UI
 * }
 * @Override public void onAuthenticationError(FirebaseError firebaseError) {
 * // Show Login Error
 * }
 * });
 * <p/>
 * <p/>
 * </code>
 */
public class RosefireAuth {

    private static final String TAG = "RFA";
    public static boolean DEBUG = false;

    private final String mRoseAuthServiceUrl;
    private final Firebase mFirebaseRef;
    private final String mRegistryToken;

    /**
     * <p>
     * Create a RoseFirebaseAuthenticator with https://rosefire.csse.rose-hulman.edu as the
     * the url that the server is running on.
     * </p>
     *
     * @param repo          The firebase repo to authenticate with.
     * @param registryToken The registryToken for your app; generated from
     *                      the server's registration page.
     */
    public RosefireAuth(Firebase repo, String registryToken) {
        this(repo, registryToken, "https://rosefire.csse.rose-hulman.edu");
    }

    /**
     * <p>
     * Create a RoseFirebaseAuthenticator with a custom url that the server is running on.
     * </p>
     *
     * @param repo           The firebase repo to authenticate with.
     * @param registryToken  The registryToken for your app; generated from
     *                       the server's registration page.
     * @param authServiceUrl The url that the Rose authentication token is running at.
     */
    public RosefireAuth(Firebase repo, String registryToken, String authServiceUrl) {
        mFirebaseRef = repo;
        mRegistryToken = registryToken;
        mRoseAuthServiceUrl = authServiceUrl + "/api/";
        if (DEBUG) {
            Log.d(TAG, "URL base endpoint: " + mRoseAuthServiceUrl);
        }
    }

    /**
     * <p>
     * Authenticate the user with Rose-Hulman credentials given a Rose-Hulman email and password.
     * </p>
     * <p>
     * This method is async and the result will be handled in the handler's callbacks
     * </p>
     *
     * @param email    A valid Rose-Hulman email.
     * @param password A valid Rose-Hulman password for the email.
     * @param handler  A Firebase AuthResultHandler for callbacks.
     */
    public void authWithRoseHulman(String email, String password, AuthResultHandler handler) {
        authWithRoseHulman(email, password, handler, null);
    }

    /**
     * <p>
     * Authenticate the user with Rose-Hulman credentials given a Rose-Hulman email and password,
     * with custom options for the auth token.
     * </p>
     * <p>
     * This method is async and the result will be handled in the handler's callbacks
     * </p>
     *
     * @param email    A valid Rose-Hulman email.
     * @param password A valid Rose-Hulman password for the email.
     * @param handler  A Firebase AuthResultHandler for callbacks.
     * @param options  The options for the auth token that is generated on the server.
     */
    public void authWithRoseHulman(String email, String password, AuthResultHandler handler, TokenOptions options) {
        if (DEBUG) {
            Log.d(TAG, "Authenticating user " + email);
        }
        new RoseTokenFetcher(email, password, handler, options).execute();
    }

    /**
     * <p>The authentication token options that will be generated on the server. </p>
     * <p>For more details see <a href="https://github.com/rockwotj/rose-firebase-auth#post-apiauth">
     * https://github.com/rockwotj/rose-firebase-auth#post-apiauth</a></p>
     */
    public static class TokenOptions {
        private Long expires;
        private Long notBefore;
        private Boolean admin;

        /**
         * Create an empty options object
         */
        public TokenOptions() {
            this(null, null, null);
        }

        /**
         * Create an options object with the given options.
         *
         * @param admin     If true, then all security rules are disabled for this user.
         *                  This can only be true for the user who the token is registred with.
         * @param expires   A timestamp of when the token is invalid.
         * @param notBefore A timestamp of when the token should start being valid.
         */
        public TokenOptions(Long expires, Long notBefore, Boolean admin) {
            this.expires = expires;
            this.notBefore = notBefore;
            this.admin = admin;
        }

        public Long getExpires() {
            return expires;
        }

        /**
         * Set when the auth token expires.
         *
         * @param expires A timestamp of when the token is invalid.
         */
        public void setExpires(Long expires) {
            this.expires = expires;
        }

        public Long getNotBefore() {
            return notBefore;
        }

        /**
         * Set when the auth token starts being valid.
         *
         * @param notBefore A timestamp of when the token should start being valid.
         */
        public void setNotBefore(Long notBefore) {
            this.notBefore = notBefore;
        }

        /**
         * Set if the user has all of the firebase options disabled.
         *
         * @param admin If true, then all security rules are disabled for this user.
         *              This can only be true for the user who the token is registred with.
         */
        public void setAdmin(Boolean admin) {
            this.admin = admin;
        }

        public Boolean isAdmin() {
            return admin;
        }
    }

    private class RoseTokenFetcher extends AsyncTask<Void, Void, String> {

        private final String mEmail;
        private final String mPassword;
        private final AuthResultHandler mResultHandler;
        private final TokenOptions mOptions;
        private RosefireException error;

        public RoseTokenFetcher(String email, String password, AuthResultHandler handler, TokenOptions options) {
            mEmail = email;
            mPassword = password;
            mResultHandler = handler;
            mOptions = options;
        }

        @Override
        protected String doInBackground(Void... ignored) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode params = mapper.createObjectNode()
                    .put("email", mEmail)
                    .put("password", mPassword)
                    .put("registryToken", mRegistryToken);
            if (mOptions != null) {
                ObjectNode options = mapper.createObjectNode();
                if (mOptions.isAdmin() != null) {
                    options.put("admin", mOptions.isAdmin().booleanValue());
                }
                if (mOptions.getExpires() != null) {
                    options.put("expires", mOptions.getExpires().intValue());
                }
                if (mOptions.getNotBefore() != null) {
                    options.put("notBefore", mOptions.getNotBefore().intValue());
                }
                params.put("options", options);
            }

            String response;
            try {
                response = makeRequest("auth", params.toString());
            } catch (RosefireException e) {
                error = e;
                return null;
            }

            if (DEBUG) {
                Log.d(TAG, "Request response: " + response);
            }
            if (response == null || response.isEmpty()) {
                return null;
            }
            Map<String, String> data;
            try {
                data = mapper.readValue(response, Map.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (DEBUG) {
                Log.d(TAG, "Authentation for " + data.get("username"));
            }
            return data.get("token");
        }

        @Override
        protected void onPostExecute(String roseAuthToken) {
            if (DEBUG) {
                Log.d(TAG, "roseAuthToken: " + roseAuthToken);
            }
            if (roseAuthToken != null) {
                mFirebaseRef.authWithCustomToken(roseAuthToken, mResultHandler);
            } else {
                if (mResultHandler != null) {
                    FirebaseError err = new FirebaseError(error.getStatusCode(), error.getMessage());
                    mResultHandler.onAuthenticationError(err);
                } else {
                    mFirebaseRef.authWithCustomToken("", null);
                }
            }
        }
    }

    private class RosefireException extends Exception {
        private final int statusCode;
        private final String message;

        RosefireException(int statusCode, String message) {

            this.statusCode = statusCode;
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    private String makeRequest(String endpoint, String json) throws RosefireException {
        HttpsURLConnection urlConnection = null;
        String url = mRoseAuthServiceUrl + endpoint + "/";
        if (DEBUG) {
            Log.d(TAG, "JSON data for request at " + url + " is: " + json);
        }
        String data = json;
        String result;

        try {
            urlConnection = (HttpsURLConnection) ((new URL(url).openConnection()));

            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            //TODO: Use com.fasterxml.jackson for serialization instead of bufferedReader/Writer
            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(data);
            writer.close();
            outputStream.close();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            result = sb.toString();
        } catch (Exception e) {
            int code = 0;
            try {
                code = urlConnection.getResponseCode();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            if (DEBUG) {
                Log.d(TAG, "Error code for " + url + " is: " + code);
            }
            throw new RosefireException(code, code == 400 ? "Invalid Rose-Hulman Credentials!" : "Network error!");
        }

        return result;
    }

}
