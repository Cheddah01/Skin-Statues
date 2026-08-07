package com.cozycrafters.skinstatues.skin;

import com.cozycrafters.skinstatues.model.SkinLayout;
import com.cozycrafters.skinstatues.model.SkinModel;

/**
 * A decoded skin image addressed in vanilla 64-pixel-wide skin space.
 *
 * <p>High resolution skins (128x128, 256x256, ...) are supported by averaging
 * each vanilla texel's block of image pixels, so every caller can keep working
 * in the coordinates the model geometry is defined in.
 */
public final class SkinTexture {

    /** Alpha at or above this counts as opaque; anything below places no block. */
    public static final int OPAQUE_ALPHA = 128;

    /** Returned by {@link #rgbAt(int, int)} when a texel places no block. */
    public static final int TRANSPARENT = -1;

    private final int imageWidth;
    private final int imageHeight;
    private final int[] argb;
    private final SkinModel model;
    private final SkinLayout layout;
    private final int sampleSize;

    private SkinTexture(int imageWidth, int imageHeight, int[] argb, SkinModel model) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.argb = argb;
        this.model = model;
        this.layout = SkinLayout.forSize(imageWidth, imageHeight);
        this.sampleSize = imageWidth / 64;
    }

    /**
     * @param argb row-major ARGB pixels, {@code imageWidth * imageHeight} long
     * @throws IllegalArgumentException if the image is not a valid skin shape
     */
    public static SkinTexture of(int imageWidth, int imageHeight, int[] argb, SkinModel model) {
        if (imageWidth < 64 || imageWidth % 64 != 0) {
            throw new IllegalArgumentException("Skin width must be a positive multiple of 64, got " + imageWidth);
        }
        if (imageHeight != imageWidth && imageHeight * 2 != imageWidth) {
            throw new IllegalArgumentException(
                    "Skin must be square or twice as wide as tall, got " + imageWidth + "x" + imageHeight);
        }
        if (argb.length != imageWidth * imageHeight) {
            throw new IllegalArgumentException("Pixel array does not match " + imageWidth + "x" + imageHeight);
        }
        return new SkinTexture(imageWidth, imageHeight, argb.clone(), model);
    }

    public SkinModel model() {
        return model;
    }

    public SkinLayout layout() {
        return layout;
    }

    public int imageWidth() {
        return imageWidth;
    }

    public int imageHeight() {
        return imageHeight;
    }

    /** Height of this skin in 64-wide skin space: 64 for modern skins, 32 for legacy ones. */
    public int height() {
        return imageHeight / sampleSize;
    }

    /**
     * The colour at a texel in 64-wide skin space, or {@link #TRANSPARENT} when
     * the texel is transparent or off the image.
     *
     * <p>For high resolution skins the underlying block of image pixels is
     * averaged; a block that is mostly transparent stays transparent, so a
     * detailed hat brim does not grow a fringe of solid blocks.
     */
    public int rgbAt(int u, int v) {
        if (u < 0 || v < 0 || u >= 64 || v >= height()) {
            return TRANSPARENT;
        }
        if (sampleSize == 1) {
            int pixel = argb[v * imageWidth + u];
            return alpha(pixel) >= OPAQUE_ALPHA ? pixel & 0xFFFFFF : TRANSPARENT;
        }

        int opaque = 0;
        long red = 0;
        long green = 0;
        long blue = 0;
        for (int dy = 0; dy < sampleSize; dy++) {
            int row = (v * sampleSize + dy) * imageWidth + u * sampleSize;
            for (int dx = 0; dx < sampleSize; dx++) {
                int pixel = argb[row + dx];
                if (alpha(pixel) < OPAQUE_ALPHA) {
                    continue;
                }
                opaque++;
                red += (pixel >> 16) & 0xFF;
                green += (pixel >> 8) & 0xFF;
                blue += pixel & 0xFF;
            }
        }
        int total = sampleSize * sampleSize;
        if (opaque * 2 < total) {
            return TRANSPARENT;
        }
        return (int) (red / opaque) << 16 | (int) (green / opaque) << 8 | (int) (blue / opaque);
    }

    private static int alpha(int pixel) {
        return (pixel >>> 24) & 0xFF;
    }
}
