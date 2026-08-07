package com.cozycrafters.skinstatues.fabric.statue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** UUID-only per-player operation lock. Used exclusively on the server thread. */
public final class OperationLock {
    private final Set<UUID> active = new HashSet<>();

    public boolean begin(UUID playerId) {
        return active.add(playerId);
    }

    public void finish(UUID playerId) {
        active.remove(playerId);
    }

    public boolean contains(UUID playerId) {
        return active.contains(playerId);
    }

    public int size() {
        return active.size();
    }

    public void clear() {
        active.clear();
    }
}
