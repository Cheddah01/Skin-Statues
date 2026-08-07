package com.cozycrafters.cozystatues.statue;

/**
 * Works out where a statue goes from where its builder is standing.
 *
 * <p>The rules, in order:
 * <ul>
 *   <li>the builder's yaw is snapped to a cardinal direction, so the statue is
 *       always axis aligned;</li>
 *   <li>the statue looks back at the builder — its front is the face nearest
 *       them, and its back runs away from them;</li>
 *   <li>its front layer starts one block beyond the builder's own hitbox, so
 *       nothing is ever placed inside them, whichever way they face and wherever
 *       in a block they happen to stand;</li>
 *   <li>it is centred left to right on the builder;</li>
 *   <li>it stands on the block level the builder's feet are on.</li>
 * </ul>
 */
public final class PlacementCalculator {

    /** Half the width of a standing player's hitbox. */
    public static final double PLAYER_HALF_WIDTH = 0.3;

    private PlacementCalculator() {
    }

    public static StatuePlacement compute(double x, double y, double z, float yaw,
                                          int widthBlocks, int depthBlocks) {
        return compute(x, y, z, yaw, widthBlocks, depthBlocks, PLAYER_HALF_WIDTH);
    }

    public static StatuePlacement compute(double x, double y, double z, float yaw,
                                          int widthBlocks, int depthBlocks, double halfWidth) {
        if (widthBlocks < 1 || depthBlocks < 1) {
            throw new IllegalArgumentException("A statue needs a positive width and depth.");
        }

        // The builder looks along `away`; the statue is built along it and faces back.
        Cardinal away = Cardinal.fromYaw(yaw);
        Cardinal facing = away.opposite();
        Cardinal left = facing.left();

        int anchorX;
        int anchorZ;
        if (away.x() != 0) {
            anchorX = frontLine(x, away.x(), halfWidth);
            anchorZ = centreLine(z, left.z(), widthBlocks);
        } else {
            anchorZ = frontLine(z, away.z(), halfWidth);
            anchorX = centreLine(x, left.x(), widthBlocks);
        }
        return new StatuePlacement(anchorX, (int) Math.floor(y), anchorZ, facing, left);
    }

    /**
     * The coordinate of the statue's front layer along the axis the builder is
     * facing: the first whole block that clears their hitbox entirely. Normally
     * exactly one block in front of them; two when they are standing right
     * against the boundary of their own block.
     */
    private static int frontLine(double position, int direction, double halfWidth) {
        if (direction > 0) {
            return (int) Math.ceil(position + halfWidth);
        }
        return (int) Math.floor(position - halfWidth) - 1;
    }

    /**
     * The coordinate of statue-local {@code bx == 0} along the sideways axis,
     * chosen so the statue straddles the block the builder is standing in.
     */
    private static int centreLine(double position, int leftComponent, int widthBlocks) {
        return (int) Math.floor(position) - leftComponent * ((widthBlocks - 1) / 2);
    }
}
