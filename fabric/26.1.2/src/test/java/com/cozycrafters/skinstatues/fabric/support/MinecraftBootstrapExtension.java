package com.cozycrafters.skinstatues.fabric.support;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Boots vanilla registries before pure tests refer to concrete Blocks constants. */
public final class MinecraftBootstrapExtension implements BeforeAllCallback {
    private static boolean bootstrapped;

    @Override
    public synchronized void beforeAll(ExtensionContext context) {
        if (!bootstrapped) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            bootstrapped = true;
        }
    }
}
