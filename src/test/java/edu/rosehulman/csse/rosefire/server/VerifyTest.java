package edu.rosehulman.csse.rosefire.server;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class VerifyTest {

    private static final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZWJ1ZyI6ZmFsc2UsImQiOnsidWlkIjoicm9ja3dvdGoiLCJwcm92aWRlciI6InJvc2UtaHVsbWFuIiwiZ3JvdXAiOiJTVFVERU5UIn0sInYiOjAsImFkbWluIjp0cnVlLCJpYXQiOjE0NTA5MDIzMTR9.rO9YBglcdPZFAPQloEcbvZmQ24RDoTxu9aGxyZ7msME";



    @Test
    public void testValidToken() throws RosefireError {
        String secret = "secret";
        AuthData userInfo = new RosefireTokenVerifier(secret).verify(token);
        assertEquals("rockwotj@rose-hulman.edu", userInfo.getEmail());
        assertEquals("rose-hulman", userInfo.getProvider());
        assertEquals("rockwotj", userInfo.getUsername());
        assertEquals(AuthData.Group.STUDENT, userInfo.getGroup());
        assertEquals(new Date(1450902314000L), userInfo.getIssuedAt());
    }




    @Test(expected = RosefireError.class)
    public void testInvalidToken() throws RosefireError {
        String secret = "foobar";
        new RosefireTokenVerifier(secret).verify(token);
    }

}
