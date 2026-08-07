package com.cozycrafters.skinstatues.palette;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;

/**
 * The curated built-in palette.
 *
 * <p>Every entry comes from one of three visually coherent pixel-art families:
 * concrete, terracotta, and wool. Metallic, mineral, decorative, directional,
 * and strongly patterned blocks are deliberately excluded even when their
 * average colour is numerically closer to a skin texel.
 *
 * <p>Colours represent each block's in-world appearance. They are only ever
 * used for nearest-colour matching, so small inaccuracies shift a pixel to a
 * neighbouring block rather than breaking anything. The built-in values were
 * derived from the Minecraft 26.1.2 textures and calibrated for visual matching;
 * tune them, or extend the palette, from {@code config.yml}.
 */
public final class DefaultPalette {

    private DefaultPalette() {
    }

    public static Map<Material, Integer> colors() {
        Map<Material, Integer> colors = new LinkedHashMap<>();

        // Concrete: flat, saturated colours for clothing, hair, and strong shading.
        colors.put(Material.WHITE_CONCRETE, 0xCFD5D6);
        colors.put(Material.ORANGE_CONCRETE, 0xE06100);
        colors.put(Material.MAGENTA_CONCRETE, 0xA9309F);
        colors.put(Material.LIGHT_BLUE_CONCRETE, 0x2389C6);
        colors.put(Material.YELLOW_CONCRETE, 0xF0AF15);
        colors.put(Material.LIME_CONCRETE, 0x5EA818);
        colors.put(Material.PINK_CONCRETE, 0xD5658E);
        colors.put(Material.GRAY_CONCRETE, 0x36393D);
        colors.put(Material.LIGHT_GRAY_CONCRETE, 0x7D7D73);
        colors.put(Material.CYAN_CONCRETE, 0x157788);
        colors.put(Material.PURPLE_CONCRETE, 0x641F9C);
        colors.put(Material.BLUE_CONCRETE, 0x2C2E8F);
        colors.put(Material.BROWN_CONCRETE, 0x603B1F);
        colors.put(Material.GREEN_CONCRETE, 0x495B24);
        colors.put(Material.RED_CONCRETE, 0x8E2020);
        colors.put(Material.BLACK_CONCRETE, 0x080A0F);

        // Terracotta: muted mid-tones with especially useful skin and hair coverage.
        colors.put(Material.TERRACOTTA, 0x985E43);
        colors.put(Material.WHITE_TERRACOTTA, 0xD1B2A1);
        colors.put(Material.ORANGE_TERRACOTTA, 0xA15325);
        colors.put(Material.MAGENTA_TERRACOTTA, 0x95586C);
        colors.put(Material.LIGHT_BLUE_TERRACOTTA, 0x716C89);
        colors.put(Material.YELLOW_TERRACOTTA, 0xBA8523);
        colors.put(Material.LIME_TERRACOTTA, 0x677534);
        colors.put(Material.PINK_TERRACOTTA, 0xA14E4E);
        colors.put(Material.GRAY_TERRACOTTA, 0x392A23);
        colors.put(Material.LIGHT_GRAY_TERRACOTTA, 0x876A61);
        colors.put(Material.CYAN_TERRACOTTA, 0x565B5B);
        colors.put(Material.PURPLE_TERRACOTTA, 0x764656);
        colors.put(Material.BLUE_TERRACOTTA, 0x4A3B5B);
        colors.put(Material.BROWN_TERRACOTTA, 0x4D3323);
        colors.put(Material.GREEN_TERRACOTTA, 0x4C532A);
        colors.put(Material.RED_TERRACOTTA, 0x8F3D2E);
        colors.put(Material.BLACK_TERRACOTTA, 0x251610);

        // Wool: soft highlights and intermediate saturated colours that improve coverage.
        colors.put(Material.WHITE_WOOL, 0xE9ECEC);
        colors.put(Material.ORANGE_WOOL, 0xF07613);
        colors.put(Material.MAGENTA_WOOL, 0xBD44B3);
        colors.put(Material.LIGHT_BLUE_WOOL, 0x3AAFD9);
        colors.put(Material.YELLOW_WOOL, 0xF8C527);
        colors.put(Material.LIME_WOOL, 0x70B919);
        colors.put(Material.PINK_WOOL, 0xED8DAC);
        colors.put(Material.GRAY_WOOL, 0x3E4447);
        colors.put(Material.LIGHT_GRAY_WOOL, 0x8E8E86);
        colors.put(Material.CYAN_WOOL, 0x158991);
        colors.put(Material.PURPLE_WOOL, 0x7B4AA8);
        colors.put(Material.BLUE_WOOL, 0x35399D);
        colors.put(Material.BROWN_WOOL, 0x724728);
        colors.put(Material.GREEN_WOOL, 0x546D1B);
        colors.put(Material.RED_WOOL, 0xA02722);
        colors.put(Material.BLACK_WOOL, 0x141519);

        return colors;
    }
}
