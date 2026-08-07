package com.cozycrafters.skinstatues.fabric.config;

import com.cozycrafters.skinstatues.fabric.palette.BlockPalette;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/** Fabric's small, fail-safe JSON configuration. */
public record StatuesConfig(
        int maxScale,
        int blocksPerTick,
        boolean outerLayer,
        long skinCacheMillis,
        BlockPalette palette
) {
    public static final int HARD_MAX_SCALE = 16;
    public static final int DEFAULT_MAX_SCALE = 4;
    public static final int DEFAULT_BLOCKS_PER_TICK = 2500;
    public static final int MAX_BLOCKS_PER_TICK = 50_000;
    public static final int DEFAULT_CACHE_MINUTES = 60;
    public static final int MAX_CACHE_MINUTES = 1440;

    public static StatuesConfig load(Path path, Logger logger) {
        createDefault(path, logger);
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception ex) {
            logger.error("Could not parse {}; using built-in defaults without overwriting the file: {}", path, ex.getMessage());
            return defaults();
        }

        int maxScale = clamp(integer(root, "maxScale", DEFAULT_MAX_SCALE, logger), 1, HARD_MAX_SCALE);
        int blocks = clamp(integer(root, "blocksPerTick", DEFAULT_BLOCKS_PER_TICK, logger), 1, MAX_BLOCKS_PER_TICK);
        boolean outer = bool(root, "outerLayer", true, logger);
        int cacheMinutes = clamp(integer(root, "skinCacheMinutes", DEFAULT_CACHE_MINUTES, logger), 0, MAX_CACHE_MINUTES);
        return new StatuesConfig(maxScale, blocks, outer, cacheMinutes * 60_000L,
                palette(object(root, "palette", logger), logger));
    }

    public static StatuesConfig defaults() {
        return new StatuesConfig(DEFAULT_MAX_SCALE, DEFAULT_BLOCKS_PER_TICK, true,
                DEFAULT_CACHE_MINUTES * 60_000L, BlockPalette.defaults());
    }

    public static int parseColor(String raw) {
        if (raw == null) {
            return -1;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static BlockPalette palette(JsonObject section, Logger logger) {
        if (section == null) {
            return BlockPalette.defaults();
        }
        Set<Block> excluded = new LinkedHashSet<>();
        JsonElement excludedElement = section.get("excluded");
        if (excludedElement != null && excludedElement.isJsonArray()) {
            excludedElement.getAsJsonArray().forEach(element -> {
                try {
                    Block block = block(element.getAsString());
                    if (block == null) {
                        logger.warn("Ignoring unknown palette exclusion '{}' in skinstatues.json.", element);
                    } else {
                        excluded.add(block);
                    }
                } catch (RuntimeException ex) {
                    logger.warn("Ignoring malformed palette exclusion in skinstatues.json.");
                }
            });
        }

        Map<Block, Integer> extra = new LinkedHashMap<>();
        JsonObject extras = object(section, "extra", logger);
        if (extras != null) {
            extras.entrySet().forEach(entry -> {
                Block block = block(entry.getKey());
                int color;
                try {
                    color = parseColor(entry.getValue().getAsString());
                } catch (RuntimeException ex) {
                    color = -1;
                }
                if (block == null || color < 0) {
                    logger.warn("Ignoring invalid palette extra '{}' in skinstatues.json.", entry.getKey());
                } else {
                    extra.put(block, color);
                }
            });
        }
        if (excluded.isEmpty() && extra.isEmpty()) {
            return BlockPalette.defaults();
        }
        try {
            return BlockPalette.custom(excluded, extra);
        } catch (IllegalArgumentException ex) {
            logger.warn("The configured palette excluded every block; using the built-in palette.");
            return BlockPalette.defaults();
        }
    }

    static Block block(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(value.contains(":") ? value : "minecraft:" + value);
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    private static int integer(JsonObject root, String key, int fallback, Logger logger) {
        try {
            return root.has(key) ? root.get(key).getAsInt() : fallback;
        } catch (RuntimeException ex) {
            logger.warn("Configuration key '{}' is not an integer; using {}.", key, fallback);
            return fallback;
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback, Logger logger) {
        try {
            return root.has(key) ? root.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ex) {
            logger.warn("Configuration key '{}' is not a boolean; using {}.", key, fallback);
            return fallback;
        }
    }

    private static JsonObject object(JsonObject root, String key, Logger logger) {
        if (!root.has(key)) {
            return null;
        }
        try {
            return root.getAsJsonObject(key);
        } catch (RuntimeException ex) {
            logger.warn("Configuration key '{}' is not an object; ignoring it.", key);
            return null;
        }
    }

    private static void createDefault(Path path, Logger logger) {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (InputStream input = StatuesConfig.class.getResourceAsStream("/skinstatues-default.json")) {
                if (input == null) {
                    throw new IOException("packaged default is missing");
                }
                Files.copy(input, path);
            }
        } catch (IOException ex) {
            logger.error("Could not create {}; built-in defaults will be used: {}", path, ex.getMessage());
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
