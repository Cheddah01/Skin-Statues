package com.cozycrafters.skinstatues.fabric.palette;

import net.minecraft.world.level.block.Block;

/**
 * One usable pixel-art block and the average colour it should be matched
 * against, pre-converted to L*a*b* so matching never re-does the conversion.
 */
public record PaletteEntry(Block material, int rgb, LabColor lab) {

    public static PaletteEntry of(Block material, int rgb) {
        return new PaletteEntry(material, rgb & 0xFFFFFF, LabColor.fromRgb(rgb));
    }

    public int red() {
        return (rgb >> 16) & 0xFF;
    }

    public int green() {
        return (rgb >> 8) & 0xFF;
    }

    public int blue() {
        return rgb & 0xFF;
    }
}
