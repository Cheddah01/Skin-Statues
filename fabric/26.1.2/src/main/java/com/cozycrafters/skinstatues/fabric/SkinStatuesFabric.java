package com.cozycrafters.skinstatues.fabric;

import com.cozycrafters.skinstatues.fabric.command.StatueCommand;
import com.cozycrafters.skinstatues.fabric.config.StatuesConfig;
import com.cozycrafters.skinstatues.fabric.statue.StatueRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fabric lifecycle entry point for the server-authoritative SkinStatues edition. */
public final class SkinStatuesFabric implements ModInitializer {
    public static final String MOD_ID = "skinstatues";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkinStatues");

    private StatuesConfig config;
    private volatile StatueRuntime runtime;

    @Override
    public void onInitialize() {
        config = StatuesConfig.load(FabricLoader.getInstance().getConfigDir().resolve("skinstatues.json"), LOGGER);
        StatueCommand command = new StatueCommand(() -> runtime, config::maxScale);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> command.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            runtime = new StatueRuntime(server, config, LOGGER);
            LOGGER.info("SkinStatues ready: max scale {}, {} blocks/tick, {} palette blocks.",
                    config.maxScale(), config.blocksPerTick(), config.palette().size());
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            StatueRuntime current = runtime;
            if (current != null) {
                current.tick();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            StatueRuntime current = runtime;
            runtime = null;
            if (current != null) {
                current.stop();
            }
        });
    }
}
