package com.cozycrafters.skinstatues.palette;

/**
 * A colour in CIE L*a*b*, the space the palette matches in.
 *
 * <p>Plain RGB distance is a poor stand-in for how different two colours look;
 * skin tones in particular collapse together under it. Converting through
 * linear sRGB and CIE XYZ into L*a*b* and taking the squared Euclidean distance
 * there (CIE76) keeps the matcher perceptually sensible while staying cheap
 * enough to run per pixel.
 */
public record LabColor(double l, double a, double b) {

    private static final double WHITE_X = 95.047;
    private static final double WHITE_Y = 100.0;
    private static final double WHITE_Z = 108.883;

    public static LabColor fromRgb(int rgb) {
        return fromRgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static LabColor fromRgb(int red, int green, int blue) {
        double r = linearize(red / 255.0) * 100.0;
        double g = linearize(green / 255.0) * 100.0;
        double bl = linearize(blue / 255.0) * 100.0;

        double x = r * 0.4124 + g * 0.3576 + bl * 0.1805;
        double y = r * 0.2126 + g * 0.7152 + bl * 0.0722;
        double z = r * 0.0193 + g * 0.1192 + bl * 0.9505;

        double fx = pivot(x / WHITE_X);
        double fy = pivot(y / WHITE_Y);
        double fz = pivot(z / WHITE_Z);

        return new LabColor(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz));
    }

    /** Squared CIE76 distance; ordering is all the matcher needs, so no square root. */
    public double squaredDistance(LabColor other) {
        double dl = l - other.l;
        double da = a - other.a;
        double db = b - other.b;
        return dl * dl + da * da + db * db;
    }

    private static double linearize(double channel) {
        return channel > 0.04045 ? Math.pow((channel + 0.055) / 1.055, 2.4) : channel / 12.92;
    }

    private static double pivot(double value) {
        return value > 0.008856 ? Math.cbrt(value) : (7.787 * value) + (16.0 / 116.0);
    }
}
