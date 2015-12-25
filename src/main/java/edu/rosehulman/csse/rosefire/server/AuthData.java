package edu.rosehulman.csse.rosefire.server;

import java.util.Date;


public class AuthData {

    private final String username;
    private final String domain;
    private final String email;
    private final Date issuedAt;

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
