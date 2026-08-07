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
        colors.put(Blocks.CONCRETE.white(), 0xFFFFFF);
        colors.put(Blocks.CONCRETE.black(), 0x000000);
        colors.put(Blocks.CONCRETE.red(), 0xFF0000);
        return BlockPalette.of(colors);
    }

    @Test
    void anExactColourMatchesItsOwnBlock() {
        BlockPalette palette = threeTone();
        assertEquals(Blocks.CONCRETE.white(), palette.nearest(0xFFFFFF));
        assertEquals(Blocks.CONCRETE.black(), palette.nearest(0x000000));
        assertEquals(Blocks.CONCRETE.red(), palette.nearest(0xFF0000));
    }

    @Test
    void anUnlistedColourFallsToTheNearestOne() {
        BlockPalette palette = threeTone();
        assertEquals(Blocks.CONCRETE.white(), palette.nearest(0xF0F0F0));
        assertEquals(Blocks.CONCRETE.black(), palette.nearest(0x0A0A0A));
        assertEquals(Blocks.CONCRETE.red(), palette.nearest(0xE01010));
    }

    @Test
    void matchingIsPerceptualRatherThanChannelwise() {
        // Pure blue and pure green are the same raw distance from black, but
        // green is far lighter, so only blue should collapse onto it.
        Map<Block, Integer> colors = new LinkedHashMap<>();
        colors.put(Blocks.CONCRETE.black(), 0x000000);
        colors.put(Blocks.CONCRETE.white(), 0xFFFFFF);
        BlockPalette palette = BlockPalette.of(colors);
        assertEquals(Blocks.CONCRETE.black(), palette.nearest(0x0000FF));
        assertEquals(Blocks.CONCRETE.white(), palette.nearest(0x00FF00));
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
        assertEquals(Blocks.CONCRETE.red(), palette.nearest(0xFFFF0000));
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
                Map.entry(0xF5F5F5, Blocks.WOOL.white()),
                Map.entry(0xE8E8E8, Blocks.WOOL.white()),
                Map.entry(0xA0A0A0, Blocks.WOOL.lightGray()),
                Map.entry(0x454545, Blocks.WOOL.gray()),
                Map.entry(0x101010, Blocks.CONCRETE.black()),
                Map.entry(0xF2C6A0, Blocks.SMOOTH_SANDSTONE),
                Map.entry(0xC58C62, Blocks.STRIPPED_JUNGLE_LOG),
                Map.entry(0x8D5524, Blocks.DYED_TERRACOTTA.orange()),
                Map.entry(0x4A2C1A, Blocks.DYED_TERRACOTTA.brown()),
                Map.entry(0xD8788C, Blocks.WOOL.pink()),
                Map.entry(0xB73535, Blocks.WOOL.red()),
                Map.entry(0xD96B27, Blocks.CONCRETE.orange()),
                Map.entry(0xE7BE55, Blocks.WOOL.yellow()),
                Map.entry(0x4E9A45, Blocks.CONCRETE.lime()),
                Map.entry(0x35A7A7, Blocks.WOOL.cyan()),
                Map.entry(0x62A9D8, Blocks.WOOL.lightBlue()),
                Map.entry(0x263B75, Blocks.DYED_TERRACOTTA.blue()),
                Map.entry(0x7B4AA8, Blocks.WOOL.purple()));
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
                Map.entry(0xE4AD83, Blocks.DYED_TERRACOTTA.white()), // warm peach
                Map.entry(0xD9B08C, Blocks.DYED_TERRACOTTA.white()), // beige
                Map.entry(0xC98C68, Blocks.STRIPPED_JUNGLE_LOG), // light tan, was TERRACOTTA
                Map.entry(0xB58D6E, Blocks.STRIPPED_JUNGLE_LOG), // medium tan, was WHITE_TERRACOTTA
                Map.entry(0xB07B54, Blocks.STRIPPED_JUNGLE_LOG), // tan-brown, was TERRACOTTA
                Map.entry(0x96714F, Blocks.BROWN_MUSHROOM_BLOCK), // shaded tan, was TERRACOTTA
                Map.entry(0xA96F50, Blocks.TERRACOTTA), // warm brown
                Map.entry(0x8D5524, Blocks.DYED_TERRACOTTA.orange()), // medium brown
                Map.entry(0x754A38, Blocks.WOOL.brown()), // dark brown
                Map.entry(0x5C3A21, Blocks.CONCRETE.brown())); // deep brown
        mappings.forEach((rgb, block) -> assertEquals(block, palette.nearest(rgb),
                () -> "unexpected skin-tone mapping for #%06X".formatted(rgb)));
    }

    @Test
    void exclusionsAndExtrasRebuildThePalette() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Blocks.CONCRETE.white()), Map.of(Blocks.CALCITE, 0xFFFFFF));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Blocks.CONCRETE.white()));
        assertEquals(Blocks.CALCITE, custom.nearest(0xFFFFFF));
    }

    @Test
    void anExtraColourRetunesAnExistingBlockInsteadOfDuplicatingIt() {
        int before = BlockPalette.defaults().size();
        BlockPalette custom = BlockPalette.custom(Set.of(), Map.of(Blocks.CONCRETE.gray(), 0x010203));
        assertEquals(before, custom.size());
        assertEquals(Blocks.CONCRETE.gray(), custom.nearest(0x010203));
    }

    @Test
    void anExcludedBlockCannotBeAddedBackAsAnExtra() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Blocks.CONCRETE.gray()), Map.of(Blocks.CONCRETE.gray(), 0x010203));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Blocks.CONCRETE.gray()));
    }

    @Test
    void anEmptyPaletteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.of(Map.of()));
    }

    private static void assertDifferentBlocks(Block a, Block b) {
        assertFalse(a == b, "expected different blocks but both matched " + a);
    }
}
