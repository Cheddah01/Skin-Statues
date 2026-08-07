package com.cozycrafters.skinstatues.fabric.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class StatueUndoStoreTest {
    @Test
    void captureIsNotPublishedBeforeSuccessfulCompletion() {
        StatueUndoStore store = new StatueUndoStore();
        UUID player = UUID.randomUUID();
        StatueUndoStore.Capture capture = store.begin(Level.OVERWORLD);
        capture.add(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), null, Blocks.STONE.defaultBlockState());
        assertNull(store.target(player));
        store.abort(capture);
        assertNull(store.target(player));
        assertEquals(0, store.ownershipCount());
    }

    @Test
    void successfulCompletionPublishesExactlyOneTarget() {
        StatueUndoStore store = new StatueUndoStore();
        UUID player = UUID.randomUUID();
        StatueUndoStore.Capture capture = store.begin(Level.OVERWORLD);
        capture.add(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), null, Blocks.STONE.defaultBlockState());
        StatueUndoStore.UndoRecord record = store.complete(player, capture);
        assertSame(record, store.target(player));
        assertEquals(1, store.targetCount());
    }

    @Test
    void completedUndoDiscardsTheTarget() {
        StatueUndoStore store = new StatueUndoStore();
        UUID player = UUID.randomUUID();
        StatueUndoStore.Capture capture = store.begin(Level.OVERWORLD);
        StatueUndoStore.UndoRecord record = store.complete(player, capture);
        store.finishUndo(player, record);
        assertNull(store.target(player));
    }

    @Test
    void newerStatueOwnsAnOverlappingPositionAndRemembersThePreviousOwner() {
        StatueUndoStore store = new StatueUndoStore();
        BlockPos pos = new BlockPos(4, 70, -3);
        StatueUndoStore.Capture older = store.begin(Level.OVERWORLD);
        older.add(pos, Blocks.AIR.defaultBlockState(), null, Blocks.STONE.defaultBlockState());
        store.complete(UUID.randomUUID(), older);

        StatueUndoStore.Capture newer = store.begin(Level.OVERWORLD);
        newer.add(pos, Blocks.STONE.defaultBlockState(), null, Blocks.DIORITE.defaultBlockState());
        store.complete(UUID.randomUUID(), newer);

        assertEquals(newer.statueId(), store.owner(Level.OVERWORLD, pos));
        assertEquals(older.statueId(), newer.snapshot().changes().getFirst().previousOwner());
        assertFalse(StatueUndoStore.canRestore(store.owner(Level.OVERWORLD, pos), older.statueId(),
                Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()));
    }

    @Test
    void manualChangesFailTheConservativeRestorePredicate() {
        long statue = 4;
        assertTrue(StatueUndoStore.canRestore(statue, statue,
                Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()));
        assertFalse(StatueUndoStore.canRestore(statue, statue,
                Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState()));
        assertFalse(StatueUndoStore.canRestore(9, statue,
                Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()));
    }

    @Test
    void aNewTargetReplacesThePlayersOlderTarget() {
        StatueUndoStore store = new StatueUndoStore();
        UUID player = UUID.randomUUID();
        StatueUndoStore.Capture first = store.begin(Level.OVERWORLD);
        first.add(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), null, Blocks.STONE.defaultBlockState());
        store.complete(player, first);
        StatueUndoStore.Capture second = store.begin(Level.OVERWORLD);
        second.add(BlockPos.ZERO.above(), Blocks.AIR.defaultBlockState(), null, Blocks.DIRT.defaultBlockState());
        StatueUndoStore.UndoRecord latest = store.complete(player, second);
        assertSame(latest, store.target(player));
        assertEquals(1, store.targetCount());
        assertEquals(1, store.ownershipCount());
    }
}
