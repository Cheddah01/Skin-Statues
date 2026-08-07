package com.cozycrafters.skinstatues.fabric.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class FabricProfileSourceTest {
    @Test
    void officialTextureHttpUrlsAreUpgradedToHttps() throws Exception {
        URI secured = FabricProfileSource.secureTextureUri(
                URI.create("http://textures.minecraft.net/texture/abc"));
        assertEquals("https", secured.getScheme());
        assertEquals("textures.minecraft.net", secured.getHost());
    }

    @Test
    void arbitraryInsecureTextureUrlsAreRejected() {
        assertThrows(SkinLookupException.class,
                () -> FabricProfileSource.secureTextureUri(URI.create("http://example.com/skin.png")));
        assertThrows(SkinLookupException.class,
                () -> FabricProfileSource.secureTextureUri(URI.create("file:///tmp/skin.png")));
    }
}
