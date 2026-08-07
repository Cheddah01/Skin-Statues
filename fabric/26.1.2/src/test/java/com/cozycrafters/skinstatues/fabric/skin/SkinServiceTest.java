package com.cozycrafters.skinstatues.fabric.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cozycrafters.skinstatues.fabric.model.SkinModel;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * The caching and failure handling around skin lookups. Nothing here touches
 * the network: both the profile source and the downloader are stubs.
 */
class SkinServiceTest {

    private static final URI SKIN_URL = URI.create("https://textures.minecraft.net/texture/abc");

    private final AtomicLong now = new AtomicLong(1_000L);
    private final List<String> profileLookups = new ArrayList<>();
    private final List<URI> downloads = new ArrayList<>();

    private static byte[] pngSkin() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private SkinService service(ProfileSource profiles, TextureDownloader downloader) {
        return new SkinService(profiles, downloader, 60_000L, now::get);
    }

    private ProfileSource okProfile() {
        return name -> {
            profileLookups.add(name);
            return new SkinProfile(UUID.randomUUID(), name, SKIN_URL, SkinModel.SLIM);
        };
    }

    private TextureDownloader okDownloader() {
        return url -> {
            downloads.add(url);
            try {
                return pngSkin();
            } catch (Exception ex) {
                throw new SkinLookupException("test fixture", ex);
            }
        };
    }

    @Test
    void aResolvedSkinCarriesItsProfileAndArmShape() throws Exception {
        ResolvedSkin skin = service(okProfile(), okDownloader()).resolve("Notch");
        assertEquals("Notch", skin.profile().name());
        assertEquals(SkinModel.SLIM, skin.texture().model());
        assertEquals(64, skin.texture().imageWidth());
    }

    @Test
    void repeatedLookupsAreServedFromTheCache() throws Exception {
        SkinService service = service(okProfile(), okDownloader());
        ResolvedSkin first = service.resolve("Notch");
        ResolvedSkin second = service.resolve("Notch");
        assertSame(first, second);
        assertEquals(1, profileLookups.size());
        assertEquals(1, downloads.size());
    }

    @Test
    void theCacheIsCaseInsensitive() throws Exception {
        SkinService service = service(okProfile(), okDownloader());
        service.resolve("Notch");
        service.resolve("nOTCH");
        assertEquals(1, profileLookups.size());
    }

    @Test
    void anExpiredEntryIsFetchedAgain() throws Exception {
        SkinService service = service(okProfile(), okDownloader());
        service.resolve("Notch");
        now.addAndGet(59_000L);
        service.resolve("Notch");
        assertEquals(1, profileLookups.size(), "still fresh");

        now.addAndGet(2_000L);
        service.resolve("Notch");
        assertEquals(2, profileLookups.size(), "expired, so fetched again");
    }

    @Test
    void clearingTheCacheForcesAFreshLookup() throws Exception {
        SkinService service = service(okProfile(), okDownloader());
        service.resolve("Notch");
        assertEquals(1, service.cachedCount());
        service.clear();
        assertEquals(0, service.cachedCount());
        service.resolve("Notch");
        assertEquals(2, profileLookups.size());
    }

    @Test
    void anUnknownPlayerFailsWithoutDownloadingAnything() {
        SkinService service = service(name -> {
            profileLookups.add(name);
            throw new SkinLookupException("No Minecraft player named '" + name + "' was found.");
        }, okDownloader());

        SkinLookupException error = assertThrows(SkinLookupException.class, () -> service.resolve("Nobody"));
        assertEquals("No Minecraft player named 'Nobody' was found.", error.getMessage());
        assertEquals(0, downloads.size());
    }

    @Test
    void failuresAreCachedBrieflySoTheCommandCannotBeUsedToSpamMojang() {
        SkinService service = service(name -> {
            profileLookups.add(name);
            throw new SkinLookupException("No Minecraft player named '" + name + "' was found.");
        }, okDownloader());

        for (int i = 0; i < 5; i++) {
            assertThrows(SkinLookupException.class, () -> service.resolve("Nobody"));
        }
        assertEquals(1, profileLookups.size());

        now.addAndGet(SkinService.FAILURE_CACHE_MILLIS + 1);
        assertThrows(SkinLookupException.class, () -> service.resolve("Nobody"));
        assertEquals(2, profileLookups.size());
    }

    @Test
    void aFailedDownloadIsReportedAndNotCachedAsASuccess() {
        SkinService service = service(okProfile(), url -> {
            downloads.add(url);
            throw new SkinLookupException("The skin texture could not be downloaded. Try again in a moment.");
        });
        assertThrows(SkinLookupException.class, () -> service.resolve("Notch"));
        assertEquals(1, downloads.size());
        assertThrows(SkinLookupException.class, () -> service.resolve("Notch"));
        assertEquals(1, downloads.size(), "the failure is cached, not retried immediately");
    }

    @Test
    void malformedTextureDataIsRejectedCleanly() {
        SkinService service = service(okProfile(), url -> "not a png".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        SkinLookupException error = assertThrows(SkinLookupException.class, () -> service.resolve("Notch"));
        assertEquals("That player's skin image could not be read.", error.getMessage());
    }

    @Test
    void aSkinWithAnUnsupportedSizeIsRejectedCleanly() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(50, 50, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] png = out.toByteArray();

        SkinService service = service(okProfile(), url -> png);
        SkinLookupException error = assertThrows(SkinLookupException.class, () -> service.resolve("Notch"));
        assertEquals("That player's skin image has an unsupported size.", error.getMessage());
    }

    @Test
    void cachingCanBeTurnedOff() throws Exception {
        SkinService service = new SkinService(okProfile(), okDownloader(), 0L, now::get);
        service.resolve("Notch");
        service.resolve("Notch");
        assertEquals(2, profileLookups.size());
    }
}
