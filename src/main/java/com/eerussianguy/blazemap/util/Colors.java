package com.eerussianguy.blazemap.util;

import java.awt.*;

public class Colors {
    public static final int NO_TINT = -1;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int DISABLED = 0x666666;
    public static final int LABEL_COLOR = 0xFF404040;
    public static final int WIDGET_BACKGROUND = 0xA0000000;

    public static int layerBlend(int bottom, int top) {
        if((top & 0xFF000000) == 0xFF000000) return top; // top is opaque, use top
        if((top & 0xFF000000) == 0) return bottom; // top is transparent, use bottom
        if((bottom & 0xFF000000) == 0) return top; // bottom is transparent, use top

        float point = ((float) (top >> 24)) / 255F;
        return 0xFF000000 | interpolate(bottom, 0, top, 1, point);

        // float alpha = top >> 24;
        // return 0xFF000000 | interpolate(bottom, 0, top, 255, alpha);
    }

    public static int interpolate(int color1, float key1, int color2, float key2, float point) {
        point = (point - key1) / (key2 - key1);
        int b0 = interpolate((color1 >> 24) & 0xFF, (color2 >> 24) & 0xFF, point);
        int b1 = interpolate((color1 >> 16) & 0xFF, (color2 >> 16) & 0xFF, point);
        int b2 = interpolate((color1 >> 8) & 0xFF, (color2 >> 8) & 0xFF, point);
        int b3 = interpolate(color1 & 0xFF, color2 & 0xFF, point);
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    public static int interpolate(int a, int b, float p) {
        a *= (1F - p);
        b *= p;
        return Math.max(0, Math.min(255, a + b));
    }

    public static int abgr(Color color) {
        return color.getAlpha() << 24 | color.getBlue() << 16 | color.getGreen() << 8 | color.getRed();
    }

    public static int abgr(int color) {
        int r = color & 0xFF0000;
        int b = color & 0x0000FF;
        color &= 0xFF00FF00;
        return color | (b << 16) | (r >> 16);
    }

    public static int randomBrightColor() {
        float hue = ((float) (System.nanoTime() / 1000) % 360) / 360F;
        return Color.HSBtoRGB(hue, 1, 1);
    }

    public static float[] decomposeRGBA(int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;
        return new float[] {a, r, g, b};
    }

    public static float[] decomposeRGB(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;
        return new float[] {r, g, b};
    }
}
