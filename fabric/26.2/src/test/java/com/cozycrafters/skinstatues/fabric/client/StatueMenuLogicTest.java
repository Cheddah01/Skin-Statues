package com.cozycrafters.skinstatues.fabric.client;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatueMenuLogicTest {
    @Test
    void createCommandTrimsThePlayerName() {
        assertEquals(Optional.of("statue Steve 2"), StatueMenuLogic.createCommand("  Steve  ", "2"));
    }

    @Test
    void createCommandTrimsTheScale() {
        assertEquals(Optional.of("statue Alex 4"), StatueMenuLogic.createCommand("Alex", " 4 "));
    }

    @Test
    void undoUsesTheExistingServerCommand() {
        assertEquals("statue undo", StatueMenuLogic.undoCommand());
    }

    @Test
    void blankPlayerNameIsRejected() {
        assertEquals(StatueMenuLogic.Validation.PLAYER_REQUIRED,
                StatueMenuLogic.state("   ", "1", true).validation());
        assertEquals(Optional.empty(), StatueMenuLogic.createCommand("   ", "1"));
    }

    @Test
    void embeddedSpacesAreRejected() {
        assertEquals(StatueMenuLogic.Validation.PLAYER_INVALID,
                StatueMenuLogic.state("Not Steve", "1", true).validation());
    }

    @Test
    void commandCharactersAndNewlinesAreRejected() {
        assertEquals(Optional.empty(), StatueMenuLogic.createCommand("Steve/undo", "1"));
        assertEquals(Optional.empty(), StatueMenuLogic.createCommand("Steve\nundo", "1"));
    }

    @Test
    void positiveWholeNumberScaleIsValid() {
        assertTrue(StatueMenuLogic.state("Steve", "1", true).canCreate());
        assertTrue(StatueMenuLogic.state("Steve", "2", true).canCreate());
        assertTrue(StatueMenuLogic.state("Steve", "4", true).canCreate());
    }

    @Test
    void zeroScaleIsRejected() {
        assertEquals(StatueMenuLogic.Validation.SCALE_INVALID,
                StatueMenuLogic.state("Steve", "0", true).validation());
    }

    @Test
    void negativeScaleIsRejected() {
        assertEquals(StatueMenuLogic.Validation.SCALE_INVALID,
                StatueMenuLogic.state("Steve", "-1", true).validation());
    }

    @Test
    void decimalScaleIsRejected() {
        assertEquals(StatueMenuLogic.Validation.SCALE_INVALID,
                StatueMenuLogic.state("Steve", "1.5", true).validation());
    }

    @Test
    void textScaleIsRejected() {
        assertEquals(StatueMenuLogic.Validation.SCALE_INVALID,
                StatueMenuLogic.state("Steve", "large", true).validation());
    }

    @Test
    void overflowingScaleIsRejectedLocally() {
        assertEquals(StatueMenuLogic.Validation.SCALE_INVALID,
                StatueMenuLogic.state("Steve", "9999999999", true).validation());
    }

    @Test
    void unavailableCommandDisablesCreateEvenForAValidForm() {
        StatueMenuLogic.FormState state = StatueMenuLogic.state("Steve", "2", false);
        assertEquals(StatueMenuLogic.Validation.VALID, state.validation());
        assertFalse(state.canCreate());
        assertFalse(state.canUndo());
    }

    @Test
    void availableCommandAndValidFormEnableCreate() {
        StatueMenuLogic.FormState state = StatueMenuLogic.state("Steve", "2", true);
        assertTrue(state.canCreate());
        assertTrue(state.canUndo());
    }
}
