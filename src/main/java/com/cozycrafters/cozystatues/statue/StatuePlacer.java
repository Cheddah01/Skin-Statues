package com.cozycrafters.cozystatues.statue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.World;
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

    /**
     * Starts building. Must be called on the server thread.
     *
     * @param onFinished receives the number of blocks actually placed, which is
     *                   short of the plan if the build was abandoned. Always
     *                   called exactly once unless the plugin is disabling.
     */
    public void place(World world, StatuePlan plan, int blocksPerTick, Consumer<Integer> onFinished) {
        Build build = new Build(world, plan, Math.max(1, blocksPerTick), onFinished);
        BukkitTask task = build.runTaskTimer(plugin, 1L, 1L);
        running.add(task);
        build.task = task;
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
        private final int blocksPerTick;
        private final Consumer<Integer> onFinished;
        private int index;
        private int placed;
        private BukkitTask task;

        private Build(World world, StatuePlan plan, int blocksPerTick, Consumer<Integer> onFinished) {
            this.world = world;
            this.plan = plan;
            this.blocksPerTick = blocksPerTick;
            this.onFinished = onFinished;
        }

        @Override
        public void run() {
            if (Bukkit.getWorld(world.getUID()) == null) {
                // The world was unloaded mid-build; there is nowhere left to build.
                finish();
                return;
            }

            int budget = Math.min(blocksPerTick, plan.blockCount() - index);
            for (int i = 0; i < budget; i++, index++) {
                world.getBlockAt(plan.x(index), plan.y(index), plan.z(index))
                        .setType(plan.material(index), false);
                placed++;
            }

            if (index >= plan.blockCount()) {
                finish();
            }
        }

        private void finish() {
            cancel();
            if (task != null) {
                running.remove(task);
            }
            onFinished.accept(placed);
        }
    }
}
