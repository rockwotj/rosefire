package edu.rosehulman.csse.rosefire.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class RosefireTokenVerifier {

    private final JWTDecoder decoder;

    public RosefireTokenVerifier(String secret) {
        decoder = new JWTDecoder(secret);
    }

    public AuthData verify(String token) throws RosefireError {
        JSONObject payload = decoder.decode(token);
        try {
            JSONObject authData = payload.getJSONObject("d");
            Long timestamp = payload.getLong("iat") * 1000;
            String group = null;
            if (authData.has("group")) {
                group = authData.getString("group");
            }
            return new AuthData(authData.getString("uid"), authData.getString("provider"), group, new Date(timestamp));
        } catch (JSONException e) {
            throw new RosefireError("Invalid Rosefire token", e);
        }
    }


}
