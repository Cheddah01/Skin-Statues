package com.cozycrafters.skinstatues.fabric.statue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cozycrafters.skinstatues.fabric.model.BodyPart;
import com.cozycrafters.skinstatues.fabric.model.ModelBox;
import com.cozycrafters.skinstatues.fabric.model.PlayerModel;
import com.cozycrafters.skinstatues.fabric.model.SkinLayout;
import com.cozycrafters.skinstatues.fabric.model.SkinModel;
import com.cozycrafters.skinstatues.fabric.model.TextureRegion;
import com.cozycrafters.skinstatues.fabric.palette.BlockPalette;
import com.cozycrafters.skinstatues.fabric.skin.SkinTexture;
import com.cozycrafters.skinstatues.fabric.support.TestSkins;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

class OuterLayerGeometryTest {

    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0x000000;
    private static final StatuePlacement PLACEMENT = StatuePlacement.of(0, 0, 0, Cardinal.NORTH);

    private static StatuePlanner planner() {
        Map<Block, Integer> colors = new LinkedHashMap<>();
        colors.put(Blocks.CONCRETE.white(), WHITE);
        colors.put(Blocks.CONCRETE.black(), BLACK);
        return new StatuePlanner(BlockPalette.of(colors));
    }

    @Test
    void displacementIsOneWorldBlockAtScalesOneTwoAndFourOnEveryFaceNormal() {
        for (int scale : new int[]{1, 2, 4}) {
            ModelBox base = base(BodyPart.HEAD, 1);
            PlayerModel model = model(base, true);
            StatuePlan plan = plan(paintedSkin(base, true), model, scale);
            int centreX = 1 + base.width() * scale / 2;
            int centreY = base.y() * scale + base.height() * scale / 2;
            int centreZ = 1 + base.depth() * scale / 2;

            assertEquals(Blocks.CONCRETE.black(), at(plan, 0, centreY, centreZ), "right at scale " + scale);
            assertEquals(Blocks.CONCRETE.white(), at(plan, 1, centreY, centreZ));
            assertEquals(Blocks.CONCRETE.black(),
                    at(plan, base.width() * scale + 1, centreY, centreZ), "left at scale " + scale);
            assertEquals(Blocks.CONCRETE.white(),
                    at(plan, base.width() * scale, centreY, centreZ));

            assertEquals(Blocks.CONCRETE.black(), at(plan, centreX, centreY, 0), "front at scale " + scale);
            assertEquals(Blocks.CONCRETE.white(), at(plan, centreX, centreY, 1));
            assertEquals(Blocks.CONCRETE.black(),
                    at(plan, centreX, centreY, base.depth() * scale + 1), "back at scale " + scale);
            assertEquals(Blocks.CONCRETE.white(),
                    at(plan, centreX, centreY, base.depth() * scale));

            int baseBottom = base.y() * scale;
            int baseTop = (base.y() + base.height()) * scale - 1;
            assertEquals(Blocks.CONCRETE.black(), at(plan, centreX, baseBottom - 1, centreZ),
                    "bottom at scale " + scale);
            assertEquals(Blocks.CONCRETE.white(), at(plan, centreX, baseBottom, centreZ));
            assertEquals(Blocks.CONCRETE.black(), at(plan, centreX, baseTop + 1, centreZ),
                    "top at scale " + scale);
            assertEquals(Blocks.CONCRETE.white(), at(plan, centreX, baseTop, centreZ));

            assertEquals(base.width() * scale + 2, plan.widthBlocks());
            assertEquals((base.y() + base.height()) * scale + 1, plan.heightBlocks());
            assertEquals(base.depth() * scale + 2, plan.depthBlocks());
        }
    }

    @Test
    void everyBodyPartUsesTheFixedWorldSpaceShell() {
        for (BodyPart part : BodyPart.values()) {
            ModelBox base = base(part, 1);
            PlayerModel model = model(base, true);
            StatuePlan plan = plan(paintedSkin(base, true), model, 4);
            int centreX = 1 + base.width() * 2;
            int centreY = base.y() * 4 + base.height() * 2;

            assertEquals(Blocks.CONCRETE.black(), at(plan, centreX, centreY, 0),
                    part + " overlay must be one block in front of its base surface");
            assertEquals(Blocks.CONCRETE.white(), at(plan, centreX, centreY, 1),
                    part + " base orientation must remain unchanged");
        }
    }

    @Test
    void oneOverlayTexelStillCoversAScaleByScaleSurfacePatch() {
        int scale = 4;
        ModelBox base = base(BodyPart.HEAD, 1);
        int[] pixels = baseOnlySkin(base);
        TextureRegion overlay = SkinLayout.MODERN.region(BodyPart.HEAD, true);
        int tx = 2;
        int ty = 3;
        TestSkins.fill(pixels, overlay.u() + base.depth() + tx,
                overlay.v() + base.depth() + ty, 1, 1, TestSkins.OPAQUE | BLACK);
        StatuePlan plan = plan(TestSkins.modern(pixels, SkinModel.CLASSIC), model(base, true), scale);

        int patchMinX = 1 + tx * scale;
        int patchMinY = (base.y() + base.height() - 1 - ty) * scale;
        int black = 0;
        for (int i = 0; i < plan.blockCount(); i++) {
            if (plan.material(i) == Blocks.CONCRETE.black()) {
                black++;
            }
        }
        assertEquals(scale * scale, black);
        for (int x = patchMinX; x < patchMinX + scale; x++) {
            for (int y = patchMinY; y < patchMinY + scale; y++) {
                assertEquals(Blocks.CONCRETE.black(), at(plan, x, y, 0));
            }
        }
    }

    @Test
    void opaqueShellHasClosedEdgesAndCornersWithoutDuplicateBlocks() {
        int scale = 4;
        ModelBox base = base(BodyPart.HEAD, 1);
        StatuePlan plan = plan(paintedSkin(base, true), model(base, true), scale);
        int width = base.width() * scale + 2;
        int height = base.height() * scale + 2;
        int depth = base.depth() * scale + 2;
        int expectedOverlay = width * height * depth
                - (width - 2) * (height - 2) * (depth - 2);
        int actualOverlay = 0;
        for (int i = 0; i < plan.blockCount(); i++) {
            if (plan.material(i) == Blocks.CONCRETE.black()) {
                actualOverlay++;
            }
        }

        assertEquals(expectedOverlay, actualOverlay,
                "every shell position, including edges and corners, should appear exactly once");
        assertEquals(Blocks.CONCRETE.black(), at(plan, 0, base.y() * scale - 1, 0));
        assertEquals(Blocks.CONCRETE.black(),
                at(plan, width - 1, (base.y() + base.height()) * scale, depth - 1));
    }

    @Test
    void transparentOverlayTexelsProduceNoBlocks() {
        ModelBox base = base(BodyPart.TORSO, 1);
        SkinTexture skin = TestSkins.modern(baseOnlySkin(base), SkinModel.CLASSIC);
        StatuePlan dressed = plan(skin, model(base, true), 4);
        StatuePlan bare = plan(skin, model(base, false), 4);

        assertEquals(bare.blockCount(), dressed.blockCount());
        assertNull(at(dressed, 1 + base.width() * 2,
                base.y() * 4 + base.height() * 2, 0));
    }

    @Test
    void fullPlayerOverlaysAreDeduplicatedAndNeverBuriedInsideBaseGeometry() {
        int scale = 4;
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, true);
        int[] pixels = TestSkins.blank();
        for (ModelBox box : model.boxes()) {
            TextureRegion region = SkinLayout.MODERN.region(box.part(), box.overlay());
            paintNet(pixels, region, box,
                    TestSkins.OPAQUE | (box.overlay() ? BLACK : WHITE));
        }
        StatuePlan plan = plan(TestSkins.modern(pixels, SkinModel.CLASSIC), model, scale);
        Set<Long> positions = new HashSet<>();
        int scaledMinX = -4 * scale - 1;
        int scaledMinZ = -2 * scale - 1;

        for (int i = 0; i < plan.blockCount(); i++) {
            assertTrue(positions.add(StatuePlan.pack(plan.x(i), plan.y(i), plan.z(i))),
                    "the final plan must not place the same world block twice");
            if (plan.material(i) != Blocks.CONCRETE.black()) {
                continue;
            }
            int bx = localX(plan.x(i));
            int bz = localZ(plan.z(i));
            int rawX = bx + scaledMinX;
            int rawY = plan.y(i);
            int rawZ = bz + scaledMinZ;
            for (ModelBox box : model.boxes()) {
                if (!box.overlay()) {
                    assertTrue(!inside(box, rawX, rawY, rawZ, scale),
                            box.part() + " base geometry contains an outer-layer block");
                }
            }
        }
    }

    private static StatuePlan plan(SkinTexture skin, PlayerModel model, int scale) {
        return planner().plan(skin, model, scale, PLACEMENT, -64, 320);
    }

    private static Block at(StatuePlan plan, int bx, int by, int bz) {
        int worldX = PLACEMENT.worldX(bx, bz);
        int worldY = PLACEMENT.worldY(by);
        int worldZ = PLACEMENT.worldZ(bx, bz);
        for (int i = 0; i < plan.blockCount(); i++) {
            if (plan.x(i) == worldX && plan.y(i) == worldY && plan.z(i) == worldZ) {
                return plan.material(i);
            }
        }
        return null;
    }

    private static int localX(int worldX) {
        for (int bx = 0; bx < 100; bx++) {
            if (PLACEMENT.worldX(bx, 0) == worldX) {
                return bx;
            }
        }
        throw new AssertionError("world X is outside the test plan");
    }

    private static int localZ(int worldZ) {
        for (int bz = 0; bz < 100; bz++) {
            if (PLACEMENT.worldZ(0, bz) == worldZ) {
                return bz;
            }
        }
        throw new AssertionError("world Z is outside the test plan");
    }

    private static boolean inside(ModelBox box, int x, int y, int z, int scale) {
        return x >= box.x() * scale && x < (box.x() + box.width()) * scale
                && y >= box.y() * scale && y < (box.y() + box.height()) * scale
                && z >= box.z() * scale && z < (box.z() + box.depth()) * scale;
    }

    private static PlayerModel model(ModelBox base, boolean outerLayer) {
        List<ModelBox> boxes = outerLayer
                ? List.of(base, ModelBox.overlayOf(base))
                : List.of(base);
        return new PlayerModel(SkinModel.CLASSIC, outerLayer, boxes);
    }

    private static ModelBox base(BodyPart part, int y) {
        return switch (part) {
            case HEAD -> ModelBox.base(part, 0, y, 0, 8, 8, 8);
            case TORSO -> ModelBox.base(part, 0, y, 0, 8, 12, 4);
            case RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG -> ModelBox.base(part, 0, y, 0, 4, 12, 4);
        };
    }

    private static SkinTexture paintedSkin(ModelBox base, boolean overlay) {
        int[] pixels = baseOnlySkin(base);
        if (overlay) {
            paintNet(pixels, SkinLayout.MODERN.region(base.part(), true), base, TestSkins.OPAQUE | BLACK);
        }
        return TestSkins.modern(pixels, SkinModel.CLASSIC);
    }

    private static int[] baseOnlySkin(ModelBox base) {
        int[] pixels = TestSkins.blank();
        paintNet(pixels, SkinLayout.MODERN.region(base.part(), false), base, TestSkins.OPAQUE | WHITE);
        return pixels;
    }

    private static void paintNet(int[] pixels, TextureRegion region, ModelBox box, int argb) {
        TestSkins.fill(pixels, region.u(), region.v(),
                2 * box.texDepth() + 2 * box.texWidth(), box.texDepth() + box.texHeight(), argb);
    }
}
