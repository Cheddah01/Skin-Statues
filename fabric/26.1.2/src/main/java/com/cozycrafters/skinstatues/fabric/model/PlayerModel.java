package com.cozycrafters.skinstatues.fabric.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a standing player: six base boxes and, optionally, the six
 * outer-layer boxes around them. Pure data, in skin pixels, with no notion of
 * skins, worlds or scale.
 *
 * <p>Vanilla proportions, with the ground plane at {@code y == 0}:
 * legs 4x12x4 side by side, a 8x12x4 torso on top of them, 4x12x4 (or 3x12x4)
 * arms hanging from the shoulders, and a 8x8x8 head centred on the torso.
 */
public record PlayerModel(SkinModel model, boolean outerLayer, List<ModelBox> boxes) {

    /** Base boxes come first so that outer-layer voxels win any shared block. */
    public static PlayerModel of(SkinModel model, boolean outerLayer) {
        int arm = model.armWidth();
        List<ModelBox> base = List.of(
                ModelBox.base(BodyPart.RIGHT_LEG, 0, 0, 0, 4, 12, 4),
                ModelBox.base(BodyPart.LEFT_LEG, 4, 0, 0, 4, 12, 4),
                ModelBox.base(BodyPart.TORSO, 0, 12, 0, 8, 12, 4),
                ModelBox.base(BodyPart.RIGHT_ARM, -arm, 12, 0, arm, 12, 4),
                ModelBox.base(BodyPart.LEFT_ARM, 8, 12, 0, arm, 12, 4),
                ModelBox.base(BodyPart.HEAD, 0, 24, -2, 8, 8, 8));

        List<ModelBox> boxes = new ArrayList<>(base);
        if (outerLayer) {
            for (ModelBox box : base) {
                boxes.add(ModelBox.overlayOf(box));
            }
        }
        return new PlayerModel(model, outerLayer, List.copyOf(boxes));
    }

    /**
     * The model's bounding box. The floor is pinned to {@code y == 0} even with
     * the outer layer enabled: the pant shells would otherwise hang one pixel
     * below the feet and lift the whole statue off the ground for the sake of a
     * face nobody can see.
     */
    public ModelBounds bounds() {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ModelBox box : boxes) {
            minX = Math.min(minX, box.x());
            minZ = Math.min(minZ, box.z());
            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        return new ModelBounds(minX, 0, minZ, maxX - minX + 1, maxY + 1, maxZ - minZ + 1);
    }
}
