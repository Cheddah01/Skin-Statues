package com.cozycrafters.skinstatues.fabric.statue;

/**
 * A snapped compass direction. Statues are only ever built on the four cardinal
 * axes, which keeps every block on the world grid and every face flat.
 */
public enum Cardinal {
    SOUTH(0, 1),
    WEST(-1, 0),
    NORTH(0, -1),
    EAST(1, 0);

    private final int x;
    private final int z;

    Cardinal(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public Cardinal opposite() {
        return switch (this) {
            case SOUTH -> NORTH;
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    /**
     * The direction the player's yaw is snapped to. Minecraft yaw runs 0 south,
     * 90 west, 180 north, 270 east, so quarter turns land exactly on the
     * declaration order of this enum.
     */
    public static Cardinal fromYaw(float yaw) {
        float normalised = yaw % 360.0f;
        if (normalised < 0.0f) {
            normalised += 360.0f;
        }
        int quarter = Math.round(normalised / 90.0f) & 3;
        return values()[quarter];
    }

    /**
     * The direction a body facing {@code this} would call its own left: the
     * cross product of up and the facing direction, which works out as
     * {@code (z, -x)}. Facing south, that is east.
     */
    public Cardinal left() {
        return of(z, -x);
    }

    private static Cardinal of(int x, int z) {
        for (Cardinal cardinal : values()) {
            if (cardinal.x == x && cardinal.z == z) {
                return cardinal;
            }
        }
        throw new IllegalArgumentException("Not a cardinal direction: " + x + "," + z);
    }
}
