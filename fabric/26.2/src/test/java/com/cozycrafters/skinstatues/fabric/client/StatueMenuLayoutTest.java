package com.cozycrafters.skinstatues.fabric.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatueMenuLayoutTest {
    @Test
    void largeScreenUsesPreferredWidthAndContentHeight() {
        StatueMenuLayout layout = StatueMenuLayout.calculate(1920, 1080);
        assertEquals(StatueMenuLayout.PREFERRED_PANEL_WIDTH, layout.panel().width());
        assertEquals(StatueMenuLayout.NORMAL_PANEL_HEIGHT, layout.panel().height());
        assertFalse(layout.panel().height() > 300, "dialog height must come from content, not the screen");
    }

    @Test
    void panelIsCenteredInLogicalScreenCoordinates() {
        StatueMenuLayout layout = StatueMenuLayout.calculate(854, 480);
        assertEquals((854 - layout.panel().width()) / 2, layout.panel().x());
        assertEquals((480 - layout.panel().height()) / 2, layout.panel().y());
    }

    @Test
    void panelWidthClampsToSmallScreenMargins() {
        StatueMenuLayout layout = StatueMenuLayout.calculate(320, 180);
        assertEquals(296, layout.panel().width());
        assertEquals(12, layout.panel().x());
    }

    @Test
    void compactLayoutFitsAHighGuiScaleLogicalScreen() {
        StatueMenuLayout layout = StatueMenuLayout.calculate(320, 180);
        assertEquals(StatueMenuLayout.COMPACT_PANEL_HEIGHT, layout.panel().height());
        assertFalse(layout.showSubtitle());
        controls(layout).forEach(bounds -> assertTrue(layout.panel().contains(bounds)));
    }

    @Test
    void normalLayoutShowsSubtitleAndContainsEveryControl() {
        StatueMenuLayout layout = StatueMenuLayout.calculate(640, 360);
        assertTrue(layout.showSubtitle());
        controls(layout).forEach(bounds -> assertTrue(layout.panel().contains(bounds)));
    }

    @Test
    void commonGuiScalesKeepTheDialogCenteredAndControlsBounded() {
        for (int guiScale : List.of(1, 2, 3, 4)) {
            int logicalWidth = 1920 / guiScale;
            int logicalHeight = 1080 / guiScale;
            StatueMenuLayout layout = StatueMenuLayout.calculate(logicalWidth, logicalHeight);
            assertEquals((logicalWidth - layout.panel().width()) / 2, layout.panel().x());
            assertEquals((logicalHeight - layout.panel().height()) / 2, layout.panel().y());
            controls(layout).forEach(bounds -> {
                assertTrue(layout.panel().contains(bounds));
                assertEquals(StatueMenuLayout.WIDGET_HEIGHT, bounds.height());
            });
        }
    }

    @Test
    void fieldsAndButtonsAlwaysUseNormalVanillaHeight() {
        for (int height : List.of(180, 240, 270, 480, 1080)) {
            StatueMenuLayout layout = StatueMenuLayout.calculate(854, height);
            controls(layout).forEach(bounds -> assertEquals(20, bounds.height()));
        }
    }

    @Test
    void controlsRemainOrderedAndNeverOverlap() {
        for (StatueMenuLayout layout : List.of(
                StatueMenuLayout.calculate(320, 180),
                StatueMenuLayout.calculate(480, 270),
                StatueMenuLayout.calculate(1920, 1080))) {
            assertTrue(layout.playerField().bottom() <= layout.scaleField().y());
            assertTrue(layout.scaleField().bottom() <= layout.statusY());
            assertTrue(layout.statusY() + layout.statusHeight() <= layout.createButton().y());
            assertTrue(layout.createButton().bottom() <= layout.undoButton().y());
        }
    }

    @Test
    void undoCanNeverGrowFromAvailableScreenHeight() {
        StatueMenuLayout small = StatueMenuLayout.calculate(320, 180);
        StatueMenuLayout huge = StatueMenuLayout.calculate(3840, 2160);
        assertEquals(StatueMenuLayout.WIDGET_HEIGHT, small.undoButton().height());
        assertEquals(StatueMenuLayout.WIDGET_HEIGHT, huge.undoButton().height());
    }

    @Test
    void statusRegionIsStableRegardlessOfScreenSizeMode() {
        StatueMenuLayout normal = StatueMenuLayout.calculate(640, 360);
        StatueMenuLayout compact = StatueMenuLayout.calculate(320, 180);
        assertEquals(StatueMenuLayout.STATUS_HEIGHT, normal.statusHeight());
        assertEquals(StatueMenuLayout.STATUS_HEIGHT, compact.statusHeight());
    }

    private static List<StatueMenuLayout.Bounds> controls(StatueMenuLayout layout) {
        return List.of(layout.playerField(), layout.scaleField(), layout.createButton(), layout.undoButton());
    }
}
