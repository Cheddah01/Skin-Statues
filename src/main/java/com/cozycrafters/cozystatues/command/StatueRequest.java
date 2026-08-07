package com.cozycrafters.cozystatues.command;

import java.util.regex.Pattern;

/**
 * A validated {@code /statue <name> <scale>} or {@code /statue undo} invocation.
 *
 * <p>Parsing is a pure function of the arguments and the configured maximum, so
 * the whole command surface can be tested without a server.
 */
public record StatueRequest(String playerName, int scale) {

    /** Mojang usernames: 1-16 characters of letters, digits and underscores. */
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    /** Whole numbers only: this rejects "2.5", "-1", "1e3" and "two" alike. */
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");

    public static final String USAGE = "Usage: /statue <name> <scale> or /statue undo";

    public sealed interface Result {

        record Ok(StatueRequest request) implements Result {
        }

        record Undo() implements Result {
        }

        record Error(String message) implements Result {
        }
    }

    public static Result parse(String[] args, int maxScale) {
        if (args.length == 1 && args[0].equalsIgnoreCase("undo")) {
            return new Result.Undo();
        }
        if (args.length != 2) {
            return new Result.Error(USAGE);
        }

        String name = args[0];
        if (!USERNAME.matcher(name).matches()) {
            return new Result.Error("'" + name + "' is not a valid Minecraft username.");
        }

        String rawScale = args[1];
        if (!WHOLE_NUMBER.matcher(rawScale).matches()) {
            return new Result.Error("Scale must be a whole number, for example 2.");
        }

        int scale = Integer.parseInt(rawScale);
        if (scale < 1) {
            return new Result.Error("Scale must be at least 1.");
        }
        if (scale > maxScale) {
            return new Result.Error("Scale must be between 1 and " + maxScale + ".");
        }
        return new Result.Ok(new StatueRequest(name, scale));
    }
}
