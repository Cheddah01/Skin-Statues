package com.cozycrafters.skinstatues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cozycrafters.skinstatues.palette.PaletteEntry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class StatuesConfigTest {

    private static final Logger QUIET = quietLogger();

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("SkinStatuesTest");
        logger.setLevel(Level.OFF);
        return logger;
    }

    /**
     * Material#isBlock resolves through the block registry, which only exists on
     * a running server, so these tests supply their own block check. Everything
     * else on the loading path is exercised for real.
     */
    private static final java.util.function.Predicate<Material> ANY_BLOCK = material -> true;

    private static StatuesConfig from(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception ex) {
            throw new AssertionError("test yaml did not parse", ex);
        }
        return StatuesConfig.from(config, QUIET, ANY_BLOCK);
    }

    @Test
    void anEmptyFileFallsBackToTheDocumentedDefaults() {
        StatuesConfig config = from("");
        assertEquals(StatuesConfig.DEFAULT_MAX_SCALE, config.maxScale());
        assertEquals(StatuesConfig.DEFAULT_BLOCKS_PER_TICK, config.blocksPerTick());
        assertTrue(config.outerLayer());
        assertEquals(StatuesConfig.DEFAULT_CACHE_MINUTES * 60_000L, config.skinCacheMillis());
        assertNotNull(config.palette());
    }

    @Test
    void configuredValuesAreUsed() {
        StatuesConfig config = from("""
                max-scale: 6
                blocks-per-tick: 500
                outer-layer: false
                skin-cache-minutes: 5
                """);
        assertEquals(6, config.maxScale());
        assertEquals(500, config.blocksPerTick());
        assertFalse(config.outerLayer());
        assertEquals(300_000L, config.skinCacheMillis());
    }

    @Test
    void absurdValuesAreClampedRatherThanTrusted() {
        StatuesConfig huge = from("""
                max-scale: 9999
                blocks-per-tick: 9999999
                skin-cache-minutes: 999999
                """);
        assertEquals(StatuesConfig.HARD_MAX_SCALE, huge.maxScale());
        assertEquals(StatuesConfig.MAX_BLOCKS_PER_TICK, huge.blocksPerTick());
        assertEquals(StatuesConfig.MAX_CACHE_MINUTES * 60_000L, huge.skinCacheMillis());

        StatuesConfig tiny = from("""
                max-scale: 0
                blocks-per-tick: -5
                skin-cache-minutes: -1
                """);
        assertEquals(1, tiny.maxScale());
        assertEquals(1, tiny.blocksPerTick());
        assertEquals(0L, tiny.skinCacheMillis());
    }

    @Test
    void anEmptyPaletteSectionKeepsTheBuiltInPalette() {
        StatuesConfig config = from("""
                palette:
                  excluded: []
                  extra: {}
                """);
        assertEquals(com.cozycrafters.skinstatues.palette.BlockPalette.defaults().size(), config.palette().size());
    }

    @Test
    void exclusionsAndExtrasAreApplied() {
        StatuesConfig config = from("""
                palette:
                  excluded:
                    - WHITE_CONCRETE
                    - minecraft:cyan_concrete
                  extra:
                    BONE_BLOCK: "#E1DDCD"
                """);
        assertFalse(hasMaterial(config, Material.WHITE_CONCRETE));
        assertFalse(hasMaterial(config, Material.CYAN_CONCRETE), "the minecraft: prefix is accepted");
        assertTrue(hasMaterial(config, Material.BONE_BLOCK));
    }

    @Test
    void unusableEntriesAreSkippedInsteadOfBreakingTheLoad() {
        StatuesConfig config = from("""
                palette:
                  excluded:
                    - NOT_A_REAL_BLOCK
                  extra:
                    ALSO_NOT_REAL: "#FFFFFF"
                    GRAY_CONCRETE: "purple"
                    ANDESITE: "#12345"
                """);
        assertNotNull(config.palette());
        assertTrue(hasMaterial(config, Material.GRAY_CONCRETE), "a bad colour leaves the built-in entry alone");
    }

    @Test
    void materialNamesAreResolvedLeniently() {
        assertEquals(Material.STONE, StatuesConfig.blockMaterial("stone", ANY_BLOCK));
        assertEquals(Material.STONE, StatuesConfig.blockMaterial(" Minecraft:Stone ", ANY_BLOCK));
        assertNull(StatuesConfig.blockMaterial("NOT_A_REAL_BLOCK", ANY_BLOCK));
        assertNull(StatuesConfig.blockMaterial("", ANY_BLOCK));
        assertNull(StatuesConfig.blockMaterial(null, ANY_BLOCK));
    }

    @Test
    void materialsThatAreNotBlocksAreRefused() {
        assertNull(StatuesConfig.blockMaterial("DIAMOND_SWORD", material -> material.isBlock()));
        assertNull(StatuesConfig.blockMaterial("STONE", material -> false));
    }

    @Test
    void coloursAreParsedWithOrWithoutTheHash() {
        assertEquals(0xFFFFFF, StatuesConfig.parseColor("#FFFFFF"));
        assertEquals(0xFFFFFF, StatuesConfig.parseColor("ffffff"));
        assertEquals(0x123456, StatuesConfig.parseColor("  #123456  "));
        assertEquals(0x000000, StatuesConfig.parseColor("#000000"));
    }

    @Test
    void malformedColoursAreRejected() {
        assertEquals(-1, StatuesConfig.parseColor(null));
        assertEquals(-1, StatuesConfig.parseColor(""));
        assertEquals(-1, StatuesConfig.parseColor("#FFF"));
        assertEquals(-1, StatuesConfig.parseColor("#FFFFFFF"));
        assertEquals(-1, StatuesConfig.parseColor("#GGGGGG"));
        assertEquals(-1, StatuesConfig.parseColor("rgb(1,2,3)"));
    }

    @Test
    void thePackagedDefaultsMatchTheCodeDefaults() throws Exception {
        try (InputStream stream = StatuesConfigTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(stream, "config.yml must be packaged");
            YamlConfiguration packaged = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertEquals(StatuesConfig.DEFAULT_MAX_SCALE, packaged.getInt("max-scale"));
            assertEquals(StatuesConfig.DEFAULT_BLOCKS_PER_TICK, packaged.getInt("blocks-per-tick"));
            assertEquals(StatuesConfig.DEFAULT_CACHE_MINUTES, packaged.getInt("skin-cache-minutes"));
            assertTrue(packaged.getBoolean("outer-layer"));
            assertNotNull(packaged.getConfigurationSection("palette"), "the palette section must ship");
            assertTrue(packaged.getStringList("palette.excluded").isEmpty());
        }
    }

    private static boolean hasMaterial(StatuesConfig config, Material material) {
        for (PaletteEntry entry : config.palette().entries()) {
            if (entry.material() == material) {
                return true;
            }
        }
        return false;
    }
}
