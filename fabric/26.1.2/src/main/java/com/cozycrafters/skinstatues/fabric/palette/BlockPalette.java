package com.cozycrafters.skinstatues.fabric.palette;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.Block;

/**
 * Immutable set of pixel-art blocks with nearest-colour lookup.
 *
 * <p>All colour reasoning in SkinStatues lives here: the statue planner only
 * ever asks the palette which block a pixel becomes.
 */
public final class BlockPalette {

    private final List<PaletteEntry> entries;
    private final Map<Integer, Block> cache = new ConcurrentHashMap<>();

    private BlockPalette(List<PaletteEntry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("A block palette needs at least one entry.");
        }
        this.entries = List.copyOf(entries);
    }

    /** The curated built-in palette. */
    public static BlockPalette defaults() {
        return of(DefaultPalette.colors());
    }

    public static BlockPalette of(Map<Block, Integer> colors) {
        List<PaletteEntry> entries = new ArrayList<>(colors.size());
        colors.forEach((material, rgb) -> entries.add(PaletteEntry.of(material, rgb)));
        return new BlockPalette(entries);
    }

    /**
     * The built-in palette minus {@code excluded} plus {@code extra}. Extra
     * entries override a built-in colour for the same material, which is how an
     * administrator retunes a block without removing it.
     */
    public static BlockPalette custom(Set<Block> excluded, Map<Block, Integer> extra) {
        Map<Block, Integer> colors = new LinkedHashMap<>(DefaultPalette.colors());
        colors.keySet().removeAll(excluded);
        extra.forEach((material, rgb) -> {
            if (!excluded.contains(material)) {
                colors.put(material, rgb);
            }
        });
        return of(colors);
    }

    public List<PaletteEntry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    /** The block whose colour is perceptually closest to the given 0xRRGGBB value. */
    public Block nearest(int rgb) {
        return cache.computeIfAbsent(rgb & 0xFFFFFF, this::match);
    }

    private Block match(int rgb) {
        LabColor target = LabColor.fromRgb(rgb);
        PaletteEntry best = entries.get(0);
        double bestDistance = Double.MAX_VALUE;
        for (PaletteEntry entry : entries) {
            double distance = target.squaredDistance(entry.lab());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry;
            }
        }
        return best.material();
    }
}
