package com.cozycrafters.cozystatues.statue;

import com.cozycrafters.cozystatues.model.BodyPart;
import com.cozycrafters.cozystatues.model.BoxFace;
import com.cozycrafters.cozystatues.model.ModelBounds;
import com.cozycrafters.cozystatues.model.ModelBox;
import com.cozycrafters.cozystatues.model.PlayerModel;
import com.cozycrafters.cozystatues.model.TextureMapper;
import com.cozycrafters.cozystatues.model.TextureRegion;
import com.cozycrafters.cozystatues.palette.BlockPalette;
import com.cozycrafters.cozystatues.skin.SkinTexture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/**
 * Turns a skin plus a model into the list of blocks that make up a statue.
 *
 * <p>Entirely pure: no Bukkit world is touched, nothing is scheduled, and the
 * same inputs always produce the same plan. It runs off the server thread.
 *
 * <p>The statue is a shell. Only voxels on the outside of each body box are
 * placed, which is what makes even a maximum-scale statue affordable; at scale
 * {@code s} that base shell is {@code s} blocks thick, so it still reads as
 * solid. Outer-layer texture pixels scale across the surface normally, but
 * their shell is inflated in final block space so it always sits exactly one
 * block outside the base body part.
 */
public final class StatuePlanner {

    private final BlockPalette palette;

    public StatuePlanner(BlockPalette palette) {
        this.palette = palette;
    }

    /** One skin pixel of the statue, in normalised non-negative model space. */
    public record PlannedPixel(int x, int y, int z, Material material) {
    }

    /** Final block dimensions, including the fixed outer-layer margin. */
    public record Dimensions(int width, int height, int depth) {
    }

    /**
     * The statue at scale 1, in model pixels.
     *
     * <p>Boxes are visited base layer first, so an outer-layer voxel wins any
     * block the two layers would both claim — the jacket should hide the shirt,
     * not the other way round.
     */
    public List<PlannedPixel> planPixels(SkinTexture skin, PlayerModel model, ModelBounds bounds) {
        Map<Integer, Material> voxels = new LinkedHashMap<>();
        for (ModelBox box : model.boxes()) {
            TextureRegion region = skin.layout().region(box.part(), box.overlay());
            if (region == null) {
                // Legacy skins have no outer layer beyond the hat.
                continue;
            }
            addBox(skin, box, region, bounds, voxels);
        }

        List<PlannedPixel> pixels = new ArrayList<>(voxels.size());
        voxels.forEach((key, material) -> pixels.add(
                new PlannedPixel((key >> 16) & 0xFF, (key >> 8) & 0xFF, key & 0xFF, material)));
        // Bottom up, so the statue visibly grows from the feet while it builds.
        pixels.sort(Comparator.comparingInt(PlannedPixel::y)
                .thenComparingInt(PlannedPixel::x)
                .thenComparingInt(PlannedPixel::z));
        return pixels;
    }

    /**
     * The world-absolute build order.
     *
     * @param worldMinY lowest buildable Y, inclusive
     * @param worldMaxY highest buildable Y, inclusive; blocks above are dropped
     *                  rather than clamped, so a statue near the build limit is
     *                  simply cut off instead of squashed
     */
    public StatuePlan plan(SkinTexture skin, PlayerModel model, int scale,
                           StatuePlacement placement, int worldMinY, int worldMaxY) {
        validateScale(scale);
        ModelBounds baseBounds = baseBounds(model);
        Dimensions dimensions = dimensions(model, scale);
        int margin = model.outerLayer() ? 1 : 0;
        int scaledMinX = baseBounds.minX() * scale - margin;
        int scaledMinZ = baseBounds.minZ() * scale - margin;
        int floorY = baseBounds.minY() * scale;

        List<PlannedPixel> basePixels = planBasePixels(skin, model, baseBounds);
        int overlayCapacity = estimateOverlayCapacity(model, scale);
        int capacity = basePixels.size() * scale * scale * scale + overlayCapacity;
        long[] positions = new long[capacity];
        Material[] materials = new Material[capacity];
        OverlayBlocks overlays = buildOverlays(skin, model, scale, baseBounds,
                scaledMinX, scaledMinZ, dimensions, overlayCapacity);
        Map<Integer, Material> materialCache = new HashMap<>();
        int count = 0;

        // Emit one complete Y layer at a time so the statue still grows from
        // its feet upward after overlays move into final block space.
        for (int by = 0; by < dimensions.height(); by++) {
            int rawY = floorY + by;
            int modelY = Math.floorDiv(rawY, scale) - baseBounds.minY();
            int worldY = placement.worldY(by);
            if (worldY >= worldMinY && worldY <= worldMaxY
                    && modelY >= 0 && modelY < baseBounds.height()) {
                for (PlannedPixel pixel : basePixels) {
                    if (pixel.y() != modelY) {
                        continue;
                    }
                    int rawX = (baseBounds.minX() + pixel.x()) * scale;
                    int rawZ = (baseBounds.minZ() + pixel.z()) * scale;
                    for (int dx = 0; dx < scale; dx++) {
                        int bx = rawX + dx - scaledMinX;
                        for (int dz = 0; dz < scale; dz++) {
                            int bz = rawZ + dz - scaledMinZ;
                            if (overlays.contains(bx, by, bz)) {
                                continue;
                            }
                            positions[count] = StatuePlan.pack(
                                    placement.worldX(bx, bz), worldY, placement.worldZ(bx, bz));
                            materials[count] = pixel.material();
                            count++;
                        }
                    }
                }
            }

            if (worldY < worldMinY || worldY > worldMaxY) {
                continue;
            }
            for (int bx = 0; bx < dimensions.width(); bx++) {
                for (int bz = 0; bz < dimensions.depth(); bz++) {
                    int rgb = overlays.rgbAt(bx, by, bz);
                    if (rgb == SkinTexture.TRANSPARENT) {
                        continue;
                    }
                    positions[count] = StatuePlan.pack(
                            placement.worldX(bx, bz), worldY, placement.worldZ(bx, bz));
                    materials[count] = materialCache.computeIfAbsent(rgb, palette::nearest);
                    count++;
                }
            }
        }

        return new StatuePlan(positions, materials, count,
                dimensions.width(), dimensions.height(), dimensions.depth());
    }

    /** Dimensions needed by placement before the absolute plan is built. */
    public Dimensions dimensions(PlayerModel model, int scale) {
        validateScale(scale);
        ModelBounds bounds = baseBounds(model);
        int margin = model.outerLayer() ? 1 : 0;
        return new Dimensions(bounds.width() * scale + margin * 2,
                bounds.height() * scale + margin,
                bounds.depth() * scale + margin * 2);
    }

    private List<PlannedPixel> planBasePixels(SkinTexture skin, PlayerModel model, ModelBounds bounds) {
        Map<Integer, Material> voxels = new LinkedHashMap<>();
        for (ModelBox box : model.boxes()) {
            if (box.overlay()) {
                continue;
            }
            TextureRegion region = skin.layout().region(box.part(), false);
            addBox(skin, box, region, bounds, voxels);
        }
        List<PlannedPixel> pixels = new ArrayList<>(voxels.size());
        voxels.forEach((key, material) -> pixels.add(
                new PlannedPixel((key >> 16) & 0xFF, (key >> 8) & 0xFF, key & 0xFF, material)));
        pixels.sort(Comparator.comparingInt(PlannedPixel::y)
                .thenComparingInt(PlannedPixel::x)
                .thenComparingInt(PlannedPixel::z));
        return pixels;
    }

    private OverlayBlocks buildOverlays(SkinTexture skin, PlayerModel model, int scale,
                                         ModelBounds baseBounds, int scaledMinX, int scaledMinZ,
                                         Dimensions dimensions, int expectedBlocks) {
        OverlayBlocks blocks = new OverlayBlocks(
                dimensions.height(), dimensions.depth(), expectedBlocks);
        if (!model.outerLayer()) {
            return blocks;
        }

        Map<BodyPart, ModelBox> bases = new EnumMap<>(BodyPart.class);
        for (ModelBox box : model.boxes()) {
            if (!box.overlay()) {
                bases.put(box.part(), box);
            }
        }

        int floorY = baseBounds.minY() * scale;
        for (ModelBox overlay : model.boxes()) {
            if (!overlay.overlay()) {
                continue;
            }
            TextureRegion region = skin.layout().region(overlay.part(), true);
            if (region == null) {
                // Legacy skins have no outer layer beyond the hat.
                continue;
            }
            ModelBox base = bases.get(overlay.part());
            addScaledOverlay(skin, base, overlay, region, scale, floorY,
                    scaledMinX, scaledMinZ, bases.values(), blocks);
        }
        return blocks;
    }

    private void addScaledOverlay(SkinTexture skin, ModelBox base, ModelBox overlay,
                                  TextureRegion region, int scale, int floorY,
                                  int scaledMinX, int scaledMinZ,
                                  Iterable<ModelBox> baseBoxes, OverlayBlocks blocks) {
        int minX = base.x() * scale - 1;
        int maxX = (base.x() + base.width()) * scale;
        int minY = base.y() * scale - 1;
        int maxY = (base.y() + base.height()) * scale;
        int minZ = base.z() * scale - 1;
        int maxZ = (base.z() + base.depth()) * scale;

        for (int y = Math.max(minY, floorY); y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x != minX && x != maxX && y != minY && y != maxY
                            && z != minZ && z != maxZ) {
                        continue;
                    }
                    // A displaced jacket or sleeve face can meet another body
                    // part. It is not visible there and must not replace a base
                    // block from that adjoining part.
                    if (insideAnyBase(x, y, z, scale, baseBoxes)) {
                        continue;
                    }
                    int rgb = sampleScaledOverlay(skin, base, overlay, region, scale,
                            x, y, z, minX, maxX, minY, maxY, minZ, maxZ);
                    if (rgb != SkinTexture.TRANSPARENT) {
                        blocks.put(x - scaledMinX, y - floorY, z - scaledMinZ, rgb);
                    }
                }
            }
        }
    }

    private static boolean insideAnyBase(int x, int y, int z, int scale, Iterable<ModelBox> boxes) {
        for (ModelBox box : boxes) {
            if (x >= box.x() * scale && x < (box.x() + box.width()) * scale
                    && y >= box.y() * scale && y < (box.y() + box.height()) * scale
                    && z >= box.z() * scale && z < (box.z() + box.depth()) * scale) {
                return true;
            }
        }
        return false;
    }

    private static int sampleScaledOverlay(SkinTexture skin, ModelBox base, ModelBox overlay,
                                           TextureRegion region, int scale,
                                           int x, int y, int z,
                                           int minX, int maxX, int minY, int maxY,
                                           int minZ, int maxZ) {
        int tx = Math.floorDiv(x - base.x() * scale, scale);
        int ty = Math.floorDiv((base.y() + base.height()) * scale - 1 - y, scale);
        int tz = Math.floorDiv(z - base.z() * scale, scale);
        for (BoxFace face : BoxFace.PRIORITY) {
            boolean onFace = switch (face) {
                case FRONT -> z == minZ;
                case BACK -> z == maxZ;
                case RIGHT -> x == minX;
                case LEFT -> x == maxX;
                case TOP -> y == maxY;
                case BOTTOM -> y == minY;
            };
            if (!onFace) {
                continue;
            }
            TextureMapper.Texel texel = TextureMapper.texelAt(overlay, region, face, tx, ty, tz);
            int rgb = skin.rgbAt(texel.u(), texel.v());
            if (rgb != SkinTexture.TRANSPARENT) {
                return rgb;
            }
        }
        return SkinTexture.TRANSPARENT;
    }

    private static ModelBounds baseBounds(PlayerModel model) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ModelBox box : model.boxes()) {
            if (box.overlay()) {
                continue;
            }
            minX = Math.min(minX, box.x());
            minZ = Math.min(minZ, box.z());
            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        return new ModelBounds(minX, 0, minZ,
                maxX - minX + 1, maxY + 1, maxZ - minZ + 1);
    }

    private static int estimateOverlayCapacity(PlayerModel model, int scale) {
        int capacity = 0;
        for (ModelBox box : model.boxes()) {
            if (!box.overlay()) {
                continue;
            }
            int width = box.texWidth() * scale + 2;
            int height = box.texHeight() * scale + 2;
            int depth = box.texDepth() * scale + 2;
            capacity += width * height * depth
                    - (width - 2) * (height - 2) * (depth - 2);
        }
        return capacity;
    }

    private static void validateScale(int scale) {
        if (scale < 1) {
            throw new IllegalArgumentException("Scale must be at least 1, got " + scale);
        }
    }

    private void addBox(SkinTexture skin, ModelBox box, TextureRegion region,
                        ModelBounds bounds, Map<Integer, Material> voxels) {
        for (int ly = 0; ly < box.height(); ly++) {
            int modelY = box.modelY(ly);
            if (modelY < bounds.minY()) {
                // Outer-layer boxes reach one pixel below the feet; that ring is
                // under the statue and would otherwise lift it off the ground.
                continue;
            }
            for (int lx = 0; lx < box.width(); lx++) {
                for (int lz = 0; lz < box.depth(); lz++) {
                    if (!box.isOnShell(lx, ly, lz)) {
                        continue;
                    }
                    int rgb = sample(skin, box, region, lx, ly, lz);
                    if (rgb == SkinTexture.TRANSPARENT) {
                        continue;
                    }
                    int nx = box.x() + lx - bounds.minX();
                    int ny = modelY - bounds.minY();
                    int nz = box.z() + lz - bounds.minZ();
                    voxels.put(nx << 16 | ny << 8 | nz, palette.nearest(rgb));
                }
            }
        }
    }

    /**
     * The colour of a shell voxel: the first opaque texel among the faces it
     * lies on, in visibility order. Falling through to the next face matters at
     * the edges of the outer layer, where a hat brim can be solid from the front
     * and transparent from the side.
     */
    private static int sample(SkinTexture skin, ModelBox box, TextureRegion region, int lx, int ly, int lz) {
        for (BoxFace face : BoxFace.PRIORITY) {
            if (!box.isOnFace(face, lx, ly, lz)) {
                continue;
            }
            TextureMapper.Texel texel = TextureMapper.texel(box, region, face, lx, ly, lz);
            int rgb = skin.rgbAt(texel.u(), texel.v());
            if (rgb != SkinTexture.TRANSPARENT) {
                return rgb;
            }
        }
        return SkinTexture.TRANSPARENT;
    }

    /** Compact primitive map; outer shells stay small even at maximum scale. */
    private static final class OverlayBlocks {
        private final int height;
        private final int depth;
        private final long[] keys;
        private final int[] rgbs;
        private final int mask;

        private OverlayBlocks(int height, int depth, int expectedSize) {
            this.height = height;
            this.depth = depth;
            int capacity = 2;
            while (capacity < expectedSize * 2) {
                capacity <<= 1;
            }
            this.keys = new long[capacity];
            this.rgbs = new int[capacity];
            this.mask = capacity - 1;
        }

        private void put(int x, int y, int z, int rgb) {
            long key = key(x, y, z);
            int slot = slot(key);
            while (keys[slot] != 0 && keys[slot] != key) {
                slot = (slot + 1) & mask;
            }
            keys[slot] = key;
            rgbs[slot] = rgb;
        }

        private boolean contains(int x, int y, int z) {
            return find(key(x, y, z)) >= 0;
        }

        private int rgbAt(int x, int y, int z) {
            int slot = find(key(x, y, z));
            return slot < 0 ? SkinTexture.TRANSPARENT : rgbs[slot];
        }

        private int find(long key) {
            int slot = slot(key);
            while (keys[slot] != 0) {
                if (keys[slot] == key) {
                    return slot;
                }
                slot = (slot + 1) & mask;
            }
            return -1;
        }

        private long key(int x, int y, int z) {
            return ((long) x * height + y) * depth + z + 1;
        }

        private int slot(long key) {
            long mixed = key ^ (key >>> 33);
            mixed *= 0xff51afd7ed558ccdl;
            mixed ^= mixed >>> 33;
            return (int) mixed & mask;
        }
    }
}
