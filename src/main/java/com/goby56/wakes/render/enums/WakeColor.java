package com.goby56.wakes.render.enums;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;

import java.awt.*;

public class WakeColor {
    public final int argb;
    public final int abgr;
    public final int r;
    public final int g;
    public final int b;
    public final int a;
    public final float h;
    public final float s;
    public final float v;


    public WakeColor(int argb) {
        // Minecraft seems to work with argb but OpenGL uses abgr
        this(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
    }

    public WakeColor(int red, int green, int blue, int alpha) {
        this.argb = alpha << 24 | red << 16 | green << 8 | blue;
        this.abgr = alpha << 24 | blue << 16 | green << 8 | red;
        this.a = alpha;
        this.r = red;
        this.g = green;
        this.b = blue;
        var hsv = Color.RGBtoHSB(red, green, blue, null);
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];
    }

    public WakeColor(float hue, float saturation, float value, float opacity) {
        this(((int)((1f - opacity) * 255)) << 24 ^ Color.HSBtoRGB(hue, saturation, value));
    }

    public WakeColor(String argbHex) {
        this(Integer.parseUnsignedInt(argbHex.replace("#", ""), 16));
    }

    public String toHex() {
        return "#" + Integer.toHexString(a << 24 | r << 16 | g << 8 | b);
    }

    private static double invertedLogisticCurve(float x) {
        float k = WakesConfig.shaderLightPassthrough;
        return WakesClient.areShadersEnabled ? k * (4 * Math.pow(x - 0.5f, 3) + 0.5f) : x;
    }

    public static int sampleColor(float waveEqAvg, int fluidCol, int lightColor, float opacity) {
        WakeColor tint = new WakeColor(fluidCol);
        double clampedRange = 1 / (1 + Math.exp(-0.1 * waveEqAvg));
        var ranges = WakesConfig.wakeColorIntervals;
        int returnIndex = ranges.size();
        for (int i = 0; i < ranges.size(); i++) {
            if (clampedRange < ranges.get(i)) {
                returnIndex = i;
                break;
            }
        }
        WakeColor color = WakesConfig.getWakeColor(returnIndex);
        return color.blend(tint, lightColor, opacity).abgr;
    }

    public WakeColor blend(WakeColor tint, int lightColor, float opacity) {
        float srcA = this.a / 255f;
        int a = (int) (opacity * 255 * srcA);
        int r = (int) ((this.r) * (srcA) + (tint.r) * (1 - srcA));
        int g = (int) ((this.g) * (srcA) + (tint.g) * (1 - srcA));
        int b = (int) ((this.b) * (srcA) + (tint.b) * (1 - srcA));

        r = (int) ((r * invertedLogisticCurve((lightColor       & 0xFF) / 255f)));
        g = (int) ((g * invertedLogisticCurve((lightColor >> 8  & 0xFF) / 255f)));
        b = (int) ((b * invertedLogisticCurve((lightColor >> 16 & 0xFF) / 255f)));

        return new WakeColor(r, g, b, a);
    }
}
