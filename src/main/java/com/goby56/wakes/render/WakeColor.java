package com.goby56.wakes.render;

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

    public WakeColor(int argb) {
        // Minecraft seems to work with argb but OpenGL uses abgr
        this(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
    }

    // no h/s/v fields: nothing in the codebase reads them (ColorPicker computes its own HSB
    // separately), and this constructor runs per-pixel in the wake render hot path, so the
    // Color.RGBtoHSB call this used to do here was pure waste
    public WakeColor(int red, int green, int blue, int alpha) {
        this.argb = alpha << 24 | red << 16 | green << 8 | blue;
        this.abgr = alpha << 24 | blue << 16 | green << 8 | red;
        this.a = alpha;
        this.r = red;
        this.g = green;
        this.b = blue;
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

    /** There are only wakeColorIntervals.size()+1 distinct wake colors; which one a texel gets
     *  depends only on its wave height, not on the node's tint/opacity. So for a fixed tint and
     *  opacity (i.e. one node on one tick), the whole result set is just buildBucketColors()'s
     *  small array, indexed by this. */
    public static int resolveBucket(float waveEqAvg) {
        double clampedRange = 1 / (1 + Math.exp(-0.1 * waveEqAvg));
        var ranges = WakesConfig.wakeColorIntervals;
        for (int i = 0; i < ranges.size(); i++) {
            if (clampedRange < ranges.get(i)) return i;
        }
        return ranges.size();
    }

    /** Precomputes the final blended ARGB for every bucket once, so per-texel color lookup in
     *  the render hot path is just an array index instead of re-running blend()'s Math.pow and
     *  allocating new WakeColors for every one of a node's texels. */
    public static int[] buildBucketColors(int fluidCol, float opacity) {
        WakeColor tint = new WakeColor(fluidCol);
        int bucketCount = WakesConfig.wakeColorIntervals.size() + 1;
        int[] colors = new int[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            colors[i] = WakesConfig.getWakeColor(i).blend(tint, opacity).argb;
        }
        return colors;
    }

    public WakeColor modifyOpacity(float opacityMultiplier) {
        return new WakeColor(r, g, b, (int) (this.a * opacityMultiplier));
    }

    public WakeColor blend(WakeColor tint, float opacity) {
        double srcA = Math.pow(this.a / 255f, WakesConfig.blendStrength * 10);
        // Pow to make tint color have a larger influence\
        // Potentially convert this into lookup table
        
        int r = (int) ((this.r) * (srcA) + (tint.r) * (1 - srcA));
        int g = (int) ((this.g) * (srcA) + (tint.g) * (1 - srcA));
        int b = (int) ((this.b) * (srcA) + (tint.b) * (1 - srcA));

        return new WakeColor(r, g, b, (int) (this.a * opacity));
    }
}
