package com.cozycrafters.skinstatues.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Client-only registration for SkinStatues key mappings. */
final class SkinStatuesKeybinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("skinstatues", "main"));

    private SkinStatuesKeybinds() {
    }

    static KeyMapping registerOpenMenu() {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.skinstatues.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LBRACKET,
                CATEGORY));
    }
}
