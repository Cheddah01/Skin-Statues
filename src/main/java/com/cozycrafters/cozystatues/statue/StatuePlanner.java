package com.cozycrafters.cozystatues.statue;

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
 * {@code s} that shell is {@code s} blocks thick, so it still reads as solid.
 * Planning happens once per skin pixel and is only then multiplied out by the
 * scale, so the expensive part of the work never depends on the scale.
 */
public final class StatuePlanner {

    private final BlockPalette palette;

    public StatuePlanner(BlockPalette palette) {
        this.palette = palette;
    }

    /** One skin pixel of the statue, in normalised non-negative model space. */
    public record PlannedPixel(int x, int y, int z, Material material) {
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
        if (scale < 1) {
            throw new IllegalArgumentException("Scale must be at least 1, got " + scale);
        }
        ModelBounds bounds = model.bounds();
        List<PlannedPixel> pixels = planPixels(skin, model, bounds);

        int capacity = pixels.size() * scale * scale * scale;
        long[] positions = new long[capacity];
        Material[] materials = new Material[capacity];
        int count = 0;

        for (PlannedPixel pixel : pixels) {
            for (int dy = 0; dy < scale; dy++) {
                int by = pixel.y() * scale + dy;
                int worldY = placement.worldY(by);
                if (worldY < worldMinY || worldY > worldMaxY) {
                    continue;
                }
                for (int dx = 0; dx < scale; dx++) {
                    int bx = pixel.x() * scale + dx;
                    for (int dz = 0; dz < scale; dz++) {
                        int bz = pixel.z() * scale + dz;
                        positions[count] = StatuePlan.pack(
                                placement.worldX(bx, bz), worldY, placement.worldZ(bx, bz));
                        materials[count] = pixel.material();
                        count++;
                    }
                }
            }
        }

        return new StatuePlan(positions, materials, count,
                bounds.width() * scale, bounds.height() * scale, bounds.depth() * scale);
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
}
