package com.cozycrafters.skinstatues.fabric.model;

/**
 * Maps a voxel on a box face to the skin texel that paints it.
 *
 * <p>A skin stores every box as the same unfolded net. With a box of
 * {@code w x h x d} at region origin {@code (u, v)} the net is:
 *
 * <pre>
 *          +---------+---------+
 *          |   top   | bottom  |          rows v .. v+d-1
 * +--------+---------+---------+---------+
 * | right  |  front  |  left   |  back   |  rows v+d .. v+d+h-1
 * +--------+---------+---------+---------+
 *  u..u+d   +w        +w        +d
 * </pre>
 *
 * <p>The side strip wraps continuously around the box, so within the strip the
 * texture runs right side (back edge to front edge), front (statue's right to
 * statue's left), left side (front edge to back edge) and finally the back
 * (statue's left to statue's right) — which is why the back face is mirrored
 * relative to the front. The top face's bottom row touches the front face, so
 * its rows run back to front; the bottom face is the vertical mirror of that.
 *
 * <p>Local voxel coordinates are box relative: {@code lx} grows towards the
 * statue's left, {@code ly} grows downwards from the top of the box and
 * {@code lz} grows backwards from the front of the box.
 */
public final class TextureMapper {

    private TextureMapper() {
    }

    /** A texel position in 64-pixel-wide skin space. */
    public record Texel(int u, int v) {
    }

    public static Texel texel(ModelBox box, TextureRegion region, BoxFace face, int lx, int ly, int lz) {
        int w = box.texWidth();
        int h = box.texHeight();
        int d = box.texDepth();

        // Outer-layer boxes are one pixel larger on every side than the texture
        // they sample, so the extra ring repeats the nearest border texel.
        int tx = clamp(lx - box.inflate(), w);
        int ty = clamp(ly - box.inflate(), h);
        int tz = clamp(lz - box.inflate(), d);

        return texelAt(box, region, face, tx, ty, tz);
    }

    /**
     * Maps already-normalised texture coordinates to a face texel. This is
     * used by world-space outer shells, whose surface pixels are scaled while
     * their outward displacement remains one block.
     */
    public static Texel texelAt(ModelBox box, TextureRegion region, BoxFace face,
                                int tx, int ty, int tz) {
        int w = box.texWidth();
        int h = box.texHeight();
        int d = box.texDepth();

        tx = clamp(tx, w);
        ty = clamp(ty, h);
        tz = clamp(tz, d);

        BoxFace sampled = face;
        if (region.mirrored()) {
            tx = w - 1 - tx;
            sampled = face.mirroredX();
        }

        int u = region.u();
        int v = region.v();
        return switch (sampled) {
            case FRONT -> new Texel(u + d + tx, v + d + ty);
            case BACK -> new Texel(u + 2 * d + w + (w - 1 - tx), v + d + ty);
            case RIGHT -> new Texel(u + (d - 1 - tz), v + d + ty);
            case LEFT -> new Texel(u + d + w + tz, v + d + ty);
            case TOP -> new Texel(u + d + tx, v + (d - 1 - tz));
            case BOTTOM -> new Texel(u + d + w + tx, v + tz);
        };
    }

    private static int clamp(int value, int size) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, size - 1);
    }
}
