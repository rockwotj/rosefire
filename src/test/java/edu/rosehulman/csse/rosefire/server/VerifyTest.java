package edu.rosehulman.csse.rosefire.server;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class VerifyTest {

    private static final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZWJ1ZyI6ZmFsc2UsImQiOnsidWlkIjoicm9ja3dvdGoiLCJkb21haW4iOiJyb3NlLWh1bG1hbi5lZHUiLCJlbWFpbCI6InJvY2t3b3RqQHJvc2UtaHVsbWFuLmVkdSIsInRpbWVzdGFtcCI6IjIwMTUtMTItMjNUMTU6MjU6MTQtMDU6MDAifSwidiI6MCwiYWRtaW4iOnRydWUsImlhdCI6MTQ1MDkwMjMxNH0.P50l5YvcRNO4IQ5WT9sKfaBFBiJHU5yxLnCxfJ5xprI";



    @Test
    public void testValidToken() throws RosefireError {
        String secret = "secret";
        AuthData userInfo = new RosefireTokenVerifier(secret).verify(token);
        assertEquals("rockwotj@rose-hulman.edu", userInfo.getEmail());
        assertEquals("rose-hulman.edu", userInfo.getDomain());
        assertEquals("rockwotj", userInfo.getUsername());
        assertEquals(new Date(1450902314000L), userInfo.getIssuedAt());
    }




    @Test(expected = RosefireError.class)
    public void testInvalidToken() throws RosefireError {
        String secret = "foobar";
        new RosefireTokenVerifier(secret).verify(token);
    }

}
