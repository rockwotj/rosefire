package edu.rosehulman.csse.rosefire.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class AuthData {

    public enum Group {
        STUDENT,
        INSTRUCTOR,
        OTHER
    }

    private final String username;
    private final String provider;
    private final Date issuedAt;
    private final Group group;
    private final JSONObject authData;

    public AuthData(JSONObject payload) throws JSONException {
        authData = payload.getJSONObject("d");
        Long timestamp = payload.getLong("iat") * 1000;
        String group = null;
        if (authData.has("group")) {
            group = authData.getString("group");
        }
        this.username =  authData.getString("uid");
        this.provider = authData.getString("provider");
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
        this.issuedAt = new Date(timestamp);
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

    public Map<String, Object> getAuthPayload() {
        Map<String, Object> payload = new HashMap<String, Object>();
        Iterator keys = authData.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            try {
                payload.put(key, authData.get(key));
            } catch (JSONException e) {
                // Eat it
            }
        }
        return payload;
    }



}
