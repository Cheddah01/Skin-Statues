package com.cozycrafters.skinstatues.skin;

import java.net.URI;

/**
 * Fetches the raw bytes of a skin texture. Blocking; never call from the server
 * thread.
 */
public interface TextureDownloader {

    byte[] download(URI url) throws SkinLookupException;
}
