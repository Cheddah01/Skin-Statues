package com.cozycrafters.skinstatues.statue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

/**
 * One in-memory undo target per player plus internal ownership for overlap
 * protection. No Bukkit player object is retained by the registry.
 */
public final class StatueUndoStore {

    private final Map<UUID, UndoRecord> targets = new HashMap<>();
    private final Map<BlockKey, Long> owners = new HashMap<>();
    private final Set<Long> activeStatues = new HashSet<>();
    private long nextId = 1;

    /** A single changed block and the complete state that preceded it. */
    public record BlockChange(
            int x,
            int y,
            int z,
            BlockState original,
            Material placedType,
            BlockData placedData,
            long previousOwner
    ) {
    }

    /** The completed changes belonging to one internally identified statue. */
    public record UndoRecord(long statueId, UUID worldId, List<BlockChange> changes) {

        public UndoRecord {
            changes = List.copyOf(changes);
        }
    }

    /** Mutable capture used only while one statue is being placed. */
    public final class Capture {

        private final long statueId;
        private final UUID worldId;
        private final List<BlockChange> changes = new ArrayList<>();

        private Capture(long statueId, UUID worldId) {
            this.statueId = statueId;
            this.worldId = worldId;
        }

        public long statueId() {
            return statueId;
        }

        public int changeCount() {
            return changes.size();
        }

        /** Claims the position and retains its pre-placement snapshot. */
        public void add(BlockState original, Material placedType, BlockData placedData) {
            BlockKey key = new BlockKey(worldId, original.getX(), original.getY(), original.getZ());
            long previous = claim(key, statueId);
            changes.add(new BlockChange(original.getX(), original.getY(), original.getZ(),
                    original, placedType, placedData.clone(), previous));
        }

        public UndoRecord snapshot() {
            return new UndoRecord(statueId, worldId, changes);
        }
    }

    /** Stateful, synchronous restoration work consumed one batch at a time. */
    public final class Restoration {

        private final World world;
        private final UndoRecord record;
        private int index;
        private int restored;
        private int skipped;

        private Restoration(World world, UndoRecord record) {
            if (!world.getUID().equals(record.worldId())) {
                throw new IllegalArgumentException("Undo record belongs to another world.");
            }
            this.world = world;
            this.record = record;
        }

        /** Processes at most {@code limit} snapshots and returns true when done. */
        public boolean runBatch(int limit) {
            int end = Math.min(record.changes().size(), index + Math.max(1, limit));
            while (index < end) {
                restore(record.changes().get(index++));
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

        private void restore(BlockChange change) {
            BlockKey key = new BlockKey(record.worldId(), change.x(), change.y(), change.z());
            if (!isOwnedBy(key, record.statueId())) {
                skipped++;
                return;
            }

            Block block = world.getBlockAt(change.x(), change.y(), change.z());
            if (block.getType() != change.placedType()
                    || !block.getBlockData().equals(change.placedData())) {
                // A player or another system changed this block after the
                // statue. It no longer belongs to this undo operation.
                forget(key, record.statueId());
                skipped++;
                return;
            }

            if (change.original().update(true, false)) {
                restored++;
            } else {
                skipped++;
            }
            restorePreviousOwner(key, record.statueId(), change.previousOwner());
        }
    }

    public synchronized Capture begin(UUID worldId) {
        long id = nextId++;
        activeStatues.add(id);
        return new Capture(id, worldId);
    }

    /** Publishes a fully completed statue and forgets this player's older one. */
    public synchronized UndoRecord complete(UUID playerId, Capture capture) {
        UndoRecord record = capture.snapshot();
        UndoRecord previous = targets.put(playerId, record);
        if (previous != null) {
            activeStatues.remove(previous.statueId());
            forgetOwnedPositions(previous);
        }
        return record;
    }

    /** Drops an incomplete statue capture without exposing an undo target. */
    public synchronized void abort(Capture capture) {
        UndoRecord record = capture.snapshot();
        activeStatues.remove(record.statueId());
        forgetOwnedPositions(record);
    }

    public synchronized UndoRecord target(UUID playerId) {
        return targets.get(playerId);
    }

    public Restoration restoration(World world, UndoRecord record) {
        return new Restoration(world, record);
    }

    /** Clears the target only if it is still the record that was restored. */
    public synchronized void finishUndo(UUID playerId, UndoRecord record) {
        if (targets.remove(playerId, record)) {
            activeStatues.remove(record.statueId());
            forgetOwnedPositions(record);
        }
    }

    public synchronized int targetCount() {
        return targets.size();
    }

    public synchronized int ownershipCount() {
        return owners.size();
    }

    public synchronized void clear() {
        targets.clear();
        owners.clear();
        activeStatues.clear();
    }

    private synchronized long claim(BlockKey key, long statueId) {
        Long previous = owners.put(key, statueId);
        return previous == null ? 0 : previous;
    }

    private synchronized boolean isOwnedBy(BlockKey key, long statueId) {
        return owners.getOrDefault(key, 0L) == statueId;
    }

    private synchronized void forget(BlockKey key, long statueId) {
        owners.remove(key, statueId);
    }

    private synchronized void restorePreviousOwner(BlockKey key, long statueId, long previousOwner) {
        if (!isOwnedBy(key, statueId)) {
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
            owners.remove(new BlockKey(record.worldId(), change.x(), change.y(), change.z()),
                    record.statueId());
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
    }
}
