package com.cozycrafters.skinstatues.support;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.mockito.Mockito;

/**
 * ServiceLoader-provided RegistryAccess for unit tests. Paper API classes such
 * as Material#isAir resolve registries through RegistryAccessHolder; without a
 * provider the first lookup poisons org.bukkit.Registry's static init for the
 * whole test JVM. Every registry is a Mockito mock: lookups return null, which
 * is sufficient for the non-registry-backed materials the tests use.
 *
 * Registered via META-INF/services/io.papermc.paper.registry.RegistryAccess.
 */
public final class TestRegistryAccess implements RegistryAccess {

    @Override
    @SuppressWarnings({"unchecked", "removal"})
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return (Registry<T>) Mockito.mock(Registry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
        return (Registry<T>) Mockito.mock(Registry.class);
    }
}
