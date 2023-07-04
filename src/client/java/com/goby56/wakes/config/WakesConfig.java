package com.goby56.wakes.config;

import com.goby56.wakes.utils.WakeColor;

public class WakesConfig {

    public static float waveSpeed = 0.95f;
    public static int initialStrength = 20;
    public static int paddleStrength = 100;
    public static float waveDecay = 0.9f;
    public static boolean useAgeDecay = false;

    public static int floodFillDistance = 3;
    public static boolean use9PointStencil = true;
    public static int ticksBeforeFill = 2;
    public static boolean drawDebugBoxes = false;

    public static ColorInterval[] colorIntervals = new ColorInterval[] {
            new ColorInterval(WakeColor.TRANSPARENT, -50, -45),
            new ColorInterval(WakeColor.DARK_GRAY, -45, -35),
            new ColorInterval(WakeColor.GRAY, -35, -30),
            new ColorInterval(WakeColor.LIGHT_GRAY, -30, -15),
            new ColorInterval(WakeColor.TRANSPARENT, -15, 2),
            new ColorInterval(WakeColor.LIGHT_GRAY, 2, 10),
            new ColorInterval(WakeColor.WHITE, 10, 20),
            new ColorInterval(WakeColor.LIGHT_GRAY, 20, 40),
            new ColorInterval(WakeColor.GRAY, 40, 50),
    };

    public static boolean useWaterBlending = true;

    public static class ColorInterval {
        public WakeColor color;
        public int lower;
        public int upper;

        public ColorInterval(WakeColor color, int lower, int upper) {
            this.color = color;
            this.lower = lower;
            this.upper = upper;
        }

        public void setColor(WakeColor color) {
            this.color = color;
        }

        public void setLower(int lower) {
            this.lower = lower;
        }

        public void setUpper(int upper) {
            this.upper = upper;
        }

    }


}
