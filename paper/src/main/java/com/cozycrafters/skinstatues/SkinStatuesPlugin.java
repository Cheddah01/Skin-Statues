package com.cozycrafters.skinstatues;

import com.cozycrafters.skinstatues.command.StatueCommand;
import com.cozycrafters.skinstatues.skin.HttpTextureDownloader;
import com.cozycrafters.skinstatues.skin.PaperProfileSource;
import com.cozycrafters.skinstatues.skin.SkinService;
import com.cozycrafters.skinstatues.statue.StatuePlacer;
import com.cozycrafters.skinstatues.statue.StatueService;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Turns a Minecraft player's skin into a giant block statue.
 *
 * <p>This class owns nothing but the lifecycle: settings are resolved once on
 * enable, the pipeline is wired from them, and everything is torn down on
 * disable so a reload leaves no task behind.
 */
public final class SkinStatuesPlugin extends JavaPlugin {

    private SkinService skins;
    private StatuePlacer placer;
    private StatueService statues;
    private StatuesConfig config;

    @Override
    public void onEnable() {
        config = StatuesConfig.load(this);
        skins = new SkinService(new PaperProfileSource(), new HttpTextureDownloader(), config.skinCacheMillis());
        placer = new StatuePlacer(this);
        statues = new StatueService(this, skins, placer, config);

        PluginCommand command = Objects.requireNonNull(getCommand("statue"),
                "statue command is missing from plugin.yml");
        StatueCommand executor = new StatueCommand(statues, config::maxScale);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("SkinStatues ready: max scale " + config.maxScale()
                + ", " + config.blocksPerTick() + " blocks/tick, "
                + config.palette().size() + " palette blocks.");
    }

    @Override
    public void onDisable() {
        // Guarded so a half-initialised plugin can never abort server shutdown.
        try {
            if (placer != null) {
                placer.cancelAll();
            }
            if (statues != null) {
                statues.clear();
            }
            if (skins != null) {
                skins.clear();
            }
        } catch (Throwable ex) {
            getLogger().log(Level.SEVERE, "SkinStatues shutdown cleanup failed.", ex);
        }
        statues = null;
        placer = null;
        skins = null;
        config = null;
    }

    public StatuesConfig settings() {
        return config;
    }

    public StatueService statues() {
        return statues;
    }
}
