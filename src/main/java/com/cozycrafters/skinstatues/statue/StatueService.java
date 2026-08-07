package com.cozycrafters.skinstatues.statue;

import com.cozycrafters.skinstatues.StatuesConfig;
import com.cozycrafters.skinstatues.model.PlayerModel;
import com.cozycrafters.skinstatues.skin.ResolvedSkin;
import com.cozycrafters.skinstatues.skin.SkinLookupException;
import com.cozycrafters.skinstatues.skin.SkinService;
import com.cozycrafters.skinstatues.util.Text;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
    private final StatueUndoStore undoStore;
    private final Set<UUID> operations = ConcurrentHashMap.newKeySet();

    public StatueService(JavaPlugin plugin, SkinService skins, StatuePlacer placer, StatuesConfig config) {
        this(plugin, skins, placer, config, new StatueUndoStore());
    }

    StatueService(JavaPlugin plugin, SkinService skins, StatuePlacer placer,
                  StatuesConfig config, StatueUndoStore undoStore) {
        this.plugin = plugin;
        this.skins = skins;
        this.placer = placer;
        this.config = config;
        this.undoStore = undoStore;
        this.planner = new StatuePlanner(config.palette());
    }

    /** Starts a statue for {@code player}. Call on the server thread. */
    public void generate(Player player, String name, int scale) {
        UUID builder = player.getUniqueId();
        if (!beginOperation(builder)) {
            player.sendMessage(Text.error("Please wait for your current statue operation to finish."));
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
                    operations.remove(builder);
                    tell(player, Text.error(ex.getMessage()));
                });
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to plan a statue of '" + name + "'.", ex);
                sync(() -> {
                    operations.remove(builder);
                    tell(player, Text.error("The statue could not be built. Check the server console."));
                });
            }
        });
    }

    public int activeBuilds() {
        return operations.size();
    }

    /** Restores the player's most recently completed statue. */
    public void undo(Player player) {
        UUID playerId = player.getUniqueId();
        if (!beginOperation(playerId)) {
            player.sendMessage(Text.error("Please wait for your current statue operation to finish."));
            return;
        }

        StatueUndoStore.UndoRecord record = undoStore.target(playerId);
        if (record == null) {
            operations.remove(playerId);
            player.sendMessage(Text.error("You don't have a statue to undo."));
            return;
        }
        World world = Bukkit.getWorld(record.worldId());
        if (world == null) {
            operations.remove(playerId);
            player.sendMessage(Text.error("That statue's world is not currently available."));
            return;
        }

        player.sendMessage(Text.info("Undoing your last statue..."));
        StatueUndoStore.Restoration restoration = undoStore.restoration(world, record);
        try {
            placer.restore(world, restoration, config.blocksPerTick(), result -> {
                operations.remove(playerId);
                if (!result.complete()) {
                    tell(player, Text.error("The statue could not be undone; please try again."));
                    return;
                }
                undoStore.finishUndo(playerId, record);
                tell(player, Text.success("Your last statue has been undone."));
            });
        } catch (RuntimeException ex) {
            operations.remove(playerId);
            plugin.getLogger().log(Level.SEVERE, "Failed to start statue undo.", ex);
            tell(player, Text.error("The statue could not be undone; please try again."));
        }
    }

    /** Clears all in-memory operation, undo and ownership state on disable. */
    public void clear() {
        operations.clear();
        undoStore.clear();
    }

    public int undoTargets() {
        return undoStore.targetCount();
    }

    public int ownedBlocks() {
        return undoStore.ownershipCount();
    }

    boolean beginOperation(UUID playerId) {
        return operations.add(playerId);
    }

    void finishOperation(UUID playerId) {
        operations.remove(playerId);
    }

    void start(Player player, ResolvedSkin skin, StatuePlan plan, World world, UUID builder) {
        if (plan.blockCount() == 0) {
            operations.remove(builder);
            tell(player, Text.error("That skin is fully transparent; there is nothing to build."));
            return;
        }
        StatueUndoStore.Capture capture = undoStore.begin(world.getUID());
        try {
            placer.place(world, plan, capture, config.blocksPerTick(), result -> {
                if (!result.complete() || result.processed() < plan.blockCount()) {
                    rollbackFailedBuild(player, world, builder, capture);
                    return;
                }
                undoStore.complete(builder, capture);
                operations.remove(builder);
                tell(player, Text.success("Statue of &f" + skin.profile().name()
                        + "&a created &7(" + plan.blockCount() + " blocks, " + plan.heightBlocks() + " tall)."));
            });
        } catch (RuntimeException ex) {
            undoStore.abort(capture);
            operations.remove(builder);
            plugin.getLogger().log(Level.SEVERE, "Failed to start statue placement.", ex);
            tell(player, Text.error("The statue could not be built. Check the server console."));
        }
    }

    private void rollbackFailedBuild(Player player, World world, UUID builder,
                                     StatueUndoStore.Capture capture) {
        World loaded = Bukkit.getWorld(world.getUID());
        if (loaded == null || capture.changeCount() == 0) {
            undoStore.abort(capture);
            operations.remove(builder);
            tell(player, Text.error("The statue could not be finished; its world was unloaded."));
            return;
        }

        StatueUndoStore.Restoration rollback = undoStore.restoration(loaded, capture.snapshot());
        try {
            placer.restore(loaded, rollback, config.blocksPerTick(), result -> {
                undoStore.abort(capture);
                operations.remove(builder);
                if (result.complete()) {
                    tell(player, Text.error("The statue could not be finished and was rolled back."));
                } else {
                    tell(player, Text.error("The statue could not be finished; cleanup was interrupted."));
                }
            });
        } catch (RuntimeException ex) {
            undoStore.abort(capture);
            operations.remove(builder);
            plugin.getLogger().log(Level.SEVERE, "Failed to start partial statue rollback.", ex);
            tell(player, Text.error("The statue could not be finished; cleanup was interrupted."));
        }
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
