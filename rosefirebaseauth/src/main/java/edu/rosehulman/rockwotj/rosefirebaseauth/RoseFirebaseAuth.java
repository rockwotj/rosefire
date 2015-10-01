package edu.rosehulman.rockwotj.rosefirebaseauth;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthResultHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class RoseFirebaseAuth {

    public static final String TAG = "RFA";

    private final String mRoseAuthServiceUrl;
    private final Firebase mFirebaseRef;
    private final String mRegistryToken;

    public RoseFirebaseAuth(Firebase repo, String registryToken) {
        // Default to localhost on emulator's port 8080.
        this(repo, registryToken, "http://10.0.0.2:8080");
    }

    public RoseFirebaseAuth(Firebase repo, String registryToken, String authServiceUrl) {
        mFirebaseRef = repo;
        mRegistryToken = registryToken;
        mRoseAuthServiceUrl = authServiceUrl + "/api/";
    }

    public void authWithRoseHulman(String email, String password, AuthResultHandler handler) {
        authWithRoseHulman(email, password, handler, null);
    }

    public void authWithRoseHulman(String email, String password, AuthResultHandler handler, TokenOptions options) {
        new RoseTokenFetcher(email, password, handler, options).execute();
    }

    public static class TokenOptions {
        private Integer expires;
        private Integer notBefore;
        private Boolean admin;

        public TokenOptions() {
            this(null, null, null);
        }

        public TokenOptions(Integer expires, Integer notBefore, Boolean admin) {
            this.expires = expires;
            this.notBefore = notBefore;
            this.admin = admin;
        }

        public Integer getExpires() {
            return expires;
        }

        public void setExpires(Integer expires) {
            this.expires = expires;
        }

        public Integer getNotBefore() {
            return notBefore;
        }

        public void setNotBefore(Integer notBefore) {
            this.notBefore = notBefore;
        }

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
                params.putPOJO("options", mOptions);
            }
            Log.d(TAG, params.toString());
            String response = makeRequest("auth", params.toString());
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
            mFirebaseRef.authWithCustomToken(roseAuthToken, mResultHandler);
        }
    }

    private String makeRequest(String endpoint, String json) {
        HttpURLConnection urlConnection;
        String url = mRoseAuthServiceUrl + endpoint + "/";
        String data = json;
        String result = null;
        try {
            urlConnection = (HttpURLConnection) ((new URL(url).openConnection()));
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
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
