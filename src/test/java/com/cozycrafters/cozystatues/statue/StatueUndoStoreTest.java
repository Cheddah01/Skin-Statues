package com.cozycrafters.cozystatues.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

class StatueUndoStoreTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Test
    void newestCompletedStatueReplacesThePreviousUndoTarget() {
        StatueUndoStore store = new StatueUndoStore();
        StatueUndoStore.Capture first = store.begin(WORLD_ID);
        first.add(snapshot(1, Material.DIRT).state(), Material.STONE, data());
        StatueUndoStore.UndoRecord old = store.complete(PLAYER_A, first);

        StatueUndoStore.Capture second = store.begin(WORLD_ID);
        second.add(snapshot(2, Material.GRASS_BLOCK).state(), Material.BLACK_CONCRETE, data());
        StatueUndoStore.UndoRecord newest = store.complete(PLAYER_A, second);

        assertSame(newest, store.target(PLAYER_A));
        assertFalse(old.statueId() == newest.statueId());
        assertEquals(1, store.targetCount());
        assertEquals(1, store.ownershipCount(), "discarded undo ownership must be released");
    }

    @Test
    void originalNonAirSnapshotAndPlacedBlockDataAreRetained() {
        StatueUndoStore store = new StatueUndoStore();
        Snapshot snapshot = snapshot(3, Material.DIRT);
        BlockData placed = data();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        capture.add(snapshot.state(), Material.STONE, placed);

        StatueUndoStore.BlockChange change = capture.snapshot().changes().getFirst();
        assertSame(snapshot.state(), change.original());
        assertEquals(Material.DIRT, change.original().getType());
        assertEquals(Material.STONE, change.placedType());
        assertSame(placed, change.placedData());
    }

    @Test
    void restorationUsesTheBlockStateSnapshotAndExactPlacedData() {
        StatueUndoStore store = new StatueUndoStore();
        Snapshot original = snapshot(4, Material.DIRT);
        BlockData placed = data();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        capture.add(original.state(), Material.STONE, placed);
        StatueUndoStore.UndoRecord record = store.complete(PLAYER_A, capture);
        World world = worldWith(original.x(), Material.STONE, placed);

        StatueUndoStore.Restoration restoration = store.restoration(world, record);
        assertTrue(restoration.runBatch(10));

        verify(original.state()).update(true, false);
        assertEquals(1, restoration.restored());
        assertEquals(0, restoration.skipped());
    }

    @Test
    void restorationBatchingIsIndependentOfStatueScale() {
        for (int scale : new int[]{1, 2, 4}) {
            StatueUndoStore store = new StatueUndoStore();
            StatueUndoStore.Capture capture = store.begin(WORLD_ID);
            List<Snapshot> snapshots = new ArrayList<>();
            BlockData placed = data();
            for (int x = 0; x < scale * scale; x++) {
                Snapshot snapshot = snapshot(x, Material.DIRT);
                snapshots.add(snapshot);
                capture.add(snapshot.state(), Material.STONE, placed);
            }
            StatueUndoStore.UndoRecord record = store.complete(PLAYER_A, capture);
            World world = mock(World.class);
            when(world.getUID()).thenReturn(WORLD_ID);
            for (Snapshot snapshot : snapshots) {
                Block block = block(Material.STONE, placed);
                when(world.getBlockAt(snapshot.x(), 64, 0)).thenReturn(block);
            }

            StatueUndoStore.Restoration restoration = store.restoration(world, record);
            int ticks = 0;
            while (!restoration.runBatch(2)) {
                ticks++;
            }
            assertEquals(scale * scale, restoration.restored());
            assertEquals((scale * scale - 1) / 2, ticks);
        }
    }

    @Test
    void newerOverlappingStatueProtectsItsBlocksAndCanRevealTheOlderOwnerAgain() {
        StatueUndoStore store = new StatueUndoStore();
        BlockData firstData = data();
        Snapshot beforeFirst = snapshot(5, Material.DIRT);
        StatueUndoStore.Capture first = store.begin(WORLD_ID);
        first.add(beforeFirst.state(), Material.STONE, firstData);
        StatueUndoStore.UndoRecord firstRecord = store.complete(PLAYER_A, first);

        BlockData secondData = data();
        Snapshot beforeSecond = snapshot(5, Material.STONE);
        StatueUndoStore.Capture second = store.begin(WORLD_ID);
        second.add(beforeSecond.state(), Material.BLACK_CONCRETE, secondData);
        StatueUndoStore.UndoRecord secondRecord = store.complete(PLAYER_B, second);

        World world = worldWith(5, Material.BLACK_CONCRETE, secondData);
        StatueUndoStore.Restoration stale = store.restoration(world, firstRecord);
        assertTrue(stale.runBatch(10));
        assertEquals(1, stale.skipped());
        verify(beforeFirst.state(), never()).update(true, false);

        StatueUndoStore.Restoration newest = store.restoration(world, secondRecord);
        assertTrue(newest.runBatch(10));
        verify(beforeSecond.state()).update(true, false);
        store.finishUndo(PLAYER_B, secondRecord);

        Block restoredFirst = world.getBlockAt(5, 64, 0);
        when(restoredFirst.getType()).thenReturn(Material.STONE);
        when(restoredFirst.getBlockData()).thenReturn(firstData);
        StatueUndoStore.Restoration older = store.restoration(world, firstRecord);
        assertTrue(older.runBatch(10));
        verify(beforeFirst.state()).update(true, false);
    }

    @Test
    void manuallyChangedLocationsAreSkippedInsteadOfOverwritten() {
        StatueUndoStore store = new StatueUndoStore();
        Snapshot original = snapshot(6, Material.DIRT);
        BlockData placed = data();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        capture.add(original.state(), Material.STONE, placed);
        StatueUndoStore.UndoRecord record = store.complete(PLAYER_A, capture);
        World world = worldWith(6, Material.GOLD_BLOCK, data());

        StatueUndoStore.Restoration restoration = store.restoration(world, record);
        assertTrue(restoration.runBatch(10));

        assertEquals(0, restoration.restored());
        assertEquals(1, restoration.skipped());
        verify(original.state(), never()).update(true, false);
        assertEquals(0, store.ownershipCount());
    }

    @Test
    void changedBlockDataIsEnoughToProtectANewerWorldEdit() {
        StatueUndoStore store = new StatueUndoStore();
        Snapshot original = snapshot(10, Material.DIRT);
        BlockData placed = data();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        capture.add(original.state(), Material.STONE, placed);
        StatueUndoStore.UndoRecord record = store.complete(PLAYER_A, capture);
        World world = worldWith(10, Material.STONE, data());

        StatueUndoStore.Restoration restoration = store.restoration(world, record);
        assertTrue(restoration.runBatch(10));

        assertEquals(0, restoration.restored());
        assertEquals(1, restoration.skipped());
        verify(original.state(), never()).update(true, false);
    }

    @Test
    void successfulUndoClearsOnlyTheCompletedTarget() {
        StatueUndoStore store = new StatueUndoStore();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        Snapshot original = snapshot(7, Material.DIRT);
        BlockData placed = data();
        capture.add(original.state(), Material.STONE, placed);
        StatueUndoStore.UndoRecord record = store.complete(PLAYER_A, capture);
        StatueUndoStore.Restoration restoration = store.restoration(
                worldWith(7, Material.STONE, placed), record);
        assertTrue(restoration.runBatch(10));

        store.finishUndo(PLAYER_A, record);

        assertNull(store.target(PLAYER_A));
        assertEquals(0, store.targetCount());
        assertEquals(0, store.ownershipCount());
    }

    @Test
    void abortBeforeCompletionNeverCreatesAnUndoTarget() {
        StatueUndoStore store = new StatueUndoStore();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        capture.add(snapshot(8, Material.DIRT).state(), Material.STONE, data());

        store.abort(capture);

        assertNull(store.target(PLAYER_A));
        assertEquals(0, store.targetCount());
        assertEquals(0, store.ownershipCount());
    }

    @Test
    void disableStyleCleanupReleasesAllSnapshotsAndOwnership() {
        StatueUndoStore store = new StatueUndoStore();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        capture.add(snapshot(9, Material.DIRT).state(), Material.STONE, data());
        store.complete(PLAYER_A, capture);

        store.clear();

        assertEquals(0, store.targetCount());
        assertEquals(0, store.ownershipCount());
    }

    private static World worldWith(int x, Material current, BlockData data) {
        World world = mock(World.class);
        Block block = block(current, data);
        when(world.getUID()).thenReturn(WORLD_ID);
        when(world.getBlockAt(x, 64, 0)).thenReturn(block);
        return world;
    }

    private static Block block(Material material, BlockData data) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.getBlockData()).thenReturn(data);
        return block;
    }

    private static Snapshot snapshot(int x, Material material) {
        BlockState state = mock(BlockState.class);
        BlockData originalData = data();
        when(state.getX()).thenReturn(x);
        when(state.getY()).thenReturn(64);
        when(state.getZ()).thenReturn(0);
        when(state.getType()).thenReturn(material);
        when(state.getBlockData()).thenReturn(originalData);
        when(state.update(true, false)).thenReturn(true);
        return new Snapshot(x, state);
    }

    private static BlockData data() {
        BlockData data = mock(BlockData.class);
        when(data.clone()).thenReturn(data);
        return data;
    }

    private record Snapshot(int x, BlockState state) {
    }
}
