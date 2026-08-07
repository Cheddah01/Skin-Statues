package com.cozycrafters.skinstatues.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cozycrafters.skinstatues.model.SkinLayout;
import com.cozycrafters.skinstatues.model.SkinModel;
import com.cozycrafters.skinstatues.support.TestSkins;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SkinTextureTest {

    @Test
    void modernSkinsAreSixtyFourSquare() {
        SkinTexture skin = TestSkins.solid(0x123456, SkinModel.CLASSIC);
        assertEquals(SkinLayout.MODERN, skin.layout());
        assertEquals(64, skin.height());
        assertEquals(0x123456, skin.rgbAt(0, 0));
        assertEquals(0x123456, skin.rgbAt(63, 63));
    }

    @Test
    void legacySkinsAreDetectedByTheirHalfHeight() {
        int[] pixels = new int[64 * 32];
        Arrays.fill(pixels, TestSkins.OPAQUE | 0xABCDEF);
        SkinTexture skin = SkinTexture.of(64, 32, pixels, SkinModel.CLASSIC);
        assertEquals(SkinLayout.LEGACY, skin.layout());
        assertEquals(32, skin.height());
        assertEquals(0xABCDEF, skin.rgbAt(10, 31));
        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(10, 32), "no pixels below a legacy skin");
    }

    @Test
    void transparentAndOffImageTexelsPlaceNoBlock() {
        int[] pixels = TestSkins.filled(0xFFFFFF);
        TestSkins.fill(pixels, 8, 8, 2, 2, 0x00FFFFFF);
        TestSkins.fill(pixels, 10, 8, 1, 1, 0x7FFFFFFF);
        SkinTexture skin = TestSkins.modern(pixels, SkinModel.CLASSIC);

        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(8, 8), "fully transparent");
        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(10, 8), "below the opacity threshold");
        assertEquals(0xFFFFFF, skin.rgbAt(11, 8));
        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(-1, 0));
        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(64, 0));
        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(0, 64));
    }

    @Test
    void halfOpaqueAlphaCountsAsOpaque() {
        int[] pixels = TestSkins.blank();
        TestSkins.fill(pixels, 0, 0, 1, 1, (SkinTexture.OPAQUE_ALPHA << 24) | 0x102030);
        SkinTexture skin = TestSkins.modern(pixels, SkinModel.CLASSIC);
        assertEquals(0x102030, skin.rgbAt(0, 0));
    }

    @Test
    void highResolutionSkinsAreAveragedDownToSkinSpace() {
        // A 128x128 skin: each vanilla texel is a 2x2 block of image pixels.
        int[] pixels = new int[128 * 128];
        Arrays.fill(pixels, TestSkins.OPAQUE);
        // Texel (0,0) is half black, half white, and should average to grey.
        pixels[0] = TestSkins.OPAQUE | 0xFFFFFF;
        pixels[1] = TestSkins.OPAQUE | 0xFFFFFF;
        pixels[128] = TestSkins.OPAQUE;
        pixels[129] = TestSkins.OPAQUE;
        SkinTexture skin = SkinTexture.of(128, 128, pixels, SkinModel.CLASSIC);

        assertEquals(64, skin.height());
        assertEquals(0x7F7F7F, skin.rgbAt(0, 0));
        assertEquals(0x000000, skin.rgbAt(5, 5));
    }

    @Test
    void mostlyTransparentHighResolutionTexelsStayTransparent() {
        int[] pixels = new int[128 * 128];
        pixels[0] = TestSkins.OPAQUE | 0xFF0000;
        SkinTexture skin = SkinTexture.of(128, 128, pixels, SkinModel.CLASSIC);
        assertEquals(SkinTexture.TRANSPARENT, skin.rgbAt(0, 0), "one opaque pixel in four is not enough");

        pixels[1] = TestSkins.OPAQUE | 0xFF0000;
        SkinTexture half = SkinTexture.of(128, 128, pixels, SkinModel.CLASSIC);
        assertEquals(0xFF0000, half.rgbAt(0, 0), "half opaque is enough");
    }

    @Test
    void unsupportedImageSizesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SkinTexture.of(32, 32, new int[32 * 32], SkinModel.CLASSIC));
        assertThrows(IllegalArgumentException.class,
                () -> SkinTexture.of(100, 100, new int[100 * 100], SkinModel.CLASSIC));
        assertThrows(IllegalArgumentException.class,
                () -> SkinTexture.of(64, 48, new int[64 * 48], SkinModel.CLASSIC));
        assertThrows(IllegalArgumentException.class,
                () -> SkinTexture.of(64, 64, new int[10], SkinModel.CLASSIC));
    }
}
