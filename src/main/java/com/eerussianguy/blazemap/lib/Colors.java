package com.eerussianguy.blazemap.lib;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Colors {
    public static final int ALPHA = 0xFF000000;
    public static final int NO_TINT = -1;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = ALPHA;
    public static final int DISABLED = 0xFF666666;
    public static final int UNFOCUSED = 0xFFA0A0A0;
    public static final int LABEL_COLOR = 0xFF404040;
    public static final int WIDGET_BACKGROUND = 0xA0000000;

    protected static final Map<Integer, Float> darknessPointCache = new ConcurrentHashMap<Integer, Float>();

    public static int layerBlend(int bottom, int top) {
        if((top & ALPHA) == ALPHA) return top; // top is opaque, use top
        if((top & ALPHA) == 0) return bottom; // top is transparent, use bottom
        if((bottom & ALPHA) == 0) return top; // bottom is transparent, use top

        float point = ((float) (top >> 24)) / 255F;
        return 0xFF000000 | interpolate(bottom, 0, top, 1, point);
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

    public static int multiplyRGB(int a, int b) {
        int _r = multiplyChannel((a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int _g = multiplyChannel((a >>  8) & 0xFF, (b >>  8) & 0xFF);
        int _b = multiplyChannel(a & 0xFF, b & 0xFF);
        return (_r << 16) | (_g << 8) | _b;
    }

    public static int multiplyChannel(float a, float b) {
        return 0xFF & (int) (a * b / 255F);
    }

    public static int setAlpha(int color, byte alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static float interpolate(float a, float b, float p) {
        a *= (1F - p);
        b *= p;
        return Math.max(0, Math.min(1.0f, a + b));
    }


    /**
     * This is a value to represent the lessening light as it attenuates while passing through semitransparent objects.
     * Will always be in the range [0 - 0.96667] (aka 0 to almost but not quite 1).
     * Higher is more shadowed.
     * 
     * @param depth How many transparent blocks the light has passed through
     */
    public static float getDarknessPoint(int depth) {
        return darknessPointCache.computeIfAbsent(depth, (size) -> {
            return Math.min(2.90f, 0.375f * (float)Math.log(size + 1)) / 3;
        });
    }


    /**
     * Calculate the colour of the bottom block as seen through the top block
     * 
     * @param top The higher block colour values
     * @param bottom The lower block colour values
     * @param depth How many transparent blocks are above this one
     * @return
     */
    public static float[] filterARGB(float[] top, float[] bottom, int depth) {
        // Adjust bottom brightness for light attenuation
        float point = getDarknessPoint(depth);

        for (int i = 1; i < 4; i++) {
            // Attenuate from depth
            bottom[i] -= bottom[i] * (point);

            // Filter the below colour through the above colour
            bottom[i] = interpolate(bottom[i], top[i], top[0]);
        }

        // Adjust opacity
        bottom[0] = 1 - ((1 - bottom[0]) * (1 - top[0]));

        return bottom;
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

    public static int HSB2RGB(float[] hsb) {
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    public static int HSB2RGB(float h, float s, float b) {
        return Color.HSBtoRGB(h, s, b);
    }

    public static float[] RGB2HSB(int color) {
        float[] hsb = new float[3];
        int r = (color & 0xFF0000) >> 16;
        int g = (color & 0xFF00) >> 8;
        int b =  color & 0xFF;
        Color.RGBtoHSB(r, g, b, hsb);
        return hsb;
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
    public static float[] decomposeRGBA(int color, float[] arr) {
        arr[0] = ((color >> 24) & 0xFF) / 255f;
        arr[1] = ((color >> 16) & 0xFF) / 255f;
        arr[2] = ((color >> 8) & 0xFF) / 255f;
        arr[3] = ((color) & 0xFF) / 255f;
        return arr;
    }

    public static int recomposeRGBA(float[] color) {
        int a = (((int)(color[0] * 255f)) & 0xFF);
        int r = (((int)(color[1] * 255f)) & 0xFF);
        int g = (((int)(color[2] * 255f)) & 0xFF);
        int b = (((int)(color[3] * 255f)) & 0xFF);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static float[] decomposeRGB(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;
        return new float[] {r, g, b};
    }

    public static int[] decomposeIntRGBA(int color) {
        int a = (int)((color >> 24) & 0xFF);
        int r = (int)((color >> 16) & 0xFF);
        int g = (int)((color >> 8) & 0xFF);
        int b = (int)((color) & 0xFF);
        return new int[] {a, r, g, b};
    }
}
