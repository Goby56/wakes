package com.goby56.wakes.render.enums;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import net.minecraft.util.math.ColorHelper;

import java.awt.*;

public class WakeColor {
    public final int argb;
    public final int abgr;
    public final int r;
    public final int g;
    public final int b;
    public final int a;
    public boolean isHighlight;


    public WakeColor(int argb, boolean isHighlight) {
       this(argb);
       this.isHighlight = isHighlight;
    }

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
        this.isHighlight = false;
    }

    public WakeColor(float hue, float saturation, float value, float opacity) {
        // TODO FIX 0% OPACITY INTERPRETED AS 100%
        this(((int)(1f - opacity * 255)) << 24 ^ Color.HSBtoRGB(hue, saturation, value));
    }

    private static double invertedLogisticCurve(float x) {
        float k = WakesClient.CONFIG_INSTANCE.shaderLightPassthrough;
        return WakesClient.areShadersEnabled ? k * (4 * Math.pow(x - 0.5f, 3) + 0.5f) : x;
    }

    public static int sampleColor(float waveEqAvg, int waterColor, int lightColor, float opacity) {
        WakeColor tint = new WakeColor(waterColor);
        double clampedRange = 100 / (1 + Math.exp(-0.1 * waveEqAvg)) - 50;
        int highlightIndex = WakesClient.CONFIG.customization.highlight.get();
        int i = 0;
        for (var colorInterval : WakesClient.CONFIG.customization.colorIntervals.getEntries()) {
            if (colorInterval.getKey() >= clampedRange) {
                WakeColor color = new WakeColor(colorInterval.getValue().argb(), i == highlightIndex);
                return color.blend(tint, lightColor, opacity).abgr;
            }
            i++;
        }
        return 0;
    }

    public WakeColor blend(WakeColor tint, int lightColor, float opacity) {
        float srcA = this.a / 255f;
        int a = (int) (opacity * 255 * srcA);
        int r = 255, g = 255, b = 255;
        if (!this.isHighlight) {
            r = (int) ((this.r) * (1 - srcA) + (tint.r) * (srcA));
            g = (int) ((this.g) * (1 - srcA) + (tint.g) * (srcA));
            b = (int) ((this.b) * (1 - srcA) + (tint.b) * (srcA));
        }
        r = (int) ((r * invertedLogisticCurve((lightColor       & 0xFF) / 255f)));
        g = (int) ((g * invertedLogisticCurve((lightColor >> 8  & 0xFF) / 255f)));
        b = (int) ((b * invertedLogisticCurve((lightColor >> 16 & 0xFF) / 255f)));

        return new WakeColor(r, g, b, a);
    }
}
