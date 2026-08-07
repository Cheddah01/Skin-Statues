package com.cozycrafters.cozystatues.statue;

/**
 * Where a statue sits in the world and which way round it is.
 *
 * <p>Statue-local block coordinates are non-negative: {@code bx} runs along the
 * statue's left, {@code by} upwards from its feet and {@code bz} backwards from
 * its front. This record turns those into world coordinates, so nothing
 * downstream has to know about rotation again.
 *
 * @param anchorX  world X of statue-local (0, 0, 0)
 * @param anchorY  world Y of the statue's bottom layer
 * @param anchorZ  world Z of statue-local (0, 0, 0)
 * @param facing   the direction the statue looks in, always towards its builder
 * @param left     the direction of the statue's own left hand
 */
public record StatuePlacement(int anchorX, int anchorY, int anchorZ, Cardinal facing, Cardinal left) {

    public static StatuePlacement of(int anchorX, int anchorY, int anchorZ, Cardinal facing) {
        return new StatuePlacement(anchorX, anchorY, anchorZ, facing, facing.left());
    }

    /** The direction from the statue's front towards its back. */
    public Cardinal back() {
        return facing.opposite();
    }

    public int worldX(int bx, int bz) {
        return anchorX + left.x() * bx + back().x() * bz;
    }

    public int worldY(int by) {
        return anchorY + by;
    }

    public int worldZ(int bx, int bz) {
        return anchorZ + left.z() * bx + back().z() * bz;
    }
}
