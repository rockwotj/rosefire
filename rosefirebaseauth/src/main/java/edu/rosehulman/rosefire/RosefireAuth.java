package edu.rosehulman.rosefire;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthResultHandler;
import com.firebase.client.FirebaseError;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import edu.rosehulman.rockwotj.rosefirebaseauth.R;

/**
 * <p>The class that authenticates a Rose-Hulman User with Firebase for you.</p>
 * <p>
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
 * <p>
 * <p>
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
        private Integer expires;
        private Integer notBefore;
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
        public TokenOptions(Integer expires, Integer notBefore, Boolean admin) {
            this.expires = expires;
            this.notBefore = notBefore;
            this.admin = admin;
        }

        public Integer getExpires() {
            return expires;
        }

        /**
         * Set when the auth token expires.
         *
         * @param expires A timestamp of when the token is invalid.
         */
        public void setExpires(Integer expires) {
            this.expires = expires;
        }

        public Integer getNotBefore() {
            return notBefore;
        }

        /**
         * Set when the auth token starts being valid.
         *
         * @param notBefore A timestamp of when the token should start being valid.
         */
        public void setNotBefore(Integer notBefore) {
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
            String response = makeRequest("auth", params.toString());
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
            Log.d(TAG, "Authentation for " + data.get("username"));
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
                    FirebaseError err = new FirebaseError(400, "Invalid credentials for rose-hulman.edu");
                    mResultHandler.onAuthenticationError(err);
                } else {
                    mFirebaseRef.authWithCustomToken("", null);
                }
            }
        }
    }

    private String makeRequest(String endpoint, String json) {
        HttpsURLConnection urlConnection;
        String url = mRoseAuthServiceUrl + endpoint + "/";
        if (DEBUG) {
            Log.d(TAG, "JSON data for request at " + url + " is: " + json);
        }
        String data = json;
        String result = null;
        try {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());

            Field firebaseContext = com.firebase.client.core.Context.class.getField("androidContext");
            firebaseContext.setAccessible(true);
            Context context = (Context) firebaseContext.get(null);

            InputStream is = context.getResources().openRawResource(R.raw.rosefire);
            store.load(is, "rosefire".toCharArray());
            is.close();


            final Certificate rootca = store.getCertificate("rosefire.csse.rose-hulman.edu");

            // Turn it to X509 format.
            is = new ByteArrayInputStream(rootca.getEncoded());
            X509Certificate x509ca = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null == x509ca) {
                throw new CertificateException("Embedded SSL certificate has expired.");
            }

            // Check the CA's validity.
            x509ca.checkValidity();

            // Accepted CA is only the one installed in the store.
            final X509Certificate[] acceptedIssuers = new X509Certificate[]{x509ca};

            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            Exception error = null;

                            if (null == chain || 0 == chain.length) {
                                error = new CertificateException("Certificate chain is invalid.");
                            } else if (null == authType || 0 == authType.length()) {
                                error = new CertificateException("Authentication type is invalid.");
                            } else {
                                if (DEBUG)
                                    Log.i(TAG, "Chain includes " + chain.length + " certificates.");
                                try {
                                    for (X509Certificate cert : chain) {
                                        if (DEBUG) {
                                            Log.i(TAG, "Server Certificate Details:");
                                            Log.i(TAG, "---------------------------");
                                            Log.i(TAG, "IssuerDN: " + cert.getIssuerDN().toString());
                                            Log.i(TAG, "SubjectDN: " + cert.getSubjectDN().toString());
                                            Log.i(TAG, "Serial Number: " + cert.getSerialNumber());
                                            Log.i(TAG, "Version: " + cert.getVersion());
                                            Log.i(TAG, "Not before: " + cert.getNotBefore().toString());
                                            Log.i(TAG, "Not after: " + cert.getNotAfter().toString());
                                            Log.i(TAG, "---------------------------");
                                        }
                                        // Make sure that it hasn't expired.
                                        cert.checkValidity();

                                        // Verify the certificate's public key chain.
                                        cert.verify(rootca.getPublicKey());
                                    }
                                } catch (InvalidKeyException e) {
                                    error = e;
                                } catch (NoSuchAlgorithmException e) {
                                    error = e;
                                } catch (NoSuchProviderException e) {
                                    error = e;
                                } catch (SignatureException e) {
                                    error = e;
                                }
                            }
                            if (null != error) {
                                Log.e("GALE", "Certificate error", error);
                                throw new CertificateException(error);
                            }
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return acceptedIssuers;
                        }
                    }
            };
            sc.init(null, trustManagers, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
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
            e.printStackTrace();
        }
        return result;
    }

}
