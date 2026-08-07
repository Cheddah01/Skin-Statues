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
    void theBuiltInPaletteIsBroadAndFreeOfDuplicates() {
        BlockPalette palette = BlockPalette.defaults();
        assertTrue(palette.size() >= 60, "a usable pixel-art palette needs plenty of tones");

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
    void exclusionsAndExtrasRebuildThePalette() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Blocks.WHITE_CONCRETE), Map.of(Blocks.CALCITE, 0xFFFFFF));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Blocks.WHITE_CONCRETE));
        assertEquals(Blocks.CALCITE, custom.nearest(0xFFFFFF));
    }

    @Test
    void anExtraColourRetunesAnExistingBlockInsteadOfDuplicatingIt() {
        int before = BlockPalette.defaults().size();
        BlockPalette custom = BlockPalette.custom(Set.of(), Map.of(Blocks.STONE, 0x010203));
        assertEquals(before, custom.size());
        assertEquals(Blocks.STONE, custom.nearest(0x010203));
    }

    @Test
    void anExcludedBlockCannotBeAddedBackAsAnExtra() {
        BlockPalette custom = BlockPalette.custom(
                Set.of(Blocks.STONE), Map.of(Blocks.STONE, 0x010203));
        assertFalse(custom.entries().stream().anyMatch(e -> e.material() == Blocks.STONE));
    }

    @Test
    void anEmptyPaletteIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.of(Map.of()));
    }

    private static void assertDifferentBlocks(Block a, Block b) {
        assertFalse(a == b, "expected different blocks but both matched " + a);
    }
}
