package com.cozycrafters.skinstatues.fabric.skin;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Resolves usernames to decoded skins, with caching.
 *
 * <p>{@link #resolve(String)} blocks on the network and must only be called
 * off the server thread. Successes and failures are both cached — failures only
 * briefly — so a player repeating the command, or mistyping a name, does not
 * hammer Mojang's services or fill the console with repeated warnings.
 */
public final class SkinService {

    /** Failures are remembered just long enough to absorb command spam. */
    public static final long FAILURE_CACHE_MILLIS = 60_000L;

    private static final int MAX_ENTRIES = 256;

    private final ProfileSource profiles;
    private final TextureDownloader downloader;
    private final long successCacheMillis;
    private final LongSupplier clock;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public SkinService(ProfileSource profiles, TextureDownloader downloader, long successCacheMillis) {
        this(profiles, downloader, successCacheMillis, System::currentTimeMillis);
    }

    /** Visible for tests: lets the cache be driven by a deterministic clock. */
    public SkinService(ProfileSource profiles, TextureDownloader downloader, long successCacheMillis,
                       LongSupplier clock) {
        this.profiles = profiles;
        this.downloader = downloader;
        this.successCacheMillis = Math.max(0L, successCacheMillis);
        this.clock = clock;
    }

    public ResolvedSkin resolve(String name) throws SkinLookupException {
        String key = name.toLowerCase(Locale.ROOT);
        long now = clock.getAsLong();

        Entry cached = cache.get(key);
        if (cached != null && cached.expiresAt > now) {
            if (cached.failure != null) {
                throw new SkinLookupException(cached.failure);
            }
            return cached.skin;
        }

        try {
            SkinProfile profile = profiles.lookup(name);
            byte[] png = downloader.download(profile.skinUrl());
            ResolvedSkin resolved = new ResolvedSkin(profile, SkinDecoder.decode(png, profile.model()));
            store(key, new Entry(resolved, null, now + successCacheMillis));
            return resolved;
        } catch (SkinLookupException ex) {
            store(key, new Entry(null, ex.getMessage(), now + FAILURE_CACHE_MILLIS));
            throw ex;
        }
    }

    public void clear() {
        cache.clear();
    }

    public int cachedCount() {
        return cache.size();
    }

    private void store(String key, Entry entry) {
        if (cache.size() >= MAX_ENTRIES) {
            purge(clock.getAsLong());
        }
        cache.put(key, entry);
    }

    /** Drops expired entries, and everything else if that was not enough. */
    private void purge(long now) {
        Iterator<Entry> it = cache.values().iterator();
        while (it.hasNext()) {
            if (it.next().expiresAt <= now) {
                it.remove();
            }
        }
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }
    }

    private record Entry(ResolvedSkin skin, String failure, long expiresAt) {
    }
}
