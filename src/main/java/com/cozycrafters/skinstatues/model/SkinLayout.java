package com.cozycrafters.skinstatues.model;

import java.util.Map;

/**
 * Where each body part's box net lives in a skin image.
 *
 * <p>{@link #MODERN} is the 64x64 layout every skin has used since 1.8: each
 * limb owns its own base and outer-layer region. {@link #LEGACY} is the old
 * 64x32 layout, which only stores the right limbs and the hat; the left limbs
 * are the right ones mirrored, exactly as the vanilla renderer draws them.
 */
public enum SkinLayout {
    MODERN(
            Map.of(
                    BodyPart.HEAD, TextureRegion.at(0, 0),
                    BodyPart.TORSO, TextureRegion.at(16, 16),
                    BodyPart.RIGHT_ARM, TextureRegion.at(40, 16),
                    BodyPart.LEFT_ARM, TextureRegion.at(32, 48),
                    BodyPart.RIGHT_LEG, TextureRegion.at(0, 16),
                    BodyPart.LEFT_LEG, TextureRegion.at(16, 48)),
            Map.of(
                    BodyPart.HEAD, TextureRegion.at(32, 0),
                    BodyPart.TORSO, TextureRegion.at(16, 32),
                    BodyPart.RIGHT_ARM, TextureRegion.at(40, 32),
                    BodyPart.LEFT_ARM, TextureRegion.at(48, 48),
                    BodyPart.RIGHT_LEG, TextureRegion.at(0, 32),
                    BodyPart.LEFT_LEG, TextureRegion.at(0, 48))),

    LEGACY(
            Map.of(
                    BodyPart.HEAD, TextureRegion.at(0, 0),
                    BodyPart.TORSO, TextureRegion.at(16, 16),
                    BodyPart.RIGHT_ARM, TextureRegion.at(40, 16),
                    BodyPart.LEFT_ARM, TextureRegion.mirroredAt(40, 16),
                    BodyPart.RIGHT_LEG, TextureRegion.at(0, 16),
                    BodyPart.LEFT_LEG, TextureRegion.mirroredAt(0, 16)),
            Map.of(BodyPart.HEAD, TextureRegion.at(32, 0)));

    private final Map<BodyPart, TextureRegion> base;
    private final Map<BodyPart, TextureRegion> overlay;

    SkinLayout(Map<BodyPart, TextureRegion> base, Map<BodyPart, TextureRegion> overlay) {
        this.base = base;
        this.overlay = overlay;
    }

    /** The layout implied by a skin image's proportions. */
    public static SkinLayout forSize(int width, int height) {
        return height * 2 == width ? LEGACY : MODERN;
    }

    /** The region for a part, or {@code null} when this layout does not store it. */
    public TextureRegion region(BodyPart part, boolean overlayLayer) {
        return (overlayLayer ? overlay : base).get(part);
    }
}
