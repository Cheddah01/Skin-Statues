package com.cozycrafters.skinstatues.fabric.model;

/**
 * One axis-aligned box of the player model, measured in skin pixels.
 *
 * <p>Model space is the statue's own frame:
 * <ul>
 *   <li>{@code x} grows towards the statue's <em>left</em> hand side,</li>
 *   <li>{@code y} grows upwards, {@code y == 0} being the ground the statue stands on,</li>
 *   <li>{@code z} grows towards the statue's <em>back</em>, {@code z == 0} being the
 *       front plane of the torso.</li>
 * </ul>
 *
 * <p>{@code x}/{@code y}/{@code z} are the minimum corner. Outer-layer boxes are
 * the base box grown by {@link #inflate()} pixels on every side; their texture is
 * still only {@code texWidth x texHeight x texDepth}, so the extra ring of voxels
 * repeats the border texel of the base face.
 */
public record ModelBox(
        BodyPart part,
        boolean overlay,
        int x,
        int y,
        int z,
        int width,
        int height,
        int depth,
        int texWidth,
        int texHeight,
        int texDepth,
        int inflate
) {

    public static ModelBox base(BodyPart part, int x, int y, int z, int width, int height, int depth) {
        return new ModelBox(part, false, x, y, z, width, height, depth, width, height, depth, 0);
    }

    /** The outer-layer shell around {@code base}, one pixel larger on every side. */
    public static ModelBox overlayOf(ModelBox base) {
        return new ModelBox(base.part(), true,
                base.x() - 1, base.y() - 1, base.z() - 1,
                base.width() + 2, base.height() + 2, base.depth() + 2,
                base.width(), base.height(), base.depth(), 1);
    }

    public int maxX() {
        return x + width - 1;
    }

    public int maxY() {
        return y + height - 1;
    }

    public int maxZ() {
        return z + depth - 1;
    }

    /**
     * True when the local voxel sits on the given face of this box. Local
     * coordinates run {@code lx} left-wards, {@code ly} downwards from the top
     * of the box and {@code lz} backwards from the front of the box.
     */
    public boolean isOnFace(BoxFace face, int lx, int ly, int lz) {
        return switch (face) {
            case FRONT -> lz == 0;
            case BACK -> lz == depth - 1;
            case RIGHT -> lx == 0;
            case LEFT -> lx == width - 1;
            case TOP -> ly == 0;
            case BOTTOM -> ly == height - 1;
        };
    }

    /** True when the voxel is anywhere on the box shell; interior voxels are never placed. */
    public boolean isOnShell(int lx, int ly, int lz) {
        return lx == 0 || lx == width - 1
                || ly == 0 || ly == height - 1
                || lz == 0 || lz == depth - 1;
    }

    /** Model-space Y for a local row, which is counted downwards from the box top. */
    public int modelY(int ly) {
        return maxY() - ly;
    }
}
