package com.cozycrafters.skinstatues.fabric.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class BlockPaletteTest {

    private static BlockPalette threeTone() {
        Map<Block, Integer> colors = new LinkedHashMap<>();
        colors.put(Blocks.WHITE_CONCRETE, 0xFFFFFF);
        colors.put(Blocks.BLACK_CONCRETE, 0x000000);
        colors.put(Blocks.RED_CONCRETE, 0xFF0000);
        return BlockPalette.of(colors);
    }

    @Test
    void anExactColourMatchesItsOwnBlock() {
        BlockPalette palette = threeTone();
        assertEquals(Blocks.WHITE_CONCRETE, palette.nearest(0xFFFFFF));
        assertEquals(Blocks.BLACK_CONCRETE, palette.nearest(0x000000));
        assertEquals(Blocks.RED_CONCRETE, palette.nearest(0xFF0000));
    }

    @Test
    void anUnlistedColourFallsToTheNearestOne() {
        BlockPalette palette = threeTone();
        assertEquals(Blocks.WHITE_CONCRETE, palette.nearest(0xF0F0F0));
        assertEquals(Blocks.BLACK_CONCRETE, palette.nearest(0x0A0A0A));
        assertEquals(Blocks.RED_CONCRETE, palette.nearest(0xE01010));
    }

    @Test
    void matchingIsPerceptualRatherThanChannelwise() {
        // Pure blue and pure green are the same raw distance from black, but
        // green is far lighter, so only blue should collapse onto it.
        Map<Block, Integer> colors = new LinkedHashMap<>();
        colors.put(Blocks.BLACK_CONCRETE, 0x000000);
        colors.put(Blocks.WHITE_CONCRETE, 0xFFFFFF);
        BlockPalette palette = BlockPalette.of(colors);
        assertEquals(Blocks.BLACK_CONCRETE, palette.nearest(0x0000FF));
        assertEquals(Blocks.WHITE_CONCRETE, palette.nearest(0x00FF00));
    }

    @Test
    void repeatedLookupsAreStable() {
        BlockPalette palette = threeTone();
        Block first = palette.nearest(0x804020);
        for (int i = 0; i < 5; i++) {
            assertEquals(first, palette.nearest(0x804020));
        }
    }

    @Test
    void alphaBitsInTheLookupKeyAreIgnored() {
        BlockPalette palette = threeTone();
        assertEquals(Blocks.RED_CONCRETE, palette.nearest(0xFFFF0000));
    }

    @Test
    void theBuiltInPaletteIsBalancedAndFreeOfDuplicates() {
        BlockPalette palette = BlockPalette.defaults();
        assertEquals(53, palette.size(),
                "the curated set contains concrete, terracotta, wool, and the natural-tone extension");

        Set<Block> materials = new java.util.HashSet<>();
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
        Block pale = palette.nearest(0xF5D6B8);
        Block mid = palette.nearest(0xC68642);
        Block dark = palette.nearest(0x5C3A21);
        assertDifferentBlocks(pale, mid);
        assertDifferentBlocks(mid, dark);
    }

    @Test
    void representativeColoursHaveStableVisualMappings() {
        BlockPalette palette = BlockPalette.defaults();
        Map<Integer, Block> mappings = Map.ofEntries(
                Map.entry(0xF5F5F5, Blocks.WHITE_WOOL),
                Map.entry(0xE8E8E8, Blocks.WHITE_WOOL),
                Map.entry(0xA0A0A0, Blocks.LIGHT_GRAY_WOOL),
                Map.entry(0x454545, Blocks.GRAY_WOOL),
                Map.entry(0x101010, Blocks.BLACK_CONCRETE),
                Map.entry(0xF2C6A0, Blocks.SMOOTH_SANDSTONE),
                Map.entry(0xC58C62, Blocks.STRIPPED_JUNGLE_LOG),
                Map.entry(0x8D5524, Blocks.ORANGE_TERRACOTTA),
                Map.entry(0x4A2C1A, Blocks.BROWN_TERRACOTTA),
                Map.entry(0xD8788C, Blocks.PINK_WOOL),
                Map.entry(0xB73535, Blocks.RED_WOOL),
                Map.entry(0xD96B27, Blocks.ORANGE_CONCRETE),
                Map.entry(0xE7BE55, Blocks.YELLOW_WOOL),
                Map.entry(0x4E9A45, Blocks.LIME_CONCRETE),
                Map.entry(0x35A7A7, Blocks.CYAN_WOOL),
                Map.entry(0x62A9D8, Blocks.LIGHT_BLUE_WOOL),
                Map.entry(0x263B75, Blocks.BLUE_TERRACOTTA),
                Map.entry(0x7B4AA8, Blocks.PURPLE_WOOL));
        mappings.forEach((rgb, block) -> assertEquals(block, palette.nearest(rgb),
                () -> "unexpected mapping for #%06X".formatted(rgb)));
    }

    @Test
    void naturalSkinTonesResolveAcrossTheWarmRamp() {
        BlockPalette palette = BlockPalette.defaults();
        // The cream-to-brown ramp a face renders through. Entries marked "was"
        // changed when the natural-tone blocks were added in front of the muddy
        // dyed fallbacks; the rest pin the tones the dyed families already
        // served well.
        Map<Integer, Block> mappings = Map.ofEntries(
                Map.entry(0xFAE7D0, Blocks.SMOOTH_QUARTZ), // cream, was WHITE_WOOL
                Map.entry(0xF5D6B8, Blocks.SMOOTH_SANDSTONE), // very pale peach, was WHITE_TERRACOTTA
                Map.entry(0xF2C6A0, Blocks.SMOOTH_SANDSTONE), // pale peach, was WHITE_TERRACOTTA
                Map.entry(0xE4AD83, Blocks.WHITE_TERRACOTTA), // warm peach
                Map.entry(0xD9B08C, Blocks.WHITE_TERRACOTTA), // beige
                Map.entry(0xC98C68, Blocks.STRIPPED_JUNGLE_LOG), // light tan, was TERRACOTTA
                Map.entry(0xB58D6E, Blocks.STRIPPED_JUNGLE_LOG), // medium tan, was WHITE_TERRACOTTA
                Map.entry(0xB07B54, Blocks.STRIPPED_JUNGLE_LOG), // tan-brown, was TERRACOTTA
                Map.entry(0x96714F, Blocks.BROWN_MUSHROOM_BLOCK), // shaded tan, was TERRACOTTA
                Map.entry(0xA96F50, Blocks.TERRACOTTA), // warm brown
                Map.entry(0x8D5524, Blocks.ORANGE_TERRACOTTA), // medium brown
                Map.entry(0x754A38, Blocks.BROWN_WOOL), // dark brown
                Map.entry(0x5C3A21, Blocks.BROWN_CONCRETE)); // deep brown
        mappings.forEach((rgb, block) -> assertEquals(block, palette.nearest(rgb),
                () -> "unexpected skin-tone mapping for #%06X".formatted(rgb)));
    }

    @Test
    void exclusionsAndExtrasRebuildThePalette() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Blocks.WHITE_CONCRETE), Map.of(Blocks.CALCITE, 0xFFFFFF));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Blocks.WHITE_CONCRETE));
        assertEquals(Blocks.CALCITE, custom.nearest(0xFFFFFF));
    }

    @Test
    void anExtraColourRetunesAnExistingBlockInsteadOfDuplicatingIt() {
        int before = BlockPalette.defaults().size();
        BlockPalette custom = BlockPalette.custom(Set.of(), Map.of(Blocks.GRAY_CONCRETE, 0x010203));
        assertEquals(before, custom.size());
        assertEquals(Blocks.GRAY_CONCRETE, custom.nearest(0x010203));
    }

    @Test
    void anExcludedBlockCannotBeAddedBackAsAnExtra() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Blocks.GRAY_CONCRETE), Map.of(Blocks.GRAY_CONCRETE, 0x010203));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Blocks.GRAY_CONCRETE));
    }

    @Test
    void anEmptyPaletteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.of(Map.of()));
    }

    private static void assertDifferentBlocks(Block a, Block b) {
        assertFalse(a == b, "expected different blocks but both matched " + a);
    }
}
