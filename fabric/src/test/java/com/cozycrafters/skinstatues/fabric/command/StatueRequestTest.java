package com.cozycrafters.skinstatues.fabric.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cozycrafters.skinstatues.fabric.command.StatueRequest.Result;
import org.junit.jupiter.api.Test;

class StatueRequestTest {

    private static final int MAX_SCALE = 4;

    private static Result parse(String... args) {
        return StatueRequest.parse(args, MAX_SCALE);
    }

    private static String error(String... args) {
        return assertInstanceOf(Result.Error.class, parse(args)).message();
    }

    private static StatueRequest ok(String... args) {
        return assertInstanceOf(Result.Ok.class, parse(args)).request();
    }

    @Test
    void aValidRequestIsAccepted() {
        StatueRequest request = ok("Notch", "2");
        assertEquals("Notch", request.playerName());
        assertEquals(2, request.scale());
    }

    @Test
    void undoIsTheSpecialOneArgumentRequest() {
        assertInstanceOf(Result.Undo.class, parse("undo"));
        assertInstanceOf(Result.Undo.class, parse("UNDO"));
        assertEquals("undo", ok("undo", "2").playerName(),
                "the existing two-argument player-name form remains valid");
    }

    @Test
    void theNameIsPassedThroughExactlyAsTyped() {
        assertEquals("Player_One", ok("Player_One", "1").playerName());
        assertEquals("aBcDeF123456789_", ok("aBcDeF123456789_", "1").playerName());
    }

    @Test
    void bothArgumentsAreRequired() {
        assertEquals(StatueRequest.USAGE, error());
        assertEquals(StatueRequest.USAGE, error("Notch"));
        assertEquals(StatueRequest.USAGE, error("Notch", "2", "extra"));
    }

    @Test
    void impossibleUsernamesAreRejectedBeforeAnyLookup() {
        assertTrue(error("", "1").contains("not a valid"));
        assertTrue(error("way_too_long_a_name", "1").contains("not a valid"));
        assertTrue(error("has space", "1").contains("not a valid"));
        assertTrue(error("Notch!", "1").contains("not a valid"));
        assertTrue(error("../etc", "1").contains("not a valid"));
    }

    @Test
    void decimalsAndTextAreNotWholeNumbers() {
        assertTrue(error("Notch", "2.5").contains("whole number"));
        assertTrue(error("Notch", "1.0").contains("whole number"));
        assertTrue(error("Notch", "two").contains("whole number"));
        assertTrue(error("Notch", "1e3").contains("whole number"));
        assertTrue(error("Notch", "").contains("whole number"));
        assertTrue(error("Notch", " 2").contains("whole number"));
    }

    @Test
    void negativeScalesAreNotWholeNumbersEither() {
        assertTrue(error("Notch", "-1").contains("whole number"));
        assertTrue(error("Notch", "-0").contains("whole number"));
    }

    @Test
    void zeroIsRejected() {
        assertEquals("Scale must be at least 1.", error("Notch", "0"));
        assertEquals("Scale must be at least 1.", error("Notch", "00"));
    }

    @Test
    void theConfiguredMaximumIsEnforced() {
        assertEquals(MAX_SCALE, ok("Notch", String.valueOf(MAX_SCALE)).scale());
        assertEquals("Scale must be between 1 and 4.", error("Notch", "5"));
        assertEquals("Scale must be between 1 and 4.", error("Notch", "999"));
    }

    @Test
    void absurdlyLargeScalesCannotOverflow() {
        // Anything past nine digits is refused outright rather than parsed.
        assertTrue(error("Notch", "2147483648").contains("whole number"));
        assertTrue(error("Notch", "99999999999").contains("whole number"));
        assertTrue(error("Notch", "999999999").contains("between 1 and"));
    }

    @Test
    void theMaximumIsWhateverTheServerConfigured() {
        assertInstanceOf(Result.Ok.class, StatueRequest.parse(new String[]{"Notch", "12"}, 16));
        assertInstanceOf(Result.Error.class, StatueRequest.parse(new String[]{"Notch", "2"}, 1));
    }
}
