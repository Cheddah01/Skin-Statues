package com.cozycrafters.cozystatues.model;

/**
 * Where a body part's box net lives in the skin image.
 *
 * @param u        left edge of the net (the top face column of the net is at {@code u + depth})
 * @param v        top edge of the net
 * @param mirrored true when the region belongs to the opposite-handed limb and
 *                 has to be flipped along the box width, as legacy 64x32 skins do
 */
public record TextureRegion(int u, int v, boolean mirrored) {

    public static TextureRegion at(int u, int v) {
        return new TextureRegion(u, v, false);
    }

    public static TextureRegion mirroredAt(int u, int v) {
        return new TextureRegion(u, v, true);
    }
}
