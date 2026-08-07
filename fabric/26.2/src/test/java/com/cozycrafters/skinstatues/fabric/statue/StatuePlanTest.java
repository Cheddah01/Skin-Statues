package com.cozycrafters.skinstatues.fabric.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Block positions are packed into longs; the packing has to survive negatives. */
class StatuePlanTest {

    @Test
    void positionsRoundTripThroughThePacking() {
        int[][] cases = {
                {0, 0, 0},
                {1, 2, 3},
                {-1, -1, -1},
                {10, 64, 20},
                {-2048, -64, 2048},
                {29_999_999, 319, -29_999_999},
                {-29_999_999, -64, 29_999_999},
        };
        for (int[] position : cases) {
            long packed = StatuePlan.pack(position[0], position[1], position[2]);
            assertEquals(position[0], StatuePlan.unpackX(packed), "x");
            assertEquals(position[1], StatuePlan.unpackY(packed), "y");
            assertEquals(position[2], StatuePlan.unpackZ(packed), "z");
        }
    }

    @Test
    void thePackingCoversTheWholeVanillaBuildHeight() {
        for (int y = -2048; y <= 2047; y += 37) {
            assertEquals(y, StatuePlan.unpackY(StatuePlan.pack(0, y, 0)));
        }
    }
}
