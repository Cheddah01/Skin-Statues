package com.cozycrafters.cozystatues.skin;

import com.cozycrafters.cozystatues.model.SkinModel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Turns downloaded PNG bytes into a {@link SkinTexture}. */
public final class SkinDecoder {

    private SkinDecoder() {
    }

    public static SkinTexture decode(byte[] png, SkinModel model) throws SkinLookupException {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException | RuntimeException ex) {
            throw new SkinLookupException("That player's skin image could not be read.", ex);
        }
        if (image == null) {
            throw new SkinLookupException("That player's skin image could not be read.");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);
        try {
            return SkinTexture.of(width, height, argb, model);
        } catch (IllegalArgumentException ex) {
            throw new SkinLookupException("That player's skin image has an unsupported size.", ex);
        }
    }
}
