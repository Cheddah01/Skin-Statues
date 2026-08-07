package com.cozycrafters.cozystatues.support;

import com.cozycrafters.cozystatues.model.SkinModel;
import com.cozycrafters.cozystatues.skin.SkinTexture;

/** Synthetic skins for tests, so nothing has to touch the network. */
public final class TestSkins {

    public static final int OPAQUE = 0xFF000000;

    private TestSkins() {
    }

    /** A fully transparent 64x64 canvas. */
    public static int[] blank() {
        return new int[64 * 64];
    }

    public static int[] filled(int rgb) {
        int[] pixels = blank();
        java.util.Arrays.fill(pixels, OPAQUE | rgb);
        return pixels;
    }

    public static void fill(int[] pixels, int u, int v, int width, int height, int argb) {
        for (int y = v; y < v + height; y++) {
            for (int x = u; x < u + width; x++) {
                pixels[y * 64 + x] = argb;
            }
        }
    }

    public static SkinTexture modern(int[] pixels, SkinModel model) {
        return SkinTexture.of(64, 64, pixels, model);
    }

    /** A 64x64 skin of a single opaque colour. */
    public static SkinTexture solid(int rgb, SkinModel model) {
        return modern(filled(rgb), model);
    }
}
