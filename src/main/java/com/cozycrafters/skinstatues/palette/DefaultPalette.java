package com.cozycrafters.skinstatues.palette;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;

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

    public static Map<Material, Integer> colors() {
        Map<Material, Integer> colors = new LinkedHashMap<>();

        // Concrete: the flattest, most saturated family, and the palette's backbone.
        colors.put(Material.WHITE_CONCRETE, 0xCFD5D6);
        colors.put(Material.ORANGE_CONCRETE, 0xE06100);
        colors.put(Material.MAGENTA_CONCRETE, 0xA9309F);
        colors.put(Material.LIGHT_BLUE_CONCRETE, 0x2489C7);
        colors.put(Material.YELLOW_CONCRETE, 0xF1AF15);
        colors.put(Material.LIME_CONCRETE, 0x5EA918);
        colors.put(Material.PINK_CONCRETE, 0xD6658F);
        colors.put(Material.GRAY_CONCRETE, 0x373A3E);
        colors.put(Material.LIGHT_GRAY_CONCRETE, 0x7D7D73);
        colors.put(Material.CYAN_CONCRETE, 0x157788);
        colors.put(Material.PURPLE_CONCRETE, 0x64209C);
        colors.put(Material.BLUE_CONCRETE, 0x2D2F8F);
        colors.put(Material.BROWN_CONCRETE, 0x603C20);
        colors.put(Material.GREEN_CONCRETE, 0x495B24);
        colors.put(Material.RED_CONCRETE, 0x8E2121);
        colors.put(Material.BLACK_CONCRETE, 0x080A0F);

        // Terracotta: the muted mid-tones, and where most skin tones land.
        colors.put(Material.TERRACOTTA, 0x985E43);
        colors.put(Material.WHITE_TERRACOTTA, 0xD2B2A1);
        colors.put(Material.ORANGE_TERRACOTTA, 0xA25426);
        colors.put(Material.MAGENTA_TERRACOTTA, 0x96586D);
        colors.put(Material.LIGHT_BLUE_TERRACOTTA, 0x716D8A);
        colors.put(Material.YELLOW_TERRACOTTA, 0xBA8523);
        colors.put(Material.LIME_TERRACOTTA, 0x687635);
        colors.put(Material.PINK_TERRACOTTA, 0xA24E4F);
        colors.put(Material.GRAY_TERRACOTTA, 0x3A2A24);
        colors.put(Material.LIGHT_GRAY_TERRACOTTA, 0x876B62);
        colors.put(Material.CYAN_TERRACOTTA, 0x575B5B);
        colors.put(Material.PURPLE_TERRACOTTA, 0x764656);
        colors.put(Material.BLUE_TERRACOTTA, 0x4A3C5B);
        colors.put(Material.BROWN_TERRACOTTA, 0x4D3324);
        colors.put(Material.GREEN_TERRACOTTA, 0x4C532A);
        colors.put(Material.RED_TERRACOTTA, 0x8F3D2F);
        colors.put(Material.BLACK_TERRACOTTA, 0x251711);

        // Stone family: greys, and the darks concrete alone cannot reach.
        colors.put(Material.STONE, 0x7D7D7D);
        colors.put(Material.ANDESITE, 0x888889);
        colors.put(Material.POLISHED_ANDESITE, 0x848786);
        colors.put(Material.DIORITE, 0xBDBDBD);
        colors.put(Material.POLISHED_DIORITE, 0xC1C2C3);
        colors.put(Material.GRANITE, 0x956756);
        colors.put(Material.POLISHED_GRANITE, 0x9A6B5A);
        colors.put(Material.CALCITE, 0xDFDFDA);
        colors.put(Material.TUFF, 0x6C6D66);
        colors.put(Material.COBBLED_DEEPSLATE, 0x4D4D50);
        colors.put(Material.POLISHED_DEEPSLATE, 0x48484A);
        colors.put(Material.DEEPSLATE_TILES, 0x373738);
        colors.put(Material.BLACKSTONE, 0x2A2429);
        colors.put(Material.POLISHED_BLACKSTONE, 0x353037);
        colors.put(Material.OBSIDIAN, 0x0F0A18);
        colors.put(Material.SMOOTH_BASALT, 0x48484E);
        colors.put(Material.DRIPSTONE_BLOCK, 0x866B5B);
        colors.put(Material.CLAY, 0xA0A6B3);
        colors.put(Material.PACKED_MUD, 0x8D6E53);
        colors.put(Material.MUD_BRICKS, 0x89694E);

        // Warm and pale tones that help with hair, skin and clothing highlights.
        colors.put(Material.QUARTZ_BLOCK, 0xEBE5DE);
        colors.put(Material.SMOOTH_SANDSTONE, 0xE0D6AB);
        colors.put(Material.SMOOTH_RED_SANDSTONE, 0xB5611F);
        colors.put(Material.END_STONE, 0xDBDE9E);
        colors.put(Material.HONEYCOMB_BLOCK, 0xE5941D);

        // Nether and end blocks for deep reds and purples.
        colors.put(Material.NETHERRACK, 0x612626);
        colors.put(Material.NETHER_BRICKS, 0x2C161A);
        colors.put(Material.RED_NETHER_BRICKS, 0x460003);
        colors.put(Material.PURPUR_BLOCK, 0xA97DA9);
        colors.put(Material.AMETHYST_BLOCK, 0x8561BF);

        // Ocean greens.
        colors.put(Material.PRISMARINE, 0x639C97);
        colors.put(Material.PRISMARINE_BRICKS, 0x63AB9E);
        colors.put(Material.DARK_PRISMARINE, 0x335B4B);
        colors.put(Material.MOSS_BLOCK, 0x596D2D);

        // Metal and mineral blocks: the brightest, most saturated options.
        colors.put(Material.COAL_BLOCK, 0x100F0F);
        colors.put(Material.IRON_BLOCK, 0xDCDCDC);
        colors.put(Material.GOLD_BLOCK, 0xF6D03D);
        colors.put(Material.DIAMOND_BLOCK, 0x62EDE4);
        colors.put(Material.EMERALD_BLOCK, 0x2ACB57);
        colors.put(Material.LAPIS_BLOCK, 0x1E438C);
        colors.put(Material.NETHERITE_BLOCK, 0x423C3F);

        // Waxed copper only: the unwaxed variants would change colour over time.
        colors.put(Material.WAXED_COPPER_BLOCK, 0xC06B4F);
        colors.put(Material.WAXED_EXPOSED_COPPER, 0xA17D67);
        colors.put(Material.WAXED_WEATHERED_COPPER, 0x6C9975);
        colors.put(Material.WAXED_OXIDIZED_COPPER, 0x52A284);

        return colors;
    }
}
