package com.cozycrafters.skinstatues.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cozycrafters.skinstatues.model.ModelBounds;
import com.cozycrafters.skinstatues.model.PlayerModel;
import com.cozycrafters.skinstatues.model.SkinModel;
import com.cozycrafters.skinstatues.palette.BlockPalette;
import com.cozycrafters.skinstatues.skin.SkinTexture;
import com.cozycrafters.skinstatues.statue.StatuePlanner.PlannedPixel;
import com.cozycrafters.skinstatues.support.TestSkins;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class StatuePlannerTest {

    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0x000000;

    /** Two blocks far enough apart that a match can only be one or the other. */
    private static StatuePlanner planner() {
        Map<Material, Integer> colors = new LinkedHashMap<>();
        colors.put(Material.WHITE_CONCRETE, WHITE);
        colors.put(Material.BLACK_CONCRETE, BLACK);
        return new StatuePlanner(BlockPalette.of(colors));
    }

    private static List<PlannedPixel> pixels(SkinTexture skin, boolean outerLayer) {
        PlayerModel model = PlayerModel.of(skin.model(), outerLayer);
        return planner().planPixels(skin, model, model.bounds());
    }

    private static Material at(List<PlannedPixel> pixels, int x, int y, int z) {
        return pixels.stream()
                .filter(p -> p.x() == x && p.y() == y && p.z() == z)
                .map(PlannedPixel::material)
                .findFirst()
                .orElse(null);
    }

    @Test
    void aSolidSkinBecomesAHollowShellOfEveryBodyPart() {
        // Shell voxels only: 296 head + 264 torso + 2x152 arms + 2x152 legs.
        List<PlannedPixel> pixels = pixels(TestSkins.solid(WHITE, SkinModel.CLASSIC), false);
        assertEquals(1168, pixels.size());
        assertTrue(pixels.stream().allMatch(p -> p.material() == Material.WHITE_CONCRETE));
    }

    @Test
    void slimArmsMakeAThinnerStatue() {
        List<PlannedPixel> classic = pixels(TestSkins.solid(WHITE, SkinModel.CLASSIC), false);
        List<PlannedPixel> slim = pixels(TestSkins.solid(WHITE, SkinModel.SLIM), false);
        // Each slim arm loses a column: 152 shell voxels become 124.
        assertEquals(classic.size() - 2 * 28, slim.size());
    }

    @Test
    void theStatueIsThirtyTwoPixelsTallAndStandsOnZero() {
        List<PlannedPixel> pixels = pixels(TestSkins.solid(WHITE, SkinModel.CLASSIC), false);
        assertEquals(0, pixels.stream().mapToInt(PlannedPixel::y).min().orElseThrow());
        assertEquals(31, pixels.stream().mapToInt(PlannedPixel::y).max().orElseThrow());
        assertEquals(0, pixels.stream().mapToInt(PlannedPixel::x).min().orElseThrow());
        assertEquals(15, pixels.stream().mapToInt(PlannedPixel::x).max().orElseThrow());
        assertEquals(0, pixels.stream().mapToInt(PlannedPixel::z).min().orElseThrow());
        assertEquals(7, pixels.stream().mapToInt(PlannedPixel::z).max().orElseThrow());
    }

    @Test
    void pixelsComeOutBottomUpSoTheStatueGrowsFromItsFeet() {
        List<PlannedPixel> pixels = pixels(TestSkins.solid(WHITE, SkinModel.CLASSIC), false);
        for (int i = 1; i < pixels.size(); i++) {
            assertTrue(pixels.get(i - 1).y() <= pixels.get(i).y(), "plan is not sorted bottom up");
        }
    }

    @Test
    void transparentPixelsPlaceNothing() {
        SkinTexture blank = TestSkins.modern(TestSkins.blank(), SkinModel.CLASSIC);
        assertEquals(0, pixels(blank, false).size());
        assertEquals(0, pixels(blank, true).size());
    }

    @Test
    void aHoleInTheSkinBecomesAHoleInTheStatue() {
        int[] skin = TestSkins.filled(WHITE);
        // (11,11) is in the middle of the head's front face, so the voxel behind
        // it belongs to that face and nothing else.
        TestSkins.fill(skin, 11, 11, 1, 1, 0x00FFFFFF);
        List<PlannedPixel> pixels = pixels(TestSkins.modern(skin, SkinModel.CLASSIC), false);

        assertNull(at(pixels, 7, 28, 0), "a transparent texel should leave a gap");
        assertEquals(1167, pixels.size());
    }

    @Test
    void aTransparentEdgeTexelFallsBackToTheNextVisibleFace() {
        int[] skin = TestSkins.filled(WHITE);
        // The top-front-right corner of the head shows three faces at once.
        // Clearing only the front one must not open a hole in the profile.
        TestSkins.fill(skin, 8, 8, 1, 1, 0x00FFFFFF);
        List<PlannedPixel> pixels = pixels(TestSkins.modern(skin, SkinModel.CLASSIC), false);

        assertEquals(Material.WHITE_CONCRETE, at(pixels, 4, 31, 0));
        assertEquals(1168, pixels.size());
    }

    @Test
    void theHeadsFrontFaceIsTheSideNearestTheBuilder() {
        int[] skin = TestSkins.filled(WHITE);
        TestSkins.fill(skin, 8, 8, 8, 8, TestSkins.OPAQUE | BLACK);
        List<PlannedPixel> pixels = pixels(TestSkins.modern(skin, SkinModel.CLASSIC), false);

        // z == 0 is the front of the statue, which always faces its builder.
        for (int x = 4; x <= 11; x++) {
            for (int y = 24; y <= 31; y++) {
                assertEquals(Material.BLACK_CONCRETE, at(pixels, x, y, 0),
                        "head front voxel " + x + "," + y + " should come from the face region");
            }
        }
        assertEquals(Material.WHITE_CONCRETE, at(pixels, 4, 31, 7), "the back of the head is not the face");
    }

    @Test
    void theLeftAndRightOfTheHeadAreNotSwapped() {
        int[] left = TestSkins.filled(WHITE);
        TestSkins.fill(left, 16, 8, 8, 8, TestSkins.OPAQUE | BLACK);
        List<PlannedPixel> leftPainted = pixels(TestSkins.modern(left, SkinModel.CLASSIC), false);
        // x grows towards the statue's left, so the head's leftmost column is 11.
        assertEquals(Material.BLACK_CONCRETE, at(leftPainted, 11, 28, 3));
        assertEquals(Material.WHITE_CONCRETE, at(leftPainted, 4, 28, 3));

        int[] right = TestSkins.filled(WHITE);
        TestSkins.fill(right, 0, 8, 8, 8, TestSkins.OPAQUE | BLACK);
        List<PlannedPixel> rightPainted = pixels(TestSkins.modern(right, SkinModel.CLASSIC), false);
        assertEquals(Material.BLACK_CONCRETE, at(rightPainted, 4, 28, 3));
        assertEquals(Material.WHITE_CONCRETE, at(rightPainted, 11, 28, 3));
    }

    @Test
    void theLeftAndRightLegsUseTheirOwnRegions() {
        int[] skin = TestSkins.filled(WHITE);
        // Paint only the left leg's net (16,48)-(31,63).
        TestSkins.fill(skin, 16, 48, 16, 16, TestSkins.OPAQUE | BLACK);
        List<PlannedPixel> pixels = pixels(TestSkins.modern(skin, SkinModel.CLASSIC), false);

        // Legs occupy x 4..7 (right) and 8..11 (left) once normalised, and their
        // front sits at z 2 because the head reaches two pixels further forward.
        assertEquals(Material.WHITE_CONCRETE, at(pixels, 4, 0, 2), "right leg keeps its own texture");
        assertEquals(Material.BLACK_CONCRETE, at(pixels, 8, 0, 2), "left leg uses the left leg region");
    }

    @Test
    void scalingMultipliesEveryAxisUniformly() {
        SkinTexture skin = TestSkins.solid(WHITE, SkinModel.CLASSIC);
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        StatuePlacement placement = StatuePlacement.of(0, 0, 0, Cardinal.NORTH);

        StatuePlan one = planner().plan(skin, model, 1, placement, -64, 320);
        StatuePlan three = planner().plan(skin, model, 3, placement, -64, 320);

        assertEquals(1168, one.blockCount());
        assertEquals(1168 * 27, three.blockCount());
        assertEquals(16, one.widthBlocks());
        assertEquals(48, three.widthBlocks());
        assertEquals(32, one.heightBlocks());
        assertEquals(96, three.heightBlocks());
        assertEquals(8, one.depthBlocks());
        assertEquals(24, three.depthBlocks());
    }

    @Test
    void scaleMustBeAtLeastOne() {
        SkinTexture skin = TestSkins.solid(WHITE, SkinModel.CLASSIC);
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        StatuePlacement placement = StatuePlacement.of(0, 0, 0, Cardinal.NORTH);
        assertThrows(IllegalArgumentException.class, () -> planner().plan(skin, model, 0, placement, -64, 320));
        assertThrows(IllegalArgumentException.class, () -> planner().plan(skin, model, -2, placement, -64, 320));
    }

    @Test
    void blocksOutsideTheWorldHeightAreDroppedRatherThanClamped() {
        SkinTexture skin = TestSkins.solid(WHITE, SkinModel.CLASSIC);
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        // Anchored so only the bottom half of the statue fits under the limit.
        StatuePlan plan = planner().plan(skin, model, 1, StatuePlacement.of(0, 300, 0, Cardinal.NORTH), -64, 315);

        assertTrue(plan.blockCount() > 0);
        assertTrue(plan.blockCount() < 1168, "blocks above the build limit should be dropped");
        for (int i = 0; i < plan.blockCount(); i++) {
            assertTrue(plan.y(i) <= 315, "no block may sit above the build limit");
        }
    }

    @Test
    void aPlanIsWorldAbsoluteAndOrientedByItsPlacement() {
        SkinTexture skin = TestSkins.solid(WHITE, SkinModel.CLASSIC);
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        // Builder faces south, so the statue faces north from one block away.
        StatuePlacement placement = PlacementCalculator.compute(10.5, 64.0, 20.5, 0.0f, 16, 8);
        StatuePlan plan = planner().plan(skin, model, 1, placement, -64, 320);

        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < plan.blockCount(); i++) {
            minZ = Math.min(minZ, plan.z(i));
            maxZ = Math.max(maxZ, plan.z(i));
            minY = Math.min(minY, plan.y(i));
        }
        assertEquals(21, minZ, "the statue starts one block in front of the builder");
        assertEquals(28, maxZ, "and extends away from them");
        assertEquals(64, minY, "standing on the builder's own floor level");
    }

    @Test
    void theOuterLayerWrapsTheBaseLayerWithoutReplacingIt() {
        SkinTexture skin = TestSkins.solid(WHITE, SkinModel.CLASSIC);
        List<PlannedPixel> bare = pixels(skin, false);
        List<PlannedPixel> dressed = pixels(skin, true);
        assertTrue(dressed.size() > bare.size(), "an opaque outer layer adds blocks");

        ModelBounds bounds = PlayerModel.of(SkinModel.CLASSIC, true).bounds();
        assertEquals(18, bounds.width());
        assertEquals(33, bounds.height());
        // The hat is the topmost thing on the statue.
        assertEquals(32, dressed.stream().mapToInt(PlannedPixel::y).max().orElseThrow());
        // Nothing is ever planned below the feet.
        assertEquals(0, dressed.stream().mapToInt(PlannedPixel::y).min().orElseThrow());
    }

    @Test
    void aTransparentOuterLayerCostsNothing() {
        int[] skin = TestSkins.blank();
        // Base layer only: head, torso, both arms, both legs.
        TestSkins.fill(skin, 0, 0, 32, 16, TestSkins.OPAQUE | WHITE);
        TestSkins.fill(skin, 16, 16, 24, 16, TestSkins.OPAQUE | WHITE);
        TestSkins.fill(skin, 40, 16, 16, 16, TestSkins.OPAQUE | WHITE);
        TestSkins.fill(skin, 0, 16, 16, 16, TestSkins.OPAQUE | WHITE);
        TestSkins.fill(skin, 16, 48, 16, 16, TestSkins.OPAQUE | WHITE);
        TestSkins.fill(skin, 32, 48, 16, 16, TestSkins.OPAQUE | WHITE);
        SkinTexture texture = TestSkins.modern(skin, SkinModel.CLASSIC);

        assertEquals(pixels(texture, false).size(), pixels(texture, true).size(),
                "a skin with no hat or jacket should build the same statue either way");
    }

    @Test
    void legacySkinsStillBuildAWholeBody() {
        int[] pixels = new int[64 * 32];
        java.util.Arrays.fill(pixels, TestSkins.OPAQUE | WHITE);
        SkinTexture legacy = SkinTexture.of(64, 32, pixels, SkinModel.CLASSIC);

        // A legacy skin stores no left limbs, but mirroring the right ones
        // still produces exactly the same body as a modern skin would.
        List<PlannedPixel> bare = pixels(legacy, false);
        assertEquals(1168, bare.size());
        assertNotNull(at(bare, 4, 0, 2), "the right leg is built");
        assertNotNull(at(bare, 8, 0, 2), "the mirrored left leg is built");

        // The hat is the only outer layer a legacy skin can contribute, so a
        // dressed legacy statue sits between a bare one and a modern one.
        List<PlannedPixel> dressed = pixels(legacy, true);
        List<PlannedPixel> modern = pixels(TestSkins.solid(WHITE, SkinModel.CLASSIC), true);
        assertTrue(dressed.size() > bare.size(), "the hat is still built from a legacy skin");
        assertTrue(dressed.size() < modern.size(), "but the jacket, sleeves and pants are not");
    }
}
