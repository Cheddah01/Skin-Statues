package com.cozycrafters.skinstatues.fabric.statue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** One in-memory undo target per player, with ownership-aware overlap protection. */
public final class StatueUndoStore {
    public static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final Map<UUID, UndoRecord> targets = new HashMap<>();
    private final Map<BlockKey, Long> owners = new HashMap<>();
    private final Set<Long> activeStatues = new HashSet<>();
    private long nextId = 1;

    public record BlockChange(
            BlockPos pos,
            BlockState original,
            @Nullable CompoundTag blockEntityTag,
            BlockState placed,
            long previousOwner
    ) {
        public BlockChange {
            pos = pos.immutable();
            blockEntityTag = blockEntityTag == null ? null : blockEntityTag.copy();
        }
    }

    public record UndoRecord(long statueId, ResourceKey<Level> dimension, List<BlockChange> changes) {
        public UndoRecord {
            changes = List.copyOf(changes);
        }
    }

    public final class Capture {
        private final long statueId;
        private final ResourceKey<Level> dimension;
        private final List<BlockChange> changes = new ArrayList<>();

        private Capture(long statueId, ResourceKey<Level> dimension) {
            this.statueId = statueId;
            this.dimension = dimension;
        }

        public long statueId() {
            return statueId;
        }

        public int changeCount() {
            return changes.size();
        }

        public void add(ServerLevel level, BlockPos pos, BlockState placed) {
            BlockState original = level.getBlockState(pos);
            BlockEntity entity = level.getBlockEntity(pos);
            CompoundTag tag = entity == null ? null : entity.saveWithFullMetadata(level.registryAccess());
            add(pos, original, tag, placed);
        }

        public void add(BlockPos pos, BlockState original, @Nullable CompoundTag tag, BlockState placed) {
            BlockKey key = new BlockKey(dimension, pos);
            long previous = claim(key, statueId);
            changes.add(new BlockChange(pos, original, tag, placed, previous));
        }

        public UndoRecord snapshot() {
            return new UndoRecord(statueId, dimension, changes);
        }
    }

    public final class Restoration {
        private final UndoRecord record;
        private int index;
        private int restored;
        private int skipped;

        private Restoration(UndoRecord record) {
            this.record = record;
        }

        public boolean runBatch(ServerLevel level, int limit) {
            if (!level.dimension().equals(record.dimension())) {
                throw new IllegalArgumentException("Undo record belongs to another dimension.");
            }
            int end = Math.min(record.changes().size(), index + Math.max(1, limit));
            while (index < end) {
                restore(level, record.changes().get(index));
                index++;
            }
            return index >= record.changes().size();
        }

        public int processed() {
            return index;
        }

        public int restored() {
            return restored;
        }

        public int skipped() {
            return skipped;
        }

        private void restore(ServerLevel level, BlockChange change) {
            BlockKey key = new BlockKey(record.dimension(), change.pos());
            BlockState current = level.getBlockState(change.pos());
            if (!canRestore(owner(key), record.statueId(), current, change.placed())) {
                if (owner(key) == record.statueId() && !current.equals(change.placed())) {
                    forget(key, record.statueId());
                }
                skipped++;
                return;
            }

            BlockEntity restoredEntity = null;
            if (change.blockEntityTag() != null) {
                restoredEntity = BlockEntity.loadStatic(change.pos(), change.original(),
                        change.blockEntityTag(), level.registryAccess());
                if (restoredEntity == null) {
                    throw new IllegalStateException("Could not restore block entity at " + change.pos());
                }
            }
            if (!level.setBlock(change.pos(), change.original(), UPDATE_FLAGS)) {
                throw new IllegalStateException("Could not restore block at " + change.pos());
            }
            if (restoredEntity != null) {
                level.setBlockEntity(restoredEntity);
                restoredEntity.setChanged();
                level.sendBlockUpdated(change.pos(), change.original(), change.original(), Block.UPDATE_CLIENTS);
            }
            restored++;
            restorePreviousOwner(key, record.statueId(), change.previousOwner());
        }
    }

    public Capture begin(ResourceKey<Level> dimension) {
        long id = nextId++;
        activeStatues.add(id);
        return new Capture(id, dimension);
    }

    public UndoRecord complete(UUID playerId, Capture capture) {
        UndoRecord record = capture.snapshot();
        UndoRecord previous = targets.put(playerId, record);
        if (previous != null) {
            activeStatues.remove(previous.statueId());
            forgetOwnedPositions(previous);
        }
        return record;
    }

    public void abort(Capture capture) {
        UndoRecord record = capture.snapshot();
        activeStatues.remove(record.statueId());
        forgetOwnedPositions(record);
    }

    public @Nullable UndoRecord target(UUID playerId) {
        return targets.get(playerId);
    }

    public Restoration restoration(UndoRecord record) {
        return new Restoration(record);
    }

    public void finishUndo(UUID playerId, UndoRecord record) {
        if (targets.remove(playerId, record)) {
            activeStatues.remove(record.statueId());
            forgetOwnedPositions(record);
        }
    }

    public int targetCount() {
        return targets.size();
    }

    public int ownershipCount() {
        return owners.size();
    }

    public void clear() {
        targets.clear();
        owners.clear();
        activeStatues.clear();
    }

    static boolean canRestore(long owner, long statueId, BlockState current, BlockState placed) {
        return owner == statueId && current.equals(placed);
    }

    long owner(ResourceKey<Level> dimension, BlockPos pos) {
        return owner(new BlockKey(dimension, pos));
    }

    private long claim(BlockKey key, long statueId) {
        Long previous = owners.put(key, statueId);
        return previous == null ? 0 : previous;
    }

    private long owner(BlockKey key) {
        return owners.getOrDefault(key, 0L);
    }

    private void forget(BlockKey key, long statueId) {
        owners.remove(key, statueId);
    }

    private void restorePreviousOwner(BlockKey key, long statueId, long previousOwner) {
        if (owner(key) != statueId) {
            return;
        }
        if (previousOwner != 0 && activeStatues.contains(previousOwner)) {
            owners.put(key, previousOwner);
        } else {
            owners.remove(key);
        }
    }

    private void forgetOwnedPositions(UndoRecord record) {
        for (BlockChange change : record.changes()) {
            owners.remove(new BlockKey(record.dimension(), change.pos()), record.statueId());
        }
    }

    private record BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
        private BlockKey {
            pos = pos.immutable();
        }
    }
}
