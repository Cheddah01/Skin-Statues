package com.cozycrafters.skinstatues.statue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Places a {@link StatuePlan} into the world a slice at a time.
 *
 * <p>All Bukkit access happens here, on the server thread, in fixed-size
 * batches so that even a maximum-scale statue never stalls a tick. Physics is
 * suppressed for every block: a statue is a static shell and should not trigger
 * updates as it grows.
 */
public final class StatuePlacer {

    private final Plugin plugin;
    private final Set<BukkitTask> running = new HashSet<>();

    public StatuePlacer(Plugin plugin) {
        this.plugin = plugin;
    }

    public record PlacementResult(int processed, boolean complete) {
    }

    public record RestorationResult(int processed, int restored, int skipped, boolean complete) {
    }

    /**
     * Starts building. Must be called on the server thread.
     *
     * @param onFinished receives completion status. Always called exactly once
     *                   unless the plugin is disabling.
     */
    public void place(World world, StatuePlan plan, StatueUndoStore.Capture capture,
                      int blocksPerTick, Consumer<PlacementResult> onFinished) {
        Build build = new Build(world, plan, capture, Math.max(1, blocksPerTick), onFinished);
        BukkitTask task = build.runTaskTimer(plugin, 1L, 1L);
        running.add(task);
        build.task = task;
    }

    /** Restores one undo record with the same per-tick budget as construction. */
    public void restore(World world, StatueUndoStore.Restoration restoration,
                        int blocksPerTick, Consumer<RestorationResult> onFinished) {
        Restore restore = new Restore(world, restoration, Math.max(1, blocksPerTick), onFinished);
        BukkitTask task = restore.runTaskTimer(plugin, 1L, 1L);
        running.add(task);
        restore.task = task;
    }

    /** Stops every in-flight build; used on plugin disable. */
    public void cancelAll() {
        for (BukkitTask task : Set.copyOf(running)) {
            task.cancel();
        }
        running.clear();
    }

    public int activeBuilds() {
        return running.size();
    }

    private final class Build extends BukkitRunnable {

        private final World world;
        private final StatuePlan plan;
        private final StatueUndoStore.Capture capture;
        private final int blocksPerTick;
        private final Consumer<PlacementResult> onFinished;
        private final Map<Material, BlockData> blockData = new EnumMap<>(Material.class);
        private int index;
        private BukkitTask task;

        private Build(World world, StatuePlan plan, StatueUndoStore.Capture capture,
                      int blocksPerTick, Consumer<PlacementResult> onFinished) {
            this.world = world;
            this.plan = plan;
            this.capture = capture;
            this.blocksPerTick = blocksPerTick;
            this.onFinished = onFinished;
        }

        @Override
        public void run() {
            if (Bukkit.getWorld(world.getUID()) == null) {
                // The world was unloaded mid-build; there is nowhere left to build.
                finish(false);
                return;
            }

            try {
                int budget = Math.min(blocksPerTick, plan.blockCount() - index);
                for (int i = 0; i < budget; i++, index++) {
                    Block block = world.getBlockAt(plan.x(index), plan.y(index), plan.z(index));
                    Material material = plan.material(index);
                    BlockData placedData = blockData.computeIfAbsent(material, Material::createBlockData);
                    BlockState original = block.getState();
                    if (original.getType() == material && original.getBlockData().equals(placedData)) {
                        continue;
                    }
                    capture.add(original, material, placedData);
                    block.setBlockData(placedData, false);
                }
            } catch (RuntimeException ex) {
                finish(false);
                return;
            }

            if (index >= plan.blockCount()) {
                finish(true);
            }
        }

        private void finish(boolean complete) {
            cancel();
            if (task != null) {
                running.remove(task);
            }
            onFinished.accept(new PlacementResult(index, complete));
        }
    }

    private final class Restore extends BukkitRunnable {

        private final World world;
        private final StatueUndoStore.Restoration restoration;
        private final int blocksPerTick;
        private final Consumer<RestorationResult> onFinished;
        private BukkitTask task;

        private Restore(World world, StatueUndoStore.Restoration restoration,
                        int blocksPerTick, Consumer<RestorationResult> onFinished) {
            this.world = world;
            this.restoration = restoration;
            this.blocksPerTick = blocksPerTick;
            this.onFinished = onFinished;
        }

        @Override
        public void run() {
            if (Bukkit.getWorld(world.getUID()) == null) {
                finish(false);
                return;
            }
            try {
                if (restoration.runBatch(blocksPerTick)) {
                    finish(true);
                }
            } catch (RuntimeException ex) {
                finish(false);
            }
        }

        private void finish(boolean complete) {
            cancel();
            if (task != null) {
                running.remove(task);
            }
            onFinished.accept(new RestorationResult(restoration.processed(), restoration.restored(),
                    restoration.skipped(), complete));
        }
    }
}
