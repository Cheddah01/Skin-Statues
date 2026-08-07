package com.cozycrafters.cozystatues.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cozycrafters.cozystatues.StatuesConfig;
import com.cozycrafters.cozystatues.palette.BlockPalette;
import com.cozycrafters.cozystatues.model.SkinModel;
import com.cozycrafters.cozystatues.skin.ResolvedSkin;
import com.cozycrafters.cozystatues.skin.SkinService;
import com.cozycrafters.cozystatues.skin.SkinProfile;
import com.cozycrafters.cozystatues.util.Text;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class StatueServiceUndoTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");

    @Test
    void noAvailableUndoReportsCleanlyAndReleasesTheOperationLock() {
        StatueUndoStore store = new StatueUndoStore();
        StatueService service = service(store);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);

        service.undo(player);

        verify(player).sendMessage(Text.error("You don't have a statue to undo."));
        assertEquals(0, service.activeBuilds());
        assertEquals(0, service.undoTargets());
    }

    @Test
    void onePlayerCannotStartConflictingOperations() {
        StatueService service = service(new StatueUndoStore());

        assertTrue(service.beginOperation(PLAYER_ID));
        assertFalse(service.beginOperation(PLAYER_ID));
        assertEquals(1, service.activeBuilds());
        service.finishOperation(PLAYER_ID);
        assertTrue(service.beginOperation(PLAYER_ID));
    }

    @Test
    void undoWhileAnotherOperationIsActiveAsksThePlayerToWait() {
        StatueService service = service(new StatueUndoStore());
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        assertTrue(service.beginOperation(PLAYER_ID));

        service.undo(player);

        verify(player).sendMessage(Text.error("Please wait for your current statue operation to finish."));
        assertEquals(1, service.activeBuilds());
    }

    @Test
    void serviceCleanupUsedOnPluginDisableClearsUndoAndOwnershipState() {
        StatueUndoStore store = new StatueUndoStore();
        StatueUndoStore.Capture capture = store.begin(WORLD_ID);
        BlockState original = mock(BlockState.class);
        BlockData placed = mock(BlockData.class);
        when(placed.clone()).thenReturn(placed);
        when(original.getX()).thenReturn(1);
        when(original.getY()).thenReturn(64);
        when(original.getZ()).thenReturn(1);
        capture.add(original, Material.STONE, placed);
        store.complete(PLAYER_ID, capture);
        StatueService service = service(store);
        assertEquals(1, service.undoTargets());
        assertEquals(1, service.ownedBlocks());

        service.clear();

        assertEquals(0, service.undoTargets());
        assertEquals(0, service.ownedBlocks());
        assertEquals(0, service.activeBuilds());
    }

    @Test
    @SuppressWarnings("unchecked")
    void placementFailureBeforeConstructionDoesNotPublishAnUndoTarget() {
        StatueUndoStore store = new StatueUndoStore();
        JavaPlugin plugin = mock(JavaPlugin.class);
        StatuePlacer placer = mock(StatuePlacer.class);
        StatuesConfig config = config();
        StatueService service = new StatueService(plugin, mock(SkinService.class), placer, config, store);
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        when(player.isOnline()).thenReturn(true);
        when(world.getUID()).thenReturn(WORLD_ID);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        doThrow(new IllegalStateException("scheduler unavailable")).when(placer).place(
                org.mockito.ArgumentMatchers.eq(world), org.mockito.ArgumentMatchers.any(StatuePlan.class),
                org.mockito.ArgumentMatchers.any(StatueUndoStore.Capture.class),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(Consumer.class));
        StatuePlan plan = new StatuePlan(new long[]{StatuePlan.pack(0, 64, 0)},
                new Material[]{Material.STONE}, 1, 1, 1, 1);
        SkinProfile profile = new SkinProfile(UUID.randomUUID(), "Notch",
                URI.create("https://textures.minecraft.net/test"), SkinModel.CLASSIC);
        ResolvedSkin skin = new ResolvedSkin(profile, null);
        assertTrue(service.beginOperation(PLAYER_ID));

        service.start(player, skin, plan, world, PLAYER_ID);

        assertEquals(0, service.undoTargets());
        assertEquals(0, service.ownedBlocks());
        assertEquals(0, service.activeBuilds());
    }

    private static StatueService service(StatueUndoStore store) {
        return new StatueService(mock(JavaPlugin.class), mock(SkinService.class),
                mock(StatuePlacer.class), config(), store);
    }

    private static StatuesConfig config() {
        BlockPalette palette = BlockPalette.of(Map.of(Material.STONE, 0x777777));
        return new StatuesConfig(4, 100, true, 60_000, palette);
    }
}
