package com.cozycrafters.skinstatues.fabric.skin;

/**
 * Resolves a username to a profile with a skin URL. Implementations block on
 * network calls and must never be invoked from the server thread.
 */
public interface ProfileSource {

    SkinProfile lookup(String name) throws SkinLookupException;
}
