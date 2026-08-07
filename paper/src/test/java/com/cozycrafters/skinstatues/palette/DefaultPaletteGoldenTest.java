package com.cozycrafters.skinstatues.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DefaultPaletteGoldenTest {

    private static final List<String> CURATED_PALETTE = List.of(
            "white_concrete=#CFD5D6",
            "orange_concrete=#E06100",
            "magenta_concrete=#A9309F",
            "light_blue_concrete=#2389C6",
            "yellow_concrete=#F0AF15",
            "lime_concrete=#5EA818",
            "pink_concrete=#D5658E",
            "gray_concrete=#36393D",
            "light_gray_concrete=#7D7D73",
            "cyan_concrete=#157788",
            "purple_concrete=#641F9C",
            "blue_concrete=#2C2E8F",
            "brown_concrete=#603B1F",
            "green_concrete=#495B24",
            "red_concrete=#8E2020",
            "black_concrete=#080A0F",
            "terracotta=#985E43",
            "white_terracotta=#D1B2A1",
            "orange_terracotta=#A15325",
            "magenta_terracotta=#95586C",
            "light_blue_terracotta=#716C89",
            "yellow_terracotta=#BA8523",
            "lime_terracotta=#677534",
            "pink_terracotta=#A14E4E",
            "gray_terracotta=#392A23",
            "light_gray_terracotta=#876A61",
            "cyan_terracotta=#565B5B",
            "purple_terracotta=#764656",
            "blue_terracotta=#4A3B5B",
            "brown_terracotta=#4D3323",
            "green_terracotta=#4C532A",
            "red_terracotta=#8F3D2E",
            "black_terracotta=#251610",
            "white_wool=#E9ECEC",
            "orange_wool=#F07613",
            "magenta_wool=#BD44B3",
            "light_blue_wool=#3AAFD9",
            "yellow_wool=#F8C527",
            "lime_wool=#70B919",
            "pink_wool=#ED8DAC",
            "gray_wool=#3E4447",
            "light_gray_wool=#8E8E86",
            "cyan_wool=#158991",
            "purple_wool=#7B4AA8",
            "blue_wool=#35399D",
            "brown_wool=#724728",
            "green_wool=#546D1B",
            "red_wool=#A02722",
            "black_wool=#141519");

    @Test
    void defaultPaletteMatchesTheCanonicalOrderedDefinition() {
        List<String> actual = DefaultPalette.colors().entrySet().stream()
                .map(entry -> entry.getKey().name().toLowerCase(Locale.ROOT)
                        + "=#%06X".formatted(entry.getValue()))
                .toList();
        assertEquals(CURATED_PALETTE, actual);
    }
}
