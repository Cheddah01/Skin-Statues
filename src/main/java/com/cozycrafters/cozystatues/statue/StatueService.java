package com.cozycrafters.cozystatues.statue;

import com.cozycrafters.cozystatues.StatuesConfig;
import com.cozycrafters.cozystatues.model.PlayerModel;
import com.cozycrafters.cozystatues.skin.ResolvedSkin;
import com.cozycrafters.cozystatues.skin.SkinLookupException;
import com.cozycrafters.cozystatues.skin.SkinService;
import com.cozycrafters.cozystatues.util.Text;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ties the pipeline together and owns its threading.
 *
 * <p>Everything expensive — profile lookup, texture download, image decoding
 * and statue planning — runs on an async task. The server thread is only ever
 * used to read the builder's location up front and to place blocks afterwards,
 * in batches, through {@link StatuePlacer}. Nothing is placed unless the whole
 * plan was built successfully, so a failed lookup can never leave half a statue
 * standing.
 */
public final class StatueService {

    private final JavaPlugin plugin;
    private final SkinService skins;
    private final StatuePlacer placer;
    private final StatuesConfig config;
    private final StatuePlanner planner;
    private final Set<UUID> building = ConcurrentHashMap.newKeySet();

    public StatueService(JavaPlugin plugin, SkinService skins, StatuePlacer placer, StatuesConfig config) {
        this.plugin = plugin;
        this.skins = skins;
        this.placer = placer;
        this.config = config;
        this.planner = new StatuePlanner(config.palette());
    }

    /** Starts a statue for {@code player}. Call on the server thread. */
    public void generate(Player player, String name, int scale) {
        UUID builder = player.getUniqueId();
        if (!building.add(builder)) {
            player.sendMessage(Text.error("Your last statue is still being built."));
            return;
        }

        Location origin = player.getLocation();
        World world = origin.getWorld();
        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();
        float yaw = origin.getYaw();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        player.sendMessage(Text.info("Building a statue of &f" + name + "&e at scale &f" + scale + "&e..."));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ResolvedSkin skin = skins.resolve(name);
                PlayerModel model = PlayerModel.of(skin.texture().model(), config.outerLayer());
                StatuePlanner.Dimensions dimensions = planner.dimensions(model, scale);
                StatuePlacement placement = PlacementCalculator.compute(
                        x, y, z, yaw, dimensions.width(), dimensions.depth());
                StatuePlan plan = planner.plan(skin.texture(), model, scale, placement, minY, maxY);
                sync(() -> start(player, skin, plan, world, builder));
            } catch (SkinLookupException ex) {
                // Expected, player-facing failures: no stack trace, no console spam.
                sync(() -> {
                    building.remove(builder);
                    tell(player, Text.error(ex.getMessage()));
                });
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to plan a statue of '" + name + "'.", ex);
                sync(() -> {
                    building.remove(builder);
                    tell(player, Text.error("The statue could not be built. Check the server console."));
                });
            }
        });
    }

    public int activeBuilds() {
        return building.size();
    }

    private void start(Player player, ResolvedSkin skin, StatuePlan plan, World world, UUID builder) {
        if (plan.blockCount() == 0) {
            building.remove(builder);
            tell(player, Text.error("That skin is fully transparent; there is nothing to build."));
            return;
        }
        placer.place(world, plan, config.blocksPerTick(), placed -> {
            building.remove(builder);
            if (placed < plan.blockCount()) {
                tell(player, Text.error("The statue could not be finished; its world was unloaded."));
                return;
            }
            tell(player, Text.success("Statue of &f" + skin.profile().name()
                    + "&a created &7(" + placed + " blocks, " + plan.heightBlocks() + " tall)."));
        });
    }

    private static void tell(Player player, Component message) {
        if (player.isOnline()) {
            player.sendMessage(message);
        }
    }

    private void sync(Runnable action) {
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}
