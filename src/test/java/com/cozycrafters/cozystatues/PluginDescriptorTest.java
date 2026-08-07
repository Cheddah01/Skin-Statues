package com.cozycrafters.cozystatues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cozycrafters.cozystatues.command.StatueCommand;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Guards the packaged plugin.yml: the command surface is meant to stay tiny. */
class PluginDescriptorTest {

    private static YamlConfiguration descriptor;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream stream = PluginDescriptorTest.class.getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream, "filtered plugin.yml must be on the packaged classpath");
            descriptor = new YamlConfiguration();
            descriptor.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void identityAndMainClassAreCorrect() {
        assertEquals("CozyStatues", descriptor.getString("name"));
        assertEquals(CozyStatuesPlugin.class.getName(), descriptor.getString("main"));
        assertEquals("1.21", descriptor.getString("api-version"));
        assertEquals("Cozy Crafters", descriptor.getString("author"));
    }

    @Test
    void statueIsTheOnlyCommand() {
        ConfigurationSection commands = descriptor.getConfigurationSection("commands");
        assertNotNull(commands);
        assertEquals(java.util.Set.of("statue"), commands.getKeys(false));
        assertEquals("/<command> <name> <scale> | /<command> undo",
                commands.getString("statue.usage"));
    }

    @Test
    void oneUsePermissionDefaultingToOperators() {
        ConfigurationSection permissions = descriptor.getConfigurationSection("permissions");
        assertNotNull(permissions);
        assertEquals(java.util.Set.of("cozystatues"), permissions.getKeys(false));
        assertEquals("op", descriptor.get("permissions.cozystatues.use.default"));
        assertEquals("cozystatues.use", StatueCommand.PERMISSION);
    }

    @Test
    void nothingIsDependedOn() {
        assertTrue(descriptor.getStringList("depend").isEmpty());
        assertTrue(descriptor.getStringList("softdepend").isEmpty());
        assertTrue(descriptor.getStringList("loadbefore").isEmpty());
    }

    @Test
    void theVersionPlaceholderIsFilteredAtPackageTime() {
        String version = descriptor.getString("version");
        assertNotNull(version);
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+.*"), "unfiltered version: " + version);
    }
}
