package edu.rosehulman.csse.rosefire.server;

import java.util.Date;


public class AuthData {

    enum Group {
        STUDENT,
        INSTRUCTOR,
        OTHER
    }

    private final String username;
    private final String provider;
    private final Date issuedAt;
    private final Group group;

    public AuthData(String username, String domain, String group, Date issuedAt)  {
        this.username = username;
        this.provider = domain;
        if (group != null) {
            if (group.equalsIgnoreCase("STUDENT")) {
                this.group = Group.STUDENT;
            } else if (group.equalsIgnoreCase("INSTRUCTOR")) {
                this.group = Group.INSTRUCTOR;
            } else {
                this.group = Group.OTHER;
            }
        } else {
            this.group = null;
        }
        this.issuedAt = issuedAt;
    }

    public String getUsername() {
        return username;
    }

    public String getProvider() {
        return provider;
    }

    public String getEmail() {
        return username + "@rose-hulman.edu";
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public Group getGroup() {
        return group;
    }

}
