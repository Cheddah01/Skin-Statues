package com.cozycrafters.cozystatues.model;

import java.util.UUID;

/** Arm shape of a skin: 4 pixel wide classic arms or 3 pixel wide slim ones. */
public enum SkinModel {
    CLASSIC(4),
    SLIM(3);

    private final int armWidth;

    SkinModel(int armWidth) {
        this.armWidth = armWidth;
    }

    public int armWidth() {
        return armWidth;
    }

    /**
     * The model vanilla falls back to when a profile carries no explicit
     * {@code metadata.model}: the low bit of the account UUID's hash picks the
     * default skin, and therefore the arm shape, exactly as the client does.
     */
    public static SkinModel defaultFor(UUID uuid) {
        return (uuid.hashCode() & 1) == 1 ? SLIM : CLASSIC;
    }
}
