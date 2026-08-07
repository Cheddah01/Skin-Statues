package com.cozycrafters.skinstatues.fabric.skin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/** Downloads skin PNGs over HTTP with short, fixed timeouts. */
public final class HttpTextureDownloader implements TextureDownloader {

    /** Vanilla skins are a few kilobytes; this only guards against a runaway response. */
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private final HttpClient client;

    public HttpTextureDownloader() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public byte[] download(URI url) throws SkinLookupException {
        String scheme = url.getScheme() == null ? "" : url.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https")) {
            throw new SkinLookupException("That player's skin data is malformed.");
        }

        HttpRequest request = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "SkinStatues")
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException ex) {
            throw new SkinLookupException("The skin texture could not be downloaded. Try again in a moment.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SkinLookupException("The skin download was interrupted.", ex);
        }

        try (InputStream body = response.body()) {
            if (response.statusCode() != 200 || !"https".equalsIgnoreCase(response.uri().getScheme())) {
                throw new SkinLookupException("The skin service returned an unexpected response. Try again in a moment.");
            }
            byte[] bytes = body.readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw new SkinLookupException("That player's skin data is malformed.");
            }
            return bytes;
        } catch (IOException ex) {
            throw new SkinLookupException("The skin texture could not be downloaded. Try again in a moment.", ex);
        }
    }
}
