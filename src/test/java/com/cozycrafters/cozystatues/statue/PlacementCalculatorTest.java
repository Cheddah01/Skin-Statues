package com.cozycrafters.cozystatues.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Where the statue lands, and which way round. The properties that matter are
 * that it always looks back at its builder, never overlaps them, and behaves
 * the same whichever way they happen to be facing.
 */
class PlacementCalculatorTest {

    private static final int WIDTH = 16;
    private static final int DEPTH = 8;

    @Test
    void yawSnapsToTheFourCardinalDirections() {
        assertEquals(Cardinal.SOUTH, Cardinal.fromYaw(0.0f));
        assertEquals(Cardinal.WEST, Cardinal.fromYaw(90.0f));
        assertEquals(Cardinal.NORTH, Cardinal.fromYaw(180.0f));
        assertEquals(Cardinal.EAST, Cardinal.fromYaw(270.0f));
    }

    @Test
    void yawSnapsToTheNearestQuarterTurnAndSurvivesWrapping() {
        assertEquals(Cardinal.SOUTH, Cardinal.fromYaw(44.0f));
        assertEquals(Cardinal.WEST, Cardinal.fromYaw(46.0f));
        assertEquals(Cardinal.SOUTH, Cardinal.fromYaw(360.0f));
        assertEquals(Cardinal.SOUTH, Cardinal.fromYaw(720.0f));
        assertEquals(Cardinal.EAST, Cardinal.fromYaw(-90.0f));
        assertEquals(Cardinal.NORTH, Cardinal.fromYaw(-180.0f));
        assertEquals(Cardinal.WEST, Cardinal.fromYaw(-630.0f));
    }

    @Test
    void aBodysLeftIsNinetyDegreesFromItsFacing() {
        assertEquals(Cardinal.EAST, Cardinal.SOUTH.left());
        assertEquals(Cardinal.WEST, Cardinal.NORTH.left());
        assertEquals(Cardinal.NORTH, Cardinal.EAST.left());
        assertEquals(Cardinal.SOUTH, Cardinal.WEST.left());
    }

    @Test
    void theStatueAlwaysLooksBackAtItsBuilder() {
        for (float yaw = 0.0f; yaw < 360.0f; yaw += 15.0f) {
            StatuePlacement placement = PlacementCalculator.compute(10.5, 64.0, 20.5, yaw, WIDTH, DEPTH);
            assertEquals(Cardinal.fromYaw(yaw).opposite(), placement.facing(),
                    "statue should face the builder at yaw " + yaw);
            assertEquals(placement.facing().left(), placement.left());
        }
    }

    @Test
    void theStatueStartsOneBlockInFrontOfTheBuilder() {
        // Facing south: the front layer is the next block along +Z.
        assertEquals(21, PlacementCalculator.compute(10.5, 64.0, 20.5, 0.0f, WIDTH, DEPTH).anchorZ());
        // Facing north: the next block along -Z.
        assertEquals(19, PlacementCalculator.compute(10.5, 64.0, 20.5, 180.0f, WIDTH, DEPTH).anchorZ());
        // Facing east: +X, facing west: -X.
        assertEquals(11, PlacementCalculator.compute(10.5, 64.0, 20.5, 270.0f, WIDTH, DEPTH).anchorX());
        assertEquals(9, PlacementCalculator.compute(10.5, 64.0, 20.5, 90.0f, WIDTH, DEPTH).anchorX());
    }

    @Test
    void aBuilderStandingOnABlockBoundaryIsStillNeverInsideTheStatue() {
        // Their hitbox already reaches into the next block, so the statue steps
        // one further out rather than materialising around them.
        assertEquals(22, PlacementCalculator.compute(10.5, 64.0, 20.95, 0.0f, WIDTH, DEPTH).anchorZ());
        assertEquals(18, PlacementCalculator.compute(10.5, 64.0, 20.05, 180.0f, WIDTH, DEPTH).anchorZ());
    }

    @Test
    void noStatueBlockEverOverlapsTheBuildersHitbox() {
        double half = PlacementCalculator.PLAYER_HALF_WIDTH;
        for (float yaw : new float[]{0.0f, 90.0f, 180.0f, 270.0f}) {
            for (double offset : new double[]{0.05, 0.3, 0.5, 0.7, 0.99}) {
                double px = 10.0 + offset;
                double pz = 20.0 + offset;
                StatuePlacement placement = PlacementCalculator.compute(px, 64.0, pz, yaw, WIDTH, DEPTH);
                for (int bx = 0; bx < WIDTH; bx++) {
                    for (int bz = 0; bz < DEPTH; bz++) {
                        int x = placement.worldX(bx, bz);
                        int z = placement.worldZ(bx, bz);
                        boolean clearOnX = x + 1 <= px - half || x >= px + half;
                        boolean clearOnZ = z + 1 <= pz - half || z >= pz + half;
                        assertTrue(clearOnX || clearOnZ,
                                "block " + x + "," + z + " overlaps the builder at " + px + "," + pz);
                    }
                }
            }
        }
    }

    @Test
    void theStatueGrowsAwayFromTheBuilderNotTowardsThem() {
        StatuePlacement placement = PlacementCalculator.compute(10.5, 64.0, 20.5, 0.0f, WIDTH, DEPTH);
        // Builder faces south, so deeper slices of the statue are further south.
        assertEquals(21, placement.worldZ(0, 0));
        assertEquals(28, placement.worldZ(0, DEPTH - 1));
    }

    @Test
    void theStatueStraddlesTheBuildersOwnColumn() {
        for (float yaw : new float[]{0.0f, 90.0f, 180.0f, 270.0f}) {
            StatuePlacement placement = PlacementCalculator.compute(10.5, 64.0, 20.5, yaw, WIDTH, DEPTH);
            int minLateral = Integer.MAX_VALUE;
            int maxLateral = Integer.MIN_VALUE;
            boolean sideways = placement.left().x() != 0;
            for (int bx = 0; bx < WIDTH; bx++) {
                int lateral = sideways ? placement.worldX(bx, 0) : placement.worldZ(bx, 0);
                minLateral = Math.min(minLateral, lateral);
                maxLateral = Math.max(maxLateral, lateral);
            }
            int builder = sideways ? 10 : 20;
            assertTrue(minLateral <= builder && builder <= maxLateral,
                    "statue is not centred on the builder at yaw " + yaw);
            int centreOffset = Math.abs((minLateral + maxLateral) - 2 * builder);
            assertTrue(centreOffset <= 1, "statue is off centre by more than half a block at yaw " + yaw);
        }
    }

    @Test
    void theStatueStandsOnTheBlockLevelTheBuildersFeetAreOn() {
        assertEquals(64, PlacementCalculator.compute(10.5, 64.0, 20.5, 0.0f, WIDTH, DEPTH).anchorY());
        assertEquals(64, PlacementCalculator.compute(10.5, 64.9, 20.5, 0.0f, WIDTH, DEPTH).anchorY());
        assertEquals(-60, PlacementCalculator.compute(10.5, -60.0, 20.5, 0.0f, WIDTH, DEPTH).anchorY());
    }

    @Test
    void statueLocalCoordinatesGrowUpwards() {
        StatuePlacement placement = PlacementCalculator.compute(10.5, 64.0, 20.5, 0.0f, WIDTH, DEPTH);
        assertEquals(64, placement.worldY(0));
        assertEquals(96, placement.worldY(32));
    }

    @Test
    void placementIsRotationallyConsistent() {
        // The same build, seen from all four directions, must have the same
        // shape: only the axes swap.
        StatuePlacement south = PlacementCalculator.compute(10.5, 64.0, 20.5, 0.0f, WIDTH, DEPTH);
        StatuePlacement east = PlacementCalculator.compute(10.5, 64.0, 20.5, 270.0f, WIDTH, DEPTH);
        assertEquals(south.worldZ(0, 0) - 20, east.worldX(0, 0) - 10);
        assertEquals(south.worldX(0, 0) - 10, -(east.worldZ(0, 0) - 20));
    }

    @Test
    void aStatueNeedsPositiveDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> PlacementCalculator.compute(0, 0, 0, 0.0f, 0, DEPTH));
        assertThrows(IllegalArgumentException.class,
                () -> PlacementCalculator.compute(0, 0, 0, 0.0f, WIDTH, 0));
    }
}
