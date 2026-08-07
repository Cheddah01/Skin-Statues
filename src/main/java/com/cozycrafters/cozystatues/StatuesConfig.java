package com.cozycrafters.cozystatues;

import com.cozycrafters.cozystatues.palette.BlockPalette;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The plugin's settings, resolved once on enable.
 *
 * <p>Loading is merge safe: the packaged defaults are layered underneath the
 * administrator's file, so new keys added in later versions appear on their own
 * without overwriting anything already customised.
 */
public record StatuesConfig(
        int maxScale,
        int blocksPerTick,
        boolean outerLayer,
        long skinCacheMillis,
        BlockPalette palette
) {

    /** A scale of 16 is a 528 block tall statue; nothing sensible needs more. */
    public static final int HARD_MAX_SCALE = 16;
    public static final int DEFAULT_MAX_SCALE = 4;
    public static final int DEFAULT_BLOCKS_PER_TICK = 2500;
    public static final int MAX_BLOCKS_PER_TICK = 50_000;
    public static final int DEFAULT_CACHE_MINUTES = 60;
    public static final int MAX_CACHE_MINUTES = 1440;

    public static StatuesConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        YamlConfiguration defaults = packagedDefaults(plugin);
        if (defaults != null) {
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
            plugin.saveConfig();
        }
        return from(config, plugin.getLogger());
    }

    /** Visible for tests: builds the settings from an already-merged tree. */
    public static StatuesConfig from(ConfigurationSection config, Logger logger) {
        return from(config, logger, Material::isBlock);
    }

    /**
     * Visible for tests, which supply their own block check: {@code isBlock}
     * resolves through the block registry and so only answers correctly on a
     * running server.
     */
    public static StatuesConfig from(ConfigurationSection config, Logger logger, Predicate<Material> isBlock) {
        int maxScale = clamp(config.getInt("max-scale", DEFAULT_MAX_SCALE), 1, HARD_MAX_SCALE);
        int blocksPerTick = clamp(config.getInt("blocks-per-tick", DEFAULT_BLOCKS_PER_TICK), 1, MAX_BLOCKS_PER_TICK);
        boolean outerLayer = config.getBoolean("outer-layer", true);
        int cacheMinutes = clamp(config.getInt("skin-cache-minutes", DEFAULT_CACHE_MINUTES), 0, MAX_CACHE_MINUTES);
        return new StatuesConfig(maxScale, blocksPerTick, outerLayer, cacheMinutes * 60_000L,
                palette(config.getConfigurationSection("palette"), logger, isBlock));
    }

    private static BlockPalette palette(ConfigurationSection section, Logger logger, Predicate<Material> isBlock) {
        if (section == null) {
            return BlockPalette.defaults();
        }

        Set<Material> excluded = new LinkedHashSet<>();
        for (String name : section.getStringList("excluded")) {
            Material material = blockMaterial(name, isBlock);
            if (material == null) {
                logger.warning("Ignoring unknown palette exclusion '" + name + "' in config.yml.");
                continue;
            }
            excluded.add(material);
        }

        Map<Material, Integer> extra = new LinkedHashMap<>();
        ConfigurationSection extraSection = section.getConfigurationSection("extra");
        if (extraSection != null) {
            for (String name : extraSection.getKeys(false)) {
                Material material = blockMaterial(name, isBlock);
                if (material == null) {
                    logger.warning("Ignoring unknown palette block '" + name + "' in config.yml.");
                    continue;
                }
                int rgb = parseColor(extraSection.getString(name));
                if (rgb < 0) {
                    logger.warning("Ignoring palette block '" + name + "': the colour must look like \"#RRGGBB\".");
                    continue;
                }
                extra.put(material, rgb);
            }
        }

        if (excluded.isEmpty() && extra.isEmpty()) {
            return BlockPalette.defaults();
        }
        try {
            return BlockPalette.custom(excluded, extra);
        } catch (IllegalArgumentException ex) {
            logger.warning("The configured palette excluded every block; falling back to the built-in palette.");
            return BlockPalette.defaults();
        }
    }

    /**
     * Resolves a configured material name, accepting {@code minecraft:} prefixes
     * and either case. Returns {@code null} when the name is not a real block.
     */
    public static Material blockMaterial(String raw) {
        return blockMaterial(raw, Material::isBlock);
    }

    public static Material blockMaterial(String raw, Predicate<Material> isBlock) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim().toUpperCase(Locale.ROOT);
        if (name.startsWith("MINECRAFT:")) {
            name = name.substring("MINECRAFT:".length());
        }
        Material material = Material.getMaterial(name);
        return material == null || !isBlock.test(material) ? null : material;
    }

    /** Parses {@code "#RRGGBB"} (the leading hash optional) into 0xRRGGBB, or -1. */
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

    private static YamlConfiguration packagedDefaults(JavaPlugin plugin) {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not read the packaged config.yml defaults: " + ex.getMessage());
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
