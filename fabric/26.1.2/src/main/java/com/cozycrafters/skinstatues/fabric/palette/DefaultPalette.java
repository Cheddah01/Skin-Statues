package com.cozycrafters.skinstatues.fabric.palette;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The curated built-in palette.
 *
 * <p>Every entry comes from one of three visually coherent pixel-art families
 * — concrete, terracotta, and wool — plus a small curated set of quiet natural
 * tones that fill the cream/peach/tan gap the dyed families leave, which is the
 * range player faces live in. Metallic, mineral, decorative, and strongly
 * patterned blocks are deliberately excluded even when their average colour is
 * numerically closer to a skin texel.
 *
 * <p>Colours represent each block's in-world appearance. They are only ever
 * used for nearest-colour matching, so small inaccuracies shift a pixel to a
 * neighbouring block rather than breaking anything. The built-in values were
 * derived from the Minecraft 26.1.2 textures and calibrated for visual matching;
 * tune them, or extend the palette, from {@code config/skinstatues.json}.
 */
public final class DefaultPalette {

    private DefaultPalette() {
    }

    public static Map<Block, Integer> colors() {
        Map<Block, Integer> colors = new LinkedHashMap<>();

        // Concrete: flat, saturated colours for clothing, hair, and strong shading.
        colors.put(Blocks.WHITE_CONCRETE, 0xCFD5D6);
        colors.put(Blocks.ORANGE_CONCRETE, 0xE06100);
        colors.put(Blocks.MAGENTA_CONCRETE, 0xA9309F);
        colors.put(Blocks.LIGHT_BLUE_CONCRETE, 0x2389C6);
        colors.put(Blocks.YELLOW_CONCRETE, 0xF0AF15);
        colors.put(Blocks.LIME_CONCRETE, 0x5EA818);
        colors.put(Blocks.PINK_CONCRETE, 0xD5658E);
        colors.put(Blocks.GRAY_CONCRETE, 0x36393D);
        colors.put(Blocks.LIGHT_GRAY_CONCRETE, 0x7D7D73);
        colors.put(Blocks.CYAN_CONCRETE, 0x157788);
        colors.put(Blocks.PURPLE_CONCRETE, 0x641F9C);
        colors.put(Blocks.BLUE_CONCRETE, 0x2C2E8F);
        colors.put(Blocks.BROWN_CONCRETE, 0x603B1F);
        colors.put(Blocks.GREEN_CONCRETE, 0x495B24);
        colors.put(Blocks.RED_CONCRETE, 0x8E2020);
        colors.put(Blocks.BLACK_CONCRETE, 0x080A0F);

        // Terracotta: muted mid-tones with especially useful skin and hair coverage.
        colors.put(Blocks.TERRACOTTA, 0x985E43);
        colors.put(Blocks.WHITE_TERRACOTTA, 0xD1B2A1);
        colors.put(Blocks.ORANGE_TERRACOTTA, 0xA15325);
        colors.put(Blocks.MAGENTA_TERRACOTTA, 0x95586C);
        colors.put(Blocks.LIGHT_BLUE_TERRACOTTA, 0x716C89);
        colors.put(Blocks.YELLOW_TERRACOTTA, 0xBA8523);
        colors.put(Blocks.LIME_TERRACOTTA, 0x677534);
        colors.put(Blocks.PINK_TERRACOTTA, 0xA14E4E);
        colors.put(Blocks.GRAY_TERRACOTTA, 0x392A23);
        colors.put(Blocks.LIGHT_GRAY_TERRACOTTA, 0x876A61);
        colors.put(Blocks.CYAN_TERRACOTTA, 0x565B5B);
        colors.put(Blocks.PURPLE_TERRACOTTA, 0x764656);
        colors.put(Blocks.BLUE_TERRACOTTA, 0x4A3B5B);
        colors.put(Blocks.BROWN_TERRACOTTA, 0x4D3323);
        colors.put(Blocks.GREEN_TERRACOTTA, 0x4C532A);
        colors.put(Blocks.RED_TERRACOTTA, 0x8F3D2E);
        colors.put(Blocks.BLACK_TERRACOTTA, 0x251610);

        // Wool: soft highlights and intermediate saturated colours that improve coverage.
        colors.put(Blocks.WHITE_WOOL, 0xE9ECEC);
        colors.put(Blocks.ORANGE_WOOL, 0xF07613);
        colors.put(Blocks.MAGENTA_WOOL, 0xBD44B3);
        colors.put(Blocks.LIGHT_BLUE_WOOL, 0x3AAFD9);
        colors.put(Blocks.YELLOW_WOOL, 0xF8C527);
        colors.put(Blocks.LIME_WOOL, 0x70B919);
        colors.put(Blocks.PINK_WOOL, 0xED8DAC);
        colors.put(Blocks.GRAY_WOOL, 0x3E4447);
        colors.put(Blocks.LIGHT_GRAY_WOOL, 0x8E8E86);
        colors.put(Blocks.CYAN_WOOL, 0x158991);
        colors.put(Blocks.PURPLE_WOOL, 0x7B4AA8);
        colors.put(Blocks.BLUE_WOOL, 0x35399D);
        colors.put(Blocks.BROWN_WOOL, 0x724728);
        colors.put(Blocks.GREEN_WOOL, 0x546D1B);
        colors.put(Blocks.RED_WOOL, 0xA02722);
        colors.put(Blocks.BLACK_WOOL, 0x141519);

        // Natural tones: the dyed families jump from white terracotta straight
        // to terracotta, which turns pale and tan skin muddy. These four quiet
        // blocks fill that cream-to-tan ramp without reintroducing metallic or
        // patterned materials.
        colors.put(Blocks.SMOOTH_QUARTZ, 0xEDE6E0);
        colors.put(Blocks.SMOOTH_SANDSTONE, 0xE0D6AA);
        colors.put(Blocks.STRIPPED_JUNGLE_LOG, 0xAB8555);
        colors.put(Blocks.BROWN_MUSHROOM_BLOCK, 0x957051);

        return colors;
    }
}
