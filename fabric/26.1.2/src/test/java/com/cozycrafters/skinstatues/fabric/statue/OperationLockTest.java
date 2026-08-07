package com.cozycrafters.skinstatues.fabric.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationLockTest {
    @Test
    void onePlayerCannotBeginTwoOperations() {
        OperationLock lock = new OperationLock();
        UUID player = UUID.randomUUID();
        assertTrue(lock.begin(player));
        assertFalse(lock.begin(player));
        assertEquals(1, lock.size());
    }

    @Test
    void differentPlayersCanOperateConcurrently() {
        OperationLock lock = new OperationLock();
        assertTrue(lock.begin(UUID.randomUUID()));
        assertTrue(lock.begin(UUID.randomUUID()));
        assertEquals(2, lock.size());
    }

    @Test
    void finishingAndClearingReleaseLocks() {
        OperationLock lock = new OperationLock();
        UUID player = UUID.randomUUID();
        lock.begin(player);
        lock.finish(player);
        assertTrue(lock.begin(player));
        lock.clear();
        assertFalse(lock.contains(player));
    }
}
