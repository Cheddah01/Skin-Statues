package com.cozycrafters.skinstatues.fabric.client;

import java.util.Optional;
import java.util.regex.Pattern;

/** Pure form validation and command construction for the optional client menu. */
public final class StatueMenuLogic {
    public static final String UNDO_COMMAND = "statue undo";

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern POSITIVE_INTEGER = Pattern.compile("[0-9]+");

    private StatueMenuLogic() {
    }

    public static FormState state(String playerInput, String scaleInput, boolean commandAvailable) {
        return new FormState(normalizePlayer(playerInput), normalizeScale(scaleInput), commandAvailable);
    }

    public static Optional<String> createCommand(String playerInput, String scaleInput) {
        FormState state = state(playerInput, scaleInput, true);
        if (!state.canCreate()) {
            return Optional.empty();
        }
        return Optional.of("statue %s %d".formatted(state.player(), Integer.parseInt(state.scale())));
    }

    public static String undoCommand() {
        return UNDO_COMMAND;
    }

    private static String normalizePlayer(String input) {
        return input == null ? "" : input.trim();
    }

    private static String normalizeScale(String input) {
        return input == null ? "" : input.trim();
    }

    private static Validation validatePlayer(String player) {
        if (player.isEmpty()) {
            return Validation.PLAYER_REQUIRED;
        }
        return PLAYER_NAME.matcher(player).matches() ? Validation.VALID : Validation.PLAYER_INVALID;
    }

    private static Validation validateScale(String scale) {
        if (!POSITIVE_INTEGER.matcher(scale).matches()) {
            return Validation.SCALE_INVALID;
        }
        try {
            return Integer.parseInt(scale) > 0 ? Validation.VALID : Validation.SCALE_INVALID;
        } catch (NumberFormatException ignored) {
            return Validation.SCALE_INVALID;
        }
    }

    public enum Validation {
        VALID,
        PLAYER_REQUIRED,
        PLAYER_INVALID,
        SCALE_INVALID
    }

    public record FormState(String player, String scale, boolean commandAvailable) {
        public Validation validation() {
            Validation playerValidation = validatePlayer(player);
            return playerValidation == Validation.VALID ? validateScale(scale) : playerValidation;
        }

        public boolean canCreate() {
            return commandAvailable && validation() == Validation.VALID;
        }

        public boolean canUndo() {
            return commandAvailable;
        }
    }
}
