package edu.rosehulman.csse.rosefire.server;

/**
 * Created by rockwotj on 12/23/15.
 */
public class RosefireError extends Exception {

    public RosefireError() {
    }

    public RosefireError(String message) {
        super(message);
    }

    public RosefireError(String message, Throwable cause) {
        super(message, cause);
    }

    public RosefireError(Throwable cause) {
        super(cause);
    }

    public RosefireError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
