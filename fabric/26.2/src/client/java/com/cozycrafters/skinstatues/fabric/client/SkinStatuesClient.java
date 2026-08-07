package com.cozycrafters.skinstatues.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Physical-client entry point for the optional command-backed statue menu. */
public final class SkinStatuesClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("SkinStatues/Client");

    @Override
    public void onInitializeClient() {
        KeyMapping openMenu = SkinStatuesKeybinds.registerOpenMenu();
        ClientTickEvents.END_CLIENT_TICK.register(client -> openMenu(client, openMenu));
        LOGGER.info("SkinStatues client menu initialized.");
    }

    private static void openMenu(Minecraft client, KeyMapping openMenu) {
        while (openMenu.consumeClick()) {
            if (client.player == null
                    || client.level == null
                    || client.getConnection() == null
                    || client.gui.screen() != null) {
                continue;
            }

            String playerName = client.player.getGameProfile().name();
            client.gui.setScreen(new SkinStatuesScreen(playerName, hasStatueCommand(client.getConnection())));
        }
    }

    static boolean hasStatueCommand(ClientPacketListener connection) {
        return connection.getCommands().getRoot().getChild("statue") != null;
    }
}
