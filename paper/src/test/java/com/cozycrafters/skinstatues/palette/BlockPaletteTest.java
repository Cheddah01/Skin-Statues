package com.cozycrafters.skinstatues.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class BlockPaletteTest {

    private static BlockPalette threeTone() {
        Map<Material, Integer> colors = new LinkedHashMap<>();
        colors.put(Material.WHITE_CONCRETE, 0xFFFFFF);
        colors.put(Material.BLACK_CONCRETE, 0x000000);
        colors.put(Material.RED_CONCRETE, 0xFF0000);
        return BlockPalette.of(colors);
    }

    @Test
    void anExactColourMatchesItsOwnBlock() {
        BlockPalette palette = threeTone();
        assertEquals(Material.WHITE_CONCRETE, palette.nearest(0xFFFFFF));
        assertEquals(Material.BLACK_CONCRETE, palette.nearest(0x000000));
        assertEquals(Material.RED_CONCRETE, palette.nearest(0xFF0000));
    }

    @Test
    void anUnlistedColourFallsToTheNearestOne() {
        BlockPalette palette = threeTone();
        assertEquals(Material.WHITE_CONCRETE, palette.nearest(0xF0F0F0));
        assertEquals(Material.BLACK_CONCRETE, palette.nearest(0x0A0A0A));
        assertEquals(Material.RED_CONCRETE, palette.nearest(0xE01010));
    }

    @Test
    void matchingIsPerceptualRatherThanChannelwise() {
        // Pure blue and pure green are the same raw distance from black, but
        // green is far lighter, so only blue should collapse onto it.
        Map<Material, Integer> colors = new LinkedHashMap<>();
        colors.put(Material.BLACK_CONCRETE, 0x000000);
        colors.put(Material.WHITE_CONCRETE, 0xFFFFFF);
        BlockPalette palette = BlockPalette.of(colors);
        assertEquals(Material.BLACK_CONCRETE, palette.nearest(0x0000FF));
        assertEquals(Material.WHITE_CONCRETE, palette.nearest(0x00FF00));
    }

    @Test
    void repeatedLookupsAreStable() {
        BlockPalette palette = threeTone();
        Material first = palette.nearest(0x804020);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, palette.nearest(0x804020));
        }
    }

    @Test
    void alphaBitsInTheLookupKeyAreIgnored() {
        BlockPalette palette = threeTone();
        assertEquals(Material.RED_CONCRETE, palette.nearest(0xFFFF0000));
    }

    @Test
    void theBuiltInPaletteIsBalancedAndFreeOfDuplicates() {
        BlockPalette palette = BlockPalette.defaults();
        assertEquals(53, palette.size(),
                "the curated set contains concrete, terracotta, wool, and the natural-tone extension");

        Set<Material> materials = new java.util.HashSet<>();
        Set<Integer> colours = new java.util.HashSet<>();
        for (PaletteEntry entry : palette.entries()) {
            assertTrue(materials.add(entry.material()), "duplicate material " + entry.material());
            assertTrue(colours.add(entry.rgb()), "duplicate colour on " + entry.material());
            assertTrue(entry.rgb() >= 0 && entry.rgb() <= 0xFFFFFF, entry.material() + " colour out of range");
        }
    }

    @Test
    void theBuiltInPaletteSpansTheWholeBrightnessRange() {
        BlockPalette palette = BlockPalette.defaults();
        int darkest = Integer.MAX_VALUE;
        int lightest = Integer.MIN_VALUE;
        for (PaletteEntry entry : palette.entries()) {
            int luma = entry.red() + entry.green() + entry.blue();
            darkest = Math.min(darkest, luma);
            lightest = Math.max(lightest, luma);
        }
        assertTrue(darkest < 90, "the palette needs near-black tones for hair and outlines");
        assertTrue(lightest > 660, "the palette needs near-white tones for eyes and highlights");
    }

    @Test
    void skinTonesDoNotAllCollapseOntoOneBlock() {
        BlockPalette palette = BlockPalette.defaults();
        Material pale = palette.nearest(0xF5D6B8);
        Material mid = palette.nearest(0xC68642);
        Material dark = palette.nearest(0x5C3A21);
        assertDifferentBlocks(pale, mid);
        assertDifferentBlocks(mid, dark);
    }

    @Test
    void representativeColoursHaveStableVisualMappings() {
        BlockPalette palette = BlockPalette.defaults();
        Map<Integer, Material> mappings = Map.ofEntries(
                Map.entry(0xF5F5F5, Material.WHITE_WOOL),
                Map.entry(0xE8E8E8, Material.WHITE_WOOL),
                Map.entry(0xA0A0A0, Material.LIGHT_GRAY_WOOL),
                Map.entry(0x454545, Material.GRAY_WOOL),
                Map.entry(0x101010, Material.BLACK_CONCRETE),
                Map.entry(0xF2C6A0, Material.SMOOTH_SANDSTONE),
                Map.entry(0xC58C62, Material.STRIPPED_JUNGLE_LOG),
                Map.entry(0x8D5524, Material.ORANGE_TERRACOTTA),
                Map.entry(0x4A2C1A, Material.BROWN_TERRACOTTA),
                Map.entry(0xD8788C, Material.PINK_WOOL),
                Map.entry(0xB73535, Material.RED_WOOL),
                Map.entry(0xD96B27, Material.ORANGE_CONCRETE),
                Map.entry(0xE7BE55, Material.YELLOW_WOOL),
                Map.entry(0x4E9A45, Material.LIME_CONCRETE),
                Map.entry(0x35A7A7, Material.CYAN_WOOL),
                Map.entry(0x62A9D8, Material.LIGHT_BLUE_WOOL),
                Map.entry(0x263B75, Material.BLUE_TERRACOTTA),
                Map.entry(0x7B4AA8, Material.PURPLE_WOOL));
        mappings.forEach((rgb, material) -> assertEquals(material, palette.nearest(rgb),
                () -> "unexpected mapping for #%06X".formatted(rgb)));
    }

    @Test
    void naturalSkinTonesResolveAcrossTheWarmRamp() {
        BlockPalette palette = BlockPalette.defaults();
        // The cream-to-brown ramp a face renders through. Entries marked "was"
        // changed when the natural-tone blocks were added in front of the muddy
        // dyed fallbacks; the rest pin the tones the dyed families already
        // served well.
        Map<Integer, Material> mappings = Map.ofEntries(
                Map.entry(0xFAE7D0, Material.SMOOTH_QUARTZ), // cream, was WHITE_WOOL
                Map.entry(0xF5D6B8, Material.SMOOTH_SANDSTONE), // very pale peach, was WHITE_TERRACOTTA
                Map.entry(0xF2C6A0, Material.SMOOTH_SANDSTONE), // pale peach, was WHITE_TERRACOTTA
                Map.entry(0xE4AD83, Material.WHITE_TERRACOTTA), // warm peach
                Map.entry(0xD9B08C, Material.WHITE_TERRACOTTA), // beige
                Map.entry(0xC98C68, Material.STRIPPED_JUNGLE_LOG), // light tan, was TERRACOTTA
                Map.entry(0xB58D6E, Material.STRIPPED_JUNGLE_LOG), // medium tan, was WHITE_TERRACOTTA
                Map.entry(0xB07B54, Material.STRIPPED_JUNGLE_LOG), // tan-brown, was TERRACOTTA
                Map.entry(0x96714F, Material.BROWN_MUSHROOM_BLOCK), // shaded tan, was TERRACOTTA
                Map.entry(0xA96F50, Material.TERRACOTTA), // warm brown
                Map.entry(0x8D5524, Material.ORANGE_TERRACOTTA), // medium brown
                Map.entry(0x754A38, Material.BROWN_WOOL), // dark brown
                Map.entry(0x5C3A21, Material.BROWN_CONCRETE)); // deep brown
        mappings.forEach((rgb, material) -> assertEquals(material, palette.nearest(rgb),
                () -> "unexpected skin-tone mapping for #%06X".formatted(rgb)));
    }

    @Test
    void exclusionsAndExtrasRebuildThePalette() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Material.WHITE_CONCRETE), Map.of(Material.CALCITE, 0xFFFFFF));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Material.WHITE_CONCRETE));
        assertEquals(Material.CALCITE, custom.nearest(0xFFFFFF));
    }

    @Test
    void anExtraColourRetunesAnExistingBlockInsteadOfDuplicatingIt() {
        int before = BlockPalette.defaults().size();
        BlockPalette custom = BlockPalette.custom(Set.of(), Map.of(Material.GRAY_CONCRETE, 0x010203));
        assertEquals(before, custom.size());
        assertEquals(Material.GRAY_CONCRETE, custom.nearest(0x010203));
    }

    @Test
    void anExcludedBlockCannotBeAddedBackAsAnExtra() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Material.GRAY_CONCRETE), Map.of(Material.GRAY_CONCRETE, 0x010203));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Material.GRAY_CONCRETE));
    }

    @Test
    void anEmptyPaletteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.of(Map.of()));
    }

    private static void assertDifferentBlocks(Material a, Material b) {
        assertFalse(a == b, "expected different blocks but both matched " + a);
    }
}
