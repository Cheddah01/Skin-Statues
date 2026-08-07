package com.cozycrafters.cozystatues.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The two skin layouts, pinned against the documented region origins. */
class SkinLayoutTest {

    @Test
    void sizeDecidesTheLayout() {
        assertEquals(SkinLayout.MODERN, SkinLayout.forSize(64, 64));
        assertEquals(SkinLayout.MODERN, SkinLayout.forSize(128, 128));
        assertEquals(SkinLayout.LEGACY, SkinLayout.forSize(64, 32));
        assertEquals(SkinLayout.LEGACY, SkinLayout.forSize(128, 64));
    }

    @Test
    void modernBaseRegionsMatchTheSkinTemplate() {
        assertEquals(TextureRegion.at(0, 0), SkinLayout.MODERN.region(BodyPart.HEAD, false));
        assertEquals(TextureRegion.at(16, 16), SkinLayout.MODERN.region(BodyPart.TORSO, false));
        assertEquals(TextureRegion.at(40, 16), SkinLayout.MODERN.region(BodyPart.RIGHT_ARM, false));
        assertEquals(TextureRegion.at(32, 48), SkinLayout.MODERN.region(BodyPart.LEFT_ARM, false));
        assertEquals(TextureRegion.at(0, 16), SkinLayout.MODERN.region(BodyPart.RIGHT_LEG, false));
        assertEquals(TextureRegion.at(16, 48), SkinLayout.MODERN.region(BodyPart.LEFT_LEG, false));
    }

    @Test
    void modernOuterRegionsMatchTheSkinTemplate() {
        assertEquals(TextureRegion.at(32, 0), SkinLayout.MODERN.region(BodyPart.HEAD, true));
        assertEquals(TextureRegion.at(16, 32), SkinLayout.MODERN.region(BodyPart.TORSO, true));
        assertEquals(TextureRegion.at(40, 32), SkinLayout.MODERN.region(BodyPart.RIGHT_ARM, true));
        assertEquals(TextureRegion.at(48, 48), SkinLayout.MODERN.region(BodyPart.LEFT_ARM, true));
        assertEquals(TextureRegion.at(0, 32), SkinLayout.MODERN.region(BodyPart.RIGHT_LEG, true));
        assertEquals(TextureRegion.at(0, 48), SkinLayout.MODERN.region(BodyPart.LEFT_LEG, true));
    }

    @Test
    void noModernRegionIsMirrored() {
        for (BodyPart part : BodyPart.values()) {
            assertFalse(SkinLayout.MODERN.region(part, false).mirrored(), part + " base");
            assertFalse(SkinLayout.MODERN.region(part, true).mirrored(), part + " outer");
        }
    }

    @Test
    void legacySkinsReuseTheRightLimbsMirroredForTheLeft() {
        assertEquals(SkinLayout.LEGACY.region(BodyPart.RIGHT_ARM, false).u(),
                SkinLayout.LEGACY.region(BodyPart.LEFT_ARM, false).u());
        assertEquals(SkinLayout.LEGACY.region(BodyPart.RIGHT_LEG, false).u(),
                SkinLayout.LEGACY.region(BodyPart.LEFT_LEG, false).u());
        assertTrue(SkinLayout.LEGACY.region(BodyPart.LEFT_ARM, false).mirrored());
        assertTrue(SkinLayout.LEGACY.region(BodyPart.LEFT_LEG, false).mirrored());
        assertFalse(SkinLayout.LEGACY.region(BodyPart.RIGHT_ARM, false).mirrored());
    }

    @Test
    void legacySkinsOnlyStoreTheHatOuterLayer() {
        assertNotNull(SkinLayout.LEGACY.region(BodyPart.HEAD, true));
        assertNull(SkinLayout.LEGACY.region(BodyPart.TORSO, true));
        assertNull(SkinLayout.LEGACY.region(BodyPart.RIGHT_ARM, true));
        assertNull(SkinLayout.LEGACY.region(BodyPart.LEFT_ARM, true));
        assertNull(SkinLayout.LEGACY.region(BodyPart.RIGHT_LEG, true));
        assertNull(SkinLayout.LEGACY.region(BodyPart.LEFT_LEG, true));
    }

    @Test
    void everyModernRegionFitsInsideASixtyFourSquareSkin() {
        // Each net is (2*width + 2*depth) wide and (depth + height) tall.
        for (BodyPart part : BodyPart.values()) {
            int width = part.isArm() ? 4 : (part == BodyPart.HEAD || part == BodyPart.TORSO ? 8 : 4);
            int height = part == BodyPart.HEAD ? 8 : 12;
            int depth = part == BodyPart.HEAD ? 8 : 4;
            for (boolean overlay : new boolean[]{false, true}) {
                TextureRegion region = SkinLayout.MODERN.region(part, overlay);
                assertTrue(region.u() + 2 * width + 2 * depth <= 64, part + " net runs off the skin");
                assertTrue(region.v() + depth + height <= 64, part + " net runs off the skin");
            }
        }
    }
}
