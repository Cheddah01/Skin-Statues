package com.cozycrafters.skinstatues.model;

/**
 * The six faces of a model box, named from the statue's own point of view:
 * {@link #LEFT} is the statue's left hand side, not the viewer's.
 */
public enum BoxFace {
    FRONT,
    LEFT,
    RIGHT,
    BACK,
    TOP,
    BOTTOM;

    /**
     * The face this one becomes when the box is textured mirrored along its
     * width. Used for legacy 64x32 skins, where the left limbs reuse the right
     * limb regions.
     */
    public BoxFace mirroredX() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            default -> this;
        };
    }

    /**
     * Face lookup order used when a voxel sits on more than one face. The
     * statue always faces the player who built it, so the front wins, then the
     * two profiles, and the rarely visible faces come last.
     */
    public static final BoxFace[] PRIORITY = {FRONT, LEFT, RIGHT, BACK, TOP, BOTTOM};
}
