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

            return new AuthData(payload);
        } catch (JSONException e) {
            throw new RosefireError("Invalid Rosefire token", e);
        }
    }


}
