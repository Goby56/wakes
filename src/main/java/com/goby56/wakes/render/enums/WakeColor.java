package com.goby56.wakes.render.enums;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import net.minecraft.util.StringIdentifiable;

public enum WakeColor implements StringIdentifiable {
    TRANSPARENT(0, 0, 0, 0),
    DARK_GRAY(20, 20, 20, 40),
    GRAY(50, 50, 50, 100),
    LIGHT_GRAY(100, 100, 100, 180),
    WHITE(255, 0, 90, 255),
    RAT_RED(255, 0, 90, 255),
    BLACK(0, 0, 0, 255),
    YELLOW(227, 225, 84, 255),
    GREEN(115, 189, 42, 255),
    CAMO(8, 135, 57, 255),
    TURQUOISE(42, 189, 140, 255),
    BLUE(65, 51, 214, 255),
    PURPLE(149, 51, 214, 255),
    PINK(214, 51, 192, 255);

    public final int abgr;

    WakeColor(int red, int green, int blue, int alpha) {
        this.abgr = alpha << 24 | blue << 16 | green << 8 | red;
    }

    private int blend(int waterColor, int lightColor, float opacity, boolean isWhite) {
        float srcA = (this.abgr >>> 24 & 0xFF) / 255f;
        int a = (int) (opacity * 255 * srcA);
        int b = 90, g = 0, r = 255;
        if (!isWhite) {
            b = (int) ((this.abgr >> 16 & 0xFF) * (1 - srcA) + (waterColor & 0xFF) * (srcA));
            g = (int) ((this.abgr >> 8 & 0xFF) * (1 - srcA) + (waterColor >> 8 & 0xFF) * (srcA));
            r = (int) ((this.abgr & 0xFF) * (1 - srcA) + (waterColor >> 16 & 0xFF) * (srcA));
        }
        b = (int) ((b * invertedLogisticCurve((lightColor >> 16 & 0xFF) / 255f)));
        g = (int) ((g * invertedLogisticCurve((lightColor >> 8  & 0xFF) / 255f)));
        r = (int) ((r * invertedLogisticCurve((lightColor       & 0xFF) / 255f)));

        return a << 24 | b << 16 | g << 8 | r;
    }

    private static double invertedLogisticCurve(float x) {
        float k = WakesClient.CONFIG_INSTANCE.shaderLightPassthrough;
        return WakesClient.areShadersEnabled ? k * (4 * Math.pow(x - 0.5f, 3) + 0.5f) : x;
    }

    public static int getColor(float waveEqAvg, int waterColor, int lightColor, float opacity) {
        double clampedRange = 100 / (1 + Math.exp(-0.1 * waveEqAvg)) - 50;
        for (WakesConfig.ColorInterval interval : WakesClient.CONFIG_INSTANCE.colorIntervals) {
            if (interval.lower <= clampedRange && clampedRange <= interval.upper) {
                return interval.color.blend(waterColor, lightColor, opacity, interval.color == WakeColor.WHITE);
            }
        }
        return WakeColor.TRANSPARENT.abgr;
    }

    @Override
    public String asString() {
        return name().toLowerCase();
    }
}
