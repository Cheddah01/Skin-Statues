package com.cozycrafters.cozystatues.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cozycrafters.cozystatues.model.TextureMapper.Texel;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The mapping from voxel to skin texel is where a statue silently goes wrong:
 * a swapped left and right, or a mirrored back, still produces a statue-shaped
 * pile of blocks. These tests pin every face of every part against the skin
 * layout that Minecraft itself uses.
 */
class TextureMapperTest {

    private static final ModelBox HEAD = ModelBox.base(BodyPart.HEAD, 0, 24, -2, 8, 8, 8);
    private static final ModelBox TORSO = ModelBox.base(BodyPart.TORSO, 0, 12, 0, 8, 12, 4);
    private static final TextureRegion HEAD_REGION = TextureRegion.at(0, 0);
    private static final TextureRegion TORSO_REGION = TextureRegion.at(16, 16);

    private static Texel texel(ModelBox box, TextureRegion region, BoxFace face, int lx, int ly, int lz) {
        return TextureMapper.texel(box, region, face, lx, ly, lz);
    }

    @Test
    void headFrontIsTheEightBySquareAtEightEight() {
        // The face everybody recognises: skin region (8,8) to (15,15).
        assertEquals(new Texel(8, 8), texel(HEAD, HEAD_REGION, BoxFace.FRONT, 0, 0, 0));
        assertEquals(new Texel(15, 15), texel(HEAD, HEAD_REGION, BoxFace.FRONT, 7, 7, 0));
    }

    @Test
    void headFrontRunsFromTheStatuesRightToItsLeft() {
        // Column 0 of the front face is the statue's own right cheek, and it is
        // the column the texture stores first.
        assertEquals(8, texel(HEAD, HEAD_REGION, BoxFace.FRONT, 0, 3, 0).u());
        assertEquals(15, texel(HEAD, HEAD_REGION, BoxFace.FRONT, 7, 3, 0).u());
    }

    @Test
    void headBackIsMirroredRelativeToTheFront() {
        // Seen from behind, the statue's left is on the viewer's left, so the
        // back region runs the other way round: (24,8) is the statue's left.
        assertEquals(new Texel(31, 8), texel(HEAD, HEAD_REGION, BoxFace.BACK, 0, 0, 7));
        assertEquals(new Texel(24, 8), texel(HEAD, HEAD_REGION, BoxFace.BACK, 7, 0, 7));
    }

    @Test
    void headSidesUseTheOuterStripsAndRunFrontToBack() {
        // Right side: (0,8)-(7,15), stored back edge first, so the front edge is u=7.
        assertEquals(new Texel(7, 8), texel(HEAD, HEAD_REGION, BoxFace.RIGHT, 0, 0, 0));
        assertEquals(new Texel(0, 8), texel(HEAD, HEAD_REGION, BoxFace.RIGHT, 0, 0, 7));
        // Left side: (16,8)-(23,15), stored front edge first.
        assertEquals(new Texel(16, 8), texel(HEAD, HEAD_REGION, BoxFace.LEFT, 7, 0, 0));
        assertEquals(new Texel(23, 8), texel(HEAD, HEAD_REGION, BoxFace.LEFT, 7, 0, 7));
    }

    @Test
    void headTopRunsBackToFrontAndTheBottomIsItsVerticalMirror() {
        // The top region's last row touches the front face in the net.
        assertEquals(new Texel(8, 7), texel(HEAD, HEAD_REGION, BoxFace.TOP, 0, 0, 0));
        assertEquals(new Texel(8, 0), texel(HEAD, HEAD_REGION, BoxFace.TOP, 0, 0, 7));
        // The bottom region is flipped vertically, which is why text drawn on
        // the underside of a head looks upside down in skin editors.
        assertEquals(new Texel(16, 0), texel(HEAD, HEAD_REGION, BoxFace.BOTTOM, 0, 7, 0));
        assertEquals(new Texel(16, 7), texel(HEAD, HEAD_REGION, BoxFace.BOTTOM, 0, 7, 7));
    }

    @Test
    void torsoUsesTheDocumentedBodyRegions() {
        assertEquals(new Texel(20, 20), texel(TORSO, TORSO_REGION, BoxFace.FRONT, 0, 0, 0));
        assertEquals(new Texel(27, 31), texel(TORSO, TORSO_REGION, BoxFace.FRONT, 7, 11, 0));
        assertEquals(new Texel(19, 20), texel(TORSO, TORSO_REGION, BoxFace.RIGHT, 0, 0, 0));
        assertEquals(new Texel(28, 20), texel(TORSO, TORSO_REGION, BoxFace.LEFT, 7, 0, 0));
        assertEquals(new Texel(39, 20), texel(TORSO, TORSO_REGION, BoxFace.BACK, 0, 0, 3));
        assertEquals(new Texel(20, 19), texel(TORSO, TORSO_REGION, BoxFace.TOP, 0, 0, 0));
        assertEquals(new Texel(28, 16), texel(TORSO, TORSO_REGION, BoxFace.BOTTOM, 0, 11, 0));
    }

    @Test
    void modernLimbsUseTheirOwnRegions() {
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        assertEquals(new Texel(44, 20), front(model, SkinLayout.MODERN, BodyPart.RIGHT_ARM));
        assertEquals(new Texel(36, 52), front(model, SkinLayout.MODERN, BodyPart.LEFT_ARM));
        assertEquals(new Texel(4, 20), front(model, SkinLayout.MODERN, BodyPart.RIGHT_LEG));
        assertEquals(new Texel(20, 52), front(model, SkinLayout.MODERN, BodyPart.LEFT_LEG));
    }

    @Test
    void slimArmsShiftTheirSideAndBackStripsInByOnePixel() {
        PlayerModel slim = PlayerModel.of(SkinModel.SLIM, false);
        ModelBox arm = box(slim, BodyPart.RIGHT_ARM);
        TextureRegion region = SkinLayout.MODERN.region(BodyPart.RIGHT_ARM, false);
        assertEquals(3, arm.width());
        // Slim right arm: right 40-43, front 44-46, left 47-50, back 51-53.
        assertEquals(44, texel(arm, region, BoxFace.FRONT, 0, 0, 0).u());
        assertEquals(46, texel(arm, region, BoxFace.FRONT, 2, 0, 0).u());
        assertEquals(43, texel(arm, region, BoxFace.RIGHT, 0, 0, 0).u());
        assertEquals(47, texel(arm, region, BoxFace.LEFT, 2, 0, 0).u());
        assertEquals(53, texel(arm, region, BoxFace.BACK, 0, 0, 3).u());
        assertEquals(51, texel(arm, region, BoxFace.BACK, 2, 0, 3).u());
    }

    @Test
    void legacyLeftLimbsMirrorTheRightOnes() {
        PlayerModel model = PlayerModel.of(SkinModel.CLASSIC, false);
        ModelBox leftLeg = box(model, BodyPart.LEFT_LEG);
        TextureRegion region = SkinLayout.LEGACY.region(BodyPart.LEFT_LEG, false);
        assertTrue(region.mirrored());

        // The front face reads right to left instead of left to right ...
        assertEquals(new Texel(7, 20), texel(leftLeg, region, BoxFace.FRONT, 0, 0, 0));
        assertEquals(new Texel(4, 20), texel(leftLeg, region, BoxFace.FRONT, 3, 0, 0));
        // ... and the outer face of the left leg comes from the right leg's
        // right-hand strip, which is what makes it a mirror image.
        assertEquals(new Texel(3, 20), texel(leftLeg, region, BoxFace.LEFT, 3, 0, 0));
        assertEquals(new Texel(8, 20), texel(leftLeg, region, BoxFace.RIGHT, 0, 0, 0));
    }

    @Test
    void outerLayerRepeatsTheBorderTexelOnItsExtraRing() {
        ModelBox hat = ModelBox.overlayOf(HEAD);
        TextureRegion region = SkinLayout.MODERN.region(BodyPart.HEAD, true);
        assertEquals(10, hat.width());
        assertEquals(8, hat.texWidth());

        // The inflated box's own (1,1) is the base box's (0,0): hat region (40,8).
        assertEquals(new Texel(40, 8), texel(hat, region, BoxFace.FRONT, 1, 1, 1));
        assertEquals(new Texel(47, 15), texel(hat, region, BoxFace.FRONT, 8, 8, 1));
        // The extra ring clamps onto the nearest border texel rather than
        // sampling outside the region.
        assertEquals(new Texel(40, 8), texel(hat, region, BoxFace.FRONT, 0, 0, 0));
        assertEquals(new Texel(47, 15), texel(hat, region, BoxFace.FRONT, 9, 9, 0));
    }

    @Test
    void everyBaseTexelOfABoxIsUsedExactlyOnce() {
        // A complete, non-overlapping net: each of the 6 faces covers its own
        // rectangle, so the box consumes exactly 2*(wh + wd + hd) texels.
        Set<Texel> seen = new HashSet<>();
        for (BoxFace face : BoxFace.values()) {
            for (int ly = 0; ly < TORSO.height(); ly++) {
                for (int lx = 0; lx < TORSO.width(); lx++) {
                    for (int lz = 0; lz < TORSO.depth(); lz++) {
                        if (!TORSO.isOnFace(face, lx, ly, lz)) {
                            continue;
                        }
                        assertTrue(seen.add(texel(TORSO, TORSO_REGION, face, lx, ly, lz)),
                                "texel reused by " + face + " at " + lx + "," + ly + "," + lz);
                    }
                }
            }
        }
        int w = TORSO.width();
        int h = TORSO.height();
        int d = TORSO.depth();
        assertEquals(2 * (w * h + w * d + h * d), seen.size());
    }

    @Test
    void aBoxNetStaysInsideItsOwnRegionOfTheSkin() {
        for (BoxFace face : BoxFace.values()) {
            for (int ly = 0; ly < HEAD.height(); ly++) {
                for (int lx = 0; lx < HEAD.width(); lx++) {
                    for (int lz = 0; lz < HEAD.depth(); lz++) {
                        if (!HEAD.isOnFace(face, lx, ly, lz)) {
                            continue;
                        }
                        Texel texel = texel(HEAD, HEAD_REGION, face, lx, ly, lz);
                        assertTrue(texel.u() >= 0 && texel.u() < 32, "u out of the head net: " + texel);
                        assertTrue(texel.v() >= 0 && texel.v() < 16, "v out of the head net: " + texel);
                    }
                }
            }
        }
    }

    @Test
    void mirroringOnlySwapsTheSideFaces() {
        assertEquals(BoxFace.RIGHT, BoxFace.LEFT.mirroredX());
        assertEquals(BoxFace.LEFT, BoxFace.RIGHT.mirroredX());
        assertEquals(BoxFace.FRONT, BoxFace.FRONT.mirroredX());
        assertEquals(BoxFace.BACK, BoxFace.BACK.mirroredX());
        assertEquals(BoxFace.TOP, BoxFace.TOP.mirroredX());
        assertEquals(BoxFace.BOTTOM, BoxFace.BOTTOM.mirroredX());
        assertFalse(TextureRegion.at(0, 0).mirrored());
    }

    private static Texel front(PlayerModel model, SkinLayout layout, BodyPart part) {
        ModelBox box = box(model, part);
        return texel(box, layout.region(part, false), BoxFace.FRONT, 0, 0, 0);
    }

    private static ModelBox box(PlayerModel model, BodyPart part) {
        return model.boxes().stream()
                .filter(b -> b.part() == part && !b.overlay())
                .findFirst()
                .orElseThrow();
    }
}
