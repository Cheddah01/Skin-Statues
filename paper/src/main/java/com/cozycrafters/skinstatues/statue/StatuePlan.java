package com.cozycrafters.skinstatues.statue;

import org.bukkit.Material;

/**
 * A finished, world-absolute build order: every block to place, bottom layer
 * first, already de-duplicated.
 *
 * <p>Positions are packed into longs (26 bits of X, 26 of Z, 12 of Y, the usual
 * Minecraft packing) so that even a maximum-scale statue costs a few megabytes
 * rather than hundreds of thousands of objects.
 */
public final class StatuePlan {

    private final long[] positions;
    private final Material[] materials;
    private final int size;
    private final int widthBlocks;
    private final int heightBlocks;
    private final int depthBlocks;

    StatuePlan(long[] positions, Material[] materials, int size,
               int widthBlocks, int heightBlocks, int depthBlocks) {
        this.positions = positions;
        this.materials = materials;
        this.size = size;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
        this.depthBlocks = depthBlocks;
    }

    public int blockCount() {
        return size;
    }

    public int widthBlocks() {
        return widthBlocks;
    }

    public int heightBlocks() {
        return heightBlocks;
    }

    public int depthBlocks() {
        return depthBlocks;
    }

    public Material material(int index) {
        return materials[index];
    }

    public int x(int index) {
        return unpackX(positions[index]);
    }

    public int y(int index) {
        return unpackY(positions[index]);
    }

    public int z(int index) {
        return unpackZ(positions[index]);
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }
}
