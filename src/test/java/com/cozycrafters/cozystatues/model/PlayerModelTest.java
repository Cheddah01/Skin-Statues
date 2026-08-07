package com.cozycrafters.cozystatues.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Vanilla proportions, and how the slim model and the outer layer change them. */
class PlayerModelTest {

    private static ModelBox box(PlayerModel model, BodyPart part, boolean overlay) {
        return model.boxes().stream()
                .filter(b -> b.part() == part && b.overlay() == overlay)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void classicModelUsesVanillaBoxSizes() {
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        assertEquals(6, model.boxes().size());
        assertBox(box(model, BodyPart.HEAD, false), 8, 8, 8);
        assertBox(box(model, BodyPart.TORSO, false), 8, 12, 4);
        assertBox(box(model, BodyPart.RIGHT_ARM, false), 4, 12, 4);
        assertBox(box(model, BodyPart.LEFT_ARM, false), 4, 12, 4);
        assertBox(box(model, BodyPart.RIGHT_LEG, false), 4, 12, 4);
        assertBox(box(model, BodyPart.LEFT_LEG, false), 4, 12, 4);
    }

    @Test
    void slimModelOnlyNarrowsTheArms() {
        PlayerModel model = PlayerModel.of(SkinModel.SLIM, false);
        assertBox(box(model, BodyPart.RIGHT_ARM, false), 3, 12, 4);
        assertBox(box(model, BodyPart.LEFT_ARM, false), 3, 12, 4);
        assertBox(box(model, BodyPart.TORSO, false), 8, 12, 4);
        assertBox(box(model, BodyPart.LEFT_LEG, false), 4, 12, 4);
    }

    @Test
    void partsStackIntoAStandingBody() {
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        ModelBox rightLeg = box(model, BodyPart.RIGHT_LEG, false);
        ModelBox leftLeg = box(model, BodyPart.LEFT_LEG, false);
        ModelBox torso = box(model, BodyPart.TORSO, false);
        ModelBox head = box(model, BodyPart.HEAD, false);
        ModelBox rightArm = box(model, BodyPart.RIGHT_ARM, false);
        ModelBox leftArm = box(model, BodyPart.LEFT_ARM, false);

        // Feet on the ground, legs side by side, torso on the legs, head on top.
        assertEquals(0, rightLeg.y());
        assertEquals(0, leftLeg.y());
        assertEquals(rightLeg.maxX() + 1, leftLeg.x());
        assertEquals(rightLeg.maxY() + 1, torso.y());
        assertEquals(torso.maxY() + 1, head.y());
        assertEquals(31, head.maxY(), "a player is 32 pixels tall");

        // Arms hang from the shoulders, one on each side of the torso.
        assertEquals(torso.y(), rightArm.y());
        assertEquals(torso.maxY(), rightArm.maxY());
        assertEquals(torso.x() - rightArm.width(), rightArm.x());
        assertEquals(torso.maxX() + 1, leftArm.x());

        // The head is deeper than the torso and centred on it.
        assertEquals(torso.z() - 2, head.z());
        assertEquals(torso.maxZ() + 2, head.maxZ());
    }

    @Test
    void classicBoundsAreSixteenWideThirtyTwoTallAndEightDeep() {
        ModelBounds bounds = PlayerModel.of(SkinModel.CLASSIC, false).bounds();
        assertEquals(16, bounds.width());
        assertEquals(32, bounds.height());
        assertEquals(8, bounds.depth());
        assertEquals(0, bounds.minY());
    }

    @Test
    void slimBoundsAreTwoPixelsNarrower() {
        ModelBounds bounds = PlayerModel.of(SkinModel.SLIM, false).bounds();
        assertEquals(14, bounds.width());
        assertEquals(32, bounds.height());
        assertEquals(8, bounds.depth());
    }

    @Test
    void outerLayerGrowsEveryDirectionExceptDownwards() {
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, true);
        assertEquals(12, model.boxes().size());
        ModelBounds bounds = model.bounds();
        assertEquals(18, bounds.width());
        assertEquals(33, bounds.height());
        assertEquals(10, bounds.depth());
        // The statue still stands on the ground: the pant shells reach below the
        // feet, but the floor stays pinned at zero and that ring is dropped.
        assertEquals(0, bounds.minY());
    }

    @Test
    void overlayBoxesWrapTheirBaseBoxWithoutOverlappingIt() {
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, true);
        for (BodyPart part : BodyPart.values()) {
            ModelBox base = box(model, part, false);
            ModelBox overlay = box(model, part, true);
            assertEquals(base.x() - 1, overlay.x());
            assertEquals(base.width() + 2, overlay.width());
            assertEquals(base.width(), overlay.texWidth());
            assertEquals(1, overlay.inflate());
            // The overlay's shell sits strictly outside the base box: the base
            // box's own corner is interior to the overlay and never claimed.
            assertFalse(overlay.isOnShell(1, 1, 1), part + " overlay overlaps its base box");
        }
    }

    @Test
    void baseBoxesCarryNoInflation() {
        for (ModelBox box : PlayerModel.of(SkinModel.CLASSIC, false).boxes()) {
            assertEquals(0, box.inflate());
            assertEquals(box.width(), box.texWidth());
            assertEquals(box.height(), box.texHeight());
            assertEquals(box.depth(), box.texDepth());
        }
    }

    @Test
    void shellExcludesInteriorVoxels() {
        ModelBox torso = ModelBox.base(BodyPart.TORSO, 0, 12, 0, 8, 12, 4);
        assertTrue(torso.isOnShell(0, 5, 1));
        assertTrue(torso.isOnShell(3, 0, 1));
        assertTrue(torso.isOnShell(3, 5, 0));
        assertFalse(torso.isOnShell(3, 5, 1), "a voxel with no exposed side is interior");
    }

    @Test
    void localRowsAreCountedDownFromTheTopOfABox() {
        ModelBox head = ModelBox.base(BodyPart.HEAD, 0, 24, -2, 8, 8, 8);
        assertEquals(31, head.modelY(0));
        assertEquals(24, head.modelY(7));
    }

    @Test
    void defaultArmShapeFollowsTheAccountUuid() {
        UUID slim = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID classic = UUID.fromString("00000000-0000-0000-0000-000000000000");
        assertEquals(SkinModel.SLIM, SkinModel.defaultFor(slim));
        assertEquals(SkinModel.CLASSIC, SkinModel.defaultFor(classic));
        assertEquals(4, SkinModel.CLASSIC.armWidth());
        assertEquals(3, SkinModel.SLIM.armWidth());
    }

    private static void assertBox(ModelBox box, int width, int height, int depth) {
        assertEquals(width, box.width(), box.part() + " width");
        assertEquals(height, box.height(), box.part() + " height");
        assertEquals(depth, box.depth(), box.part() + " depth");
    }
}
