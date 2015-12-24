package edu.rosehulman.csse.rosefire.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AuthData {

    private final String username;
    private final String domain;
    private final String email;
    private final Date issuedAt;


    AuthData(JSONObject json) throws JSONException, ParseException {
        this(json.getString("uid"), json.getString("domain"), json.getString("email"), json.getString("timestamp"));
    }

    public AuthData(String username, String domain, String email, String issuedAt) throws ParseException {
        this(username, domain, email, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(issuedAt));
    }

    public AuthData(String username, String domain, String email, Date issuedAt)  {
        this.username = username;
        this.domain = domain;
        this.email = email;
        this.issuedAt = issuedAt;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getEmail() {
        return email;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }
}
