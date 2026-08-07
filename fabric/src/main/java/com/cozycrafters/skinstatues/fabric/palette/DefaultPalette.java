package com.cozycrafters.skinstatues.fabric.palette;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The curated built-in palette.
 *
 * <p>Every entry is a full, opaque, non-directional block that keeps the same
 * appearance on all six sides. Deliberately excluded: anything that falls
 * (concrete powder, sand, gravel), burns (wool, planks), melts (snow, ice),
 * oxidises (unwaxed copper), emits light or redstone, has an inventory, needs
 * support, is transparent, or carries an axis/facing texture (bone block,
 * basalt, plain deepslate, glazed terracotta, log-like blocks).
 *
 * <p>Colours are the average colour of each block's texture. They are only ever
 * used for nearest-colour matching, so small inaccuracies shift a pixel to a
 * neighbouring block rather than breaking anything; tune them, or extend the
 * palette, from {@code config.yml}.
 */
public final class DefaultPalette {

    private DefaultPalette() {
    }

    public static Map<Block, Integer> colors() {
        Map<Block, Integer> colors = new LinkedHashMap<>();

        // Concrete: the flattest, most saturated family, and the palette's backbone.
        colors.put(Blocks.WHITE_CONCRETE, 0xCFD5D6);
        colors.put(Blocks.ORANGE_CONCRETE, 0xE06100);
        colors.put(Blocks.MAGENTA_CONCRETE, 0xA9309F);
        colors.put(Blocks.LIGHT_BLUE_CONCRETE, 0x2489C7);
        colors.put(Blocks.YELLOW_CONCRETE, 0xF1AF15);
        colors.put(Blocks.LIME_CONCRETE, 0x5EA918);
        colors.put(Blocks.PINK_CONCRETE, 0xD6658F);
        colors.put(Blocks.GRAY_CONCRETE, 0x373A3E);
        colors.put(Blocks.LIGHT_GRAY_CONCRETE, 0x7D7D73);
        colors.put(Blocks.CYAN_CONCRETE, 0x157788);
        colors.put(Blocks.PURPLE_CONCRETE, 0x64209C);
        colors.put(Blocks.BLUE_CONCRETE, 0x2D2F8F);
        colors.put(Blocks.BROWN_CONCRETE, 0x603C20);
        colors.put(Blocks.GREEN_CONCRETE, 0x495B24);
        colors.put(Blocks.RED_CONCRETE, 0x8E2121);
        colors.put(Blocks.BLACK_CONCRETE, 0x080A0F);

        // Terracotta: the muted mid-tones, and where most skin tones land.
        colors.put(Blocks.TERRACOTTA, 0x985E43);
        colors.put(Blocks.WHITE_TERRACOTTA, 0xD2B2A1);
        colors.put(Blocks.ORANGE_TERRACOTTA, 0xA25426);
        colors.put(Blocks.MAGENTA_TERRACOTTA, 0x96586D);
        colors.put(Blocks.LIGHT_BLUE_TERRACOTTA, 0x716D8A);
        colors.put(Blocks.YELLOW_TERRACOTTA, 0xBA8523);
        colors.put(Blocks.LIME_TERRACOTTA, 0x687635);
        colors.put(Blocks.PINK_TERRACOTTA, 0xA24E4F);
        colors.put(Blocks.GRAY_TERRACOTTA, 0x3A2A24);
        colors.put(Blocks.LIGHT_GRAY_TERRACOTTA, 0x876B62);
        colors.put(Blocks.CYAN_TERRACOTTA, 0x575B5B);
        colors.put(Blocks.PURPLE_TERRACOTTA, 0x764656);
        colors.put(Blocks.BLUE_TERRACOTTA, 0x4A3C5B);
        colors.put(Blocks.BROWN_TERRACOTTA, 0x4D3324);
        colors.put(Blocks.GREEN_TERRACOTTA, 0x4C532A);
        colors.put(Blocks.RED_TERRACOTTA, 0x8F3D2F);
        colors.put(Blocks.BLACK_TERRACOTTA, 0x251711);

        // Stone family: greys, and the darks concrete alone cannot reach.
        colors.put(Blocks.STONE, 0x7D7D7D);
        colors.put(Blocks.ANDESITE, 0x888889);
        colors.put(Blocks.POLISHED_ANDESITE, 0x848786);
        colors.put(Blocks.DIORITE, 0xBDBDBD);
        colors.put(Blocks.POLISHED_DIORITE, 0xC1C2C3);
        colors.put(Blocks.GRANITE, 0x956756);
        colors.put(Blocks.POLISHED_GRANITE, 0x9A6B5A);
        colors.put(Blocks.CALCITE, 0xDFDFDA);
        colors.put(Blocks.TUFF, 0x6C6D66);
        colors.put(Blocks.COBBLED_DEEPSLATE, 0x4D4D50);
        colors.put(Blocks.POLISHED_DEEPSLATE, 0x48484A);
        colors.put(Blocks.DEEPSLATE_TILES, 0x373738);
        colors.put(Blocks.BLACKSTONE, 0x2A2429);
        colors.put(Blocks.POLISHED_BLACKSTONE, 0x353037);
        colors.put(Blocks.OBSIDIAN, 0x0F0A18);
        colors.put(Blocks.SMOOTH_BASALT, 0x48484E);
        colors.put(Blocks.DRIPSTONE_BLOCK, 0x866B5B);
        colors.put(Blocks.CLAY, 0xA0A6B3);
        colors.put(Blocks.PACKED_MUD, 0x8D6E53);
        colors.put(Blocks.MUD_BRICKS, 0x89694E);

        // Warm and pale tones that help with hair, skin and clothing highlights.
        colors.put(Blocks.QUARTZ_BLOCK, 0xEBE5DE);
        colors.put(Blocks.SMOOTH_SANDSTONE, 0xE0D6AB);
        colors.put(Blocks.SMOOTH_RED_SANDSTONE, 0xB5611F);
        colors.put(Blocks.END_STONE, 0xDBDE9E);
        colors.put(Blocks.HONEYCOMB_BLOCK, 0xE5941D);

        // Nether and end blocks for deep reds and purples.
        colors.put(Blocks.NETHERRACK, 0x612626);
        colors.put(Blocks.NETHER_BRICKS, 0x2C161A);
        colors.put(Blocks.RED_NETHER_BRICKS, 0x460003);
        colors.put(Blocks.PURPUR_BLOCK, 0xA97DA9);
        colors.put(Blocks.AMETHYST_BLOCK, 0x8561BF);

        // Ocean greens.
        colors.put(Blocks.PRISMARINE, 0x639C97);
        colors.put(Blocks.PRISMARINE_BRICKS, 0x63AB9E);
        colors.put(Blocks.DARK_PRISMARINE, 0x335B4B);
        colors.put(Blocks.MOSS_BLOCK, 0x596D2D);

        // Metal and mineral blocks: the brightest, most saturated options.
        colors.put(Blocks.COAL_BLOCK, 0x100F0F);
        colors.put(Blocks.IRON_BLOCK, 0xDCDCDC);
        colors.put(Blocks.GOLD_BLOCK, 0xF6D03D);
        colors.put(Blocks.DIAMOND_BLOCK, 0x62EDE4);
        colors.put(Blocks.EMERALD_BLOCK, 0x2ACB57);
        colors.put(Blocks.LAPIS_BLOCK, 0x1E438C);
        colors.put(Blocks.NETHERITE_BLOCK, 0x423C3F);

        // Waxed copper only: the unwaxed variants would change colour over time.
        colors.put(Blocks.WAXED_COPPER_BLOCK, 0xC06B4F);
        colors.put(Blocks.WAXED_EXPOSED_COPPER, 0xA17D67);
        colors.put(Blocks.WAXED_WEATHERED_COPPER, 0x6C9975);
        colors.put(Blocks.WAXED_OXIDIZED_COPPER, 0x52A284);

        return colors;
    }
}
