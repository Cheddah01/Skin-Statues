package com.cozycrafters.skinstatues.skin;

/**
 * A skin could not be resolved. The message is written for the player who ran
 * the command, so it never leaks URLs or stack traces.
 */
public class SkinLookupException extends Exception {

    private static final long serialVersionUID = 1L;

    public SkinLookupException(String message) {
        super(message);
    }

    public SkinLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
