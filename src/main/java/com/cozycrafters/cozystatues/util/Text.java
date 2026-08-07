package com.cozycrafters.cozystatues.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Chat formatting helpers. Every player-facing message goes through here. */
public final class Text {

    private static final String PREFIX = "&8[&6CozyStatues&8]&r ";

    private Text() {
    }

    /** A prefixed message, written with the usual '&' colour codes. */
    public static Component message(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(PREFIX + legacy);
    }

    public static Component info(String message) {
        return message("&e" + message);
    }

    public static Component success(String message) {
        return message("&a" + message);
    }

    public static Component error(String message) {
        return message("&c" + message);
    }
}
