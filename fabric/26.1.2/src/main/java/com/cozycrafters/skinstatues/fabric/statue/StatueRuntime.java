package com.cozycrafters.skinstatues.fabric.statue;

import com.cozycrafters.skinstatues.fabric.config.StatuesConfig;
import com.cozycrafters.skinstatues.fabric.model.PlayerModel;
import com.cozycrafters.skinstatues.fabric.skin.FabricProfileSource;
import com.cozycrafters.skinstatues.fabric.skin.HttpTextureDownloader;
import com.cozycrafters.skinstatues.fabric.skin.ResolvedSkin;
import com.cozycrafters.skinstatues.fabric.skin.SkinLookupException;
import com.cozycrafters.skinstatues.fabric.skin.SkinService;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/** Owns asynchronous planning and server-thread, tick-batched world mutation. */
public final class StatueRuntime {
    private final MinecraftServer server;
    private final StatuesConfig config;
    private final Logger logger;
    private final SkinService skins;
    private final StatuePlanner planner;
    private final StatueUndoStore undoStore = new StatueUndoStore();
    private final OperationLock operations = new OperationLock();
    private final Queue<TickTask> tasks = new ArrayDeque<>();
    private final ExecutorService workers;
    private volatile boolean stopped;

    public StatueRuntime(MinecraftServer server, StatuesConfig config, Logger logger) {
        this.server = server;
        this.config = config;
        this.logger = logger;
        this.skins = new SkinService(new FabricProfileSource(server), new HttpTextureDownloader(), config.skinCacheMillis());
        this.planner = new StatuePlanner(config.palette());
        ThreadFactory threads = Thread.ofPlatform().name("SkinStatues-worker-", 0).daemon(true).factory();
        this.workers = Executors.newFixedThreadPool(2, threads);
    }

    /** Starts a generation request. Must be invoked on the server thread. */
    public void generate(ServerPlayer player, String requestedName, int scale) {
        UUID builder = player.getUUID();
        if (!operations.begin(builder)) {
            sendError(builder, "Please wait for your current statue operation to finish.");
            return;
        }

        ServerLevel level = player.level();
        ResourceKey<Level> dimension = level.dimension();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 1;
        player.sendSystemMessage(Component.literal("Building a statue of ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(requestedName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" at scale ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(Integer.toString(scale)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("...").withStyle(ChatFormatting.YELLOW)));

        workers.execute(() -> {
            try {
                ResolvedSkin skin = skins.resolve(requestedName);
                PlayerModel model = PlayerModel.of(skin.texture().model(), config.outerLayer());
                StatuePlanner.Dimensions dimensions = planner.dimensions(model, scale);
                StatuePlacement placement = PlacementCalculator.compute(x, y, z, yaw,
                        dimensions.width(), dimensions.depth());
                StatuePlan plan = planner.plan(skin.texture(), model, scale, placement, minY, maxY);
                execute(() -> startBuild(builder, dimension, skin, plan));
            } catch (SkinLookupException ex) {
                execute(() -> {
                    operations.finish(builder);
                    sendError(builder, ex.getMessage());
                });
            } catch (RuntimeException ex) {
                logger.error("Failed to plan a statue of '{}'.", requestedName, ex);
                execute(() -> {
                    operations.finish(builder);
                    sendError(builder, "The statue could not be built. Check the server console.");
                });
            }
        });
    }

    /** Starts restoration of the player's most recently completed statue. */
    public void undo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!operations.begin(playerId)) {
            sendError(playerId, "Please wait for your current statue operation to finish.");
            return;
        }
        StatueUndoStore.UndoRecord record = undoStore.target(playerId);
        if (record == null) {
            operations.finish(playerId);
            sendError(playerId, "You don't have a statue to undo.");
            return;
        }
        if (server.getLevel(record.dimension()) == null) {
            operations.finish(playerId);
            sendError(playerId, "That statue's world is not currently available.");
            return;
        }

        player.sendSystemMessage(Component.literal("Undoing your last statue...").withStyle(ChatFormatting.YELLOW));
        StatueUndoStore.Restoration restoration = undoStore.restoration(record);
        tasks.add(new RestoreTask(record.dimension(), restoration, "undo", complete -> {
            operations.finish(playerId);
            if (!complete) {
                sendError(playerId, "The statue could not be undone; please try again.");
                return;
            }
            undoStore.finishUndo(playerId, record);
            sendSuccess(playerId, "Your last statue has been undone.");
        }));
    }

    /** Advances every active build or undo by one configured batch. */
    public void tick() {
        int count = tasks.size();
        for (int i = 0; i < count; i++) {
            TickTask task = tasks.poll();
            if (task != null && !task.tick()) {
                tasks.add(task);
            }
        }
    }

    public void stop() {
        stopped = true;
        workers.shutdownNow();
        tasks.clear();
        operations.clear();
        undoStore.clear();
        skins.clear();
    }

    public int activeOperations() {
        return operations.size();
    }

    public int undoTargets() {
        return undoStore.targetCount();
    }

    private void startBuild(UUID builder, ResourceKey<Level> dimension, ResolvedSkin skin, StatuePlan plan) {
        if (plan.blockCount() == 0) {
            operations.finish(builder);
            sendError(builder, "That skin is fully transparent; there is nothing to build.");
            return;
        }
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            operations.finish(builder);
            sendError(builder, "The statue could not be built because its world was unloaded.");
            return;
        }
        StatueUndoStore.Capture capture = undoStore.begin(dimension);
        tasks.add(new BuildTask(dimension, plan, capture, () -> {
            undoStore.complete(builder, capture);
            operations.finish(builder);
            sendSuccess(builder, "Statue of " + skin.profile().name() + " created ("
                    + plan.blockCount() + " blocks, " + plan.heightBlocks() + " tall).");
        }, () -> rollbackFailedBuild(builder, capture)));
    }

    private void rollbackFailedBuild(UUID builder, StatueUndoStore.Capture capture) {
        if (capture.changeCount() == 0 || server.getLevel(capture.snapshot().dimension()) == null) {
            undoStore.abort(capture);
            operations.finish(builder);
            sendError(builder, "The statue could not be finished; its world was unloaded.");
            return;
        }
        StatueUndoStore.UndoRecord record = capture.snapshot();
        StatueUndoStore.Restoration restoration = undoStore.restoration(record);
        tasks.add(new RestoreTask(record.dimension(), restoration, "rollback", complete -> {
            undoStore.abort(capture);
            operations.finish(builder);
            sendError(builder, complete
                    ? "The statue could not be finished and was rolled back."
                    : "The statue could not be finished; cleanup was interrupted.");
        }));
    }

    private void execute(Runnable action) {
        if (!stopped) {
            server.execute(() -> {
                if (!stopped) {
                    action.run();
                }
            });
        }
    }

    private void sendError(UUID playerId, String message) {
        send(playerId, Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private void sendSuccess(UUID playerId, String message) {
        send(playerId, Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    private void send(UUID playerId, Component message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    private interface TickTask {
        boolean tick();
    }

    private final class BuildTask implements TickTask {
        private final ResourceKey<Level> dimension;
        private final StatuePlan plan;
        private final StatueUndoStore.Capture capture;
        private final Runnable success;
        private final Runnable failure;
        private int index;

        private BuildTask(ResourceKey<Level> dimension, StatuePlan plan, StatueUndoStore.Capture capture,
                          Runnable success, Runnable failure) {
            this.dimension = dimension;
            this.plan = plan;
            this.capture = capture;
            this.success = success;
            this.failure = failure;
        }

        @Override
        public boolean tick() {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                failure.run();
                return true;
            }
            try {
                int end = Math.min(plan.blockCount(), index + config.blocksPerTick());
                while (index < end) {
                    BlockPos pos = new BlockPos(plan.x(index), plan.y(index), plan.z(index));
                    BlockState placed = plan.material(index).defaultBlockState();
                    BlockState original = level.getBlockState(pos);
                    if (!original.equals(placed)) {
                        capture.add(level, pos, placed);
                        if (!level.setBlock(pos, placed, StatueUndoStore.UPDATE_FLAGS)) {
                            throw new IllegalStateException("Could not place block at " + pos);
                        }
                    }
                    index++;
                }
                if (index >= plan.blockCount()) {
                    success.run();
                    return true;
                }
                return false;
            } catch (RuntimeException ex) {
                logger.error("Statue placement failed after {} of {} planned blocks.", index, plan.blockCount(), ex);
                failure.run();
                return true;
            }
        }
    }

    private final class RestoreTask implements TickTask {
        private final ResourceKey<Level> dimension;
        private final StatueUndoStore.Restoration restoration;
        private final String context;
        private final java.util.function.Consumer<Boolean> finished;

        private RestoreTask(ResourceKey<Level> dimension, StatueUndoStore.Restoration restoration,
                            String context, java.util.function.Consumer<Boolean> finished) {
            this.dimension = dimension;
            this.restoration = restoration;
            this.context = context;
            this.finished = finished;
        }

        @Override
        public boolean tick() {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                finished.accept(false);
                return true;
            }
            try {
                if (restoration.runBatch(level, config.blocksPerTick())) {
                    finished.accept(true);
                    return true;
                }
                return false;
            } catch (RuntimeException ex) {
                logger.error("Statue {} failed after {} restored snapshots.", context, restoration.processed(), ex);
                finished.accept(false);
                return true;
            }
        }
    }
}
