package com.cozycrafters.skinstatues.fabric.model;

/**
 * The pixel-space bounding box of a whole player model, used both to size the
 * statue in the world and to normalise voxel coordinates to a non-negative grid.
 */
public record ModelBounds(int minX, int minY, int minZ, int width, int height, int depth) {

    public int maxX() {
        return minX + width - 1;
    }

    public int maxY() {
        return minY + height - 1;
    }

    public int maxZ() {
        return minZ + depth - 1;
    }
}
