package edu.rosehulman.csse.rosefire.server;


import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

public class JWTDecoder {

    private static final String TOKEN_SEP = ".";
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final String HMAC_256 = "HmacSHA256";
    private final String secret;

    public JWTDecoder(String secret) {
        this.secret = secret;
    }

    public JSONObject decode(String token) throws RosefireError {
        try {
            String[] encoded = Pattern.compile(TOKEN_SEP, Pattern.LITERAL).split(token);
            String encodedHeader = encoded[0];
            String encodedClaims = encoded[1];
            String sig = encoded[2];

            JSONObject header = decodeJson(encodedHeader);

            if (!"JWT".equals(header.getString("typ"))) {
                throw new RosefireError("Incorrect JWT");
            }

            if (!"HS256".equals(header.getString("alg"))) {
                throw new RosefireError("Wrong Algorithm!");
            }

            String secureBits = encodedHeader + TOKEN_SEP + encodedClaims;

            if (!sig.equals(sign(secret, secureBits))) {
                throw new RosefireError("Token generated with invalid secret!");
            }

            return decodeJson(encodedClaims);
        } catch (Exception e) {
            throw new RosefireError("Error decoding token", e);
        }
    }


    private static JSONObject decodeJson(String encodedJson) throws JSONException {
        String json = new String(Base64.decodeBase64(encodedJson), UTF8_CHARSET);
        return new JSONObject(json);
    }

    private static String sign(String secret, String secureBits) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_256);
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(UTF8_CHARSET), HMAC_256);
            sha256_HMAC.init(secret_key);
            byte sig[] = sha256_HMAC.doFinal(secureBits.getBytes(UTF8_CHARSET));
            return Base64.encodeBase64URLSafeString(sig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
