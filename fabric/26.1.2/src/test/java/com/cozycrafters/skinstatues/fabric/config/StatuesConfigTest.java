package com.cozycrafters.skinstatues.fabric.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class StatuesConfigTest {
    @TempDir
    Path directory;

    @Test
    void missingFileIsCreatedWithPaperAlignedDefaults() {
        Path file = directory.resolve("skinstatues.json");
        StatuesConfig config = StatuesConfig.load(file, LoggerFactory.getLogger("test"));
        assertTrue(Files.exists(file));
        assertEquals(4, config.maxScale());
        assertEquals(2500, config.blocksPerTick());
        assertTrue(config.outerLayer());
        assertEquals(60 * 60_000L, config.skinCacheMillis());
    }

    @Test
    void malformedJsonUsesDefaultsWithoutOverwritingTheFile() throws Exception {
        Path file = directory.resolve("skinstatues.json");
        Files.writeString(file, "{ definitely broken");
        StatuesConfig config = StatuesConfig.load(file, LoggerFactory.getLogger("test"));
        assertEquals(4, config.maxScale());
        assertEquals("{ definitely broken", Files.readString(file));
    }

    @Test
    void valuesAreClampedToDeliberateSafetyLimits() throws Exception {
        Path file = directory.resolve("skinstatues.json");
        Files.writeString(file, "{\"maxScale\":999,\"blocksPerTick\":0,\"skinCacheMinutes\":99999,\"outerLayer\":false}");
        StatuesConfig config = StatuesConfig.load(file, LoggerFactory.getLogger("test"));
        assertEquals(16, config.maxScale());
        assertEquals(1, config.blocksPerTick());
        assertEquals(1440 * 60_000L, config.skinCacheMillis());
        assertFalse(config.outerLayer());
    }

    @Test
    void malformedIndividualValuesFallBackSafely() throws Exception {
        Path file = directory.resolve("skinstatues.json");
        Files.writeString(file, "{\"maxScale\":\"nope\",\"palette\":4}");
        StatuesConfig config = StatuesConfig.load(file, LoggerFactory.getLogger("test"));
        assertEquals(4, config.maxScale());
        assertEquals(53, config.palette().size());
    }

    @Test
    void colorParsingAcceptsOnlySixDigitHex() {
        assertEquals(0x12ABEF, StatuesConfig.parseColor("#12abef"));
        assertEquals(0xFFFFFF, StatuesConfig.parseColor("FFFFFF"));
        assertEquals(-1, StatuesConfig.parseColor("#fff"));
        assertEquals(-1, StatuesConfig.parseColor("not-a-color"));
    }
}
