package com.goby56.wakes.utils;

import com.goby56.wakes.config.WakesConfig;
import net.minecraft.util.Pair;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;


public enum WakeColor implements StringIdentifiable {
    TRANSPARENT(0, 0, 0, 0),
    DARK_GRAY(147, 153, 166, 40),
    GRAY(158, 165, 176, 100),
    LIGHT_GRAY(196, 202, 209, 180),
    WHITE(255, 255, 255, 255),
    RED(189, 42, 42, 255),
    ORANGE(214, 111, 51, 255),
    YELLOW(227, 225, 84, 255),
    GREEN(115, 189, 42, 255),
    CAMO(8, 135, 57, 255),
    TURQUOISE(42, 189, 140, 255),
    BLUE(65, 51, 214, 255),
    PURPLE(149, 51, 214, 255),
    PINK(214, 51, 192, 255);

    public int argb;
    public ArrayList<Pair<Integer, Integer>> intervals;

    WakeColor(int red, int green, int blue, int alpha) {
        this.argb = ColorHelper.Argb.getArgb(alpha, blue, green, red); // abgr actually because big-endian?
    }

    public static WakeColor getColor(float avg) {
//            double clampedRange = 255 * (1 - 1 / (0.1 * Math.abs(avg) + 1));
        double clampedRange = 100 / (1 + Math.exp(-0.1 * avg)) - 50;
        WakesConfig.ColorInterval interval;
        for (int i = 0; i < 9; i++) {
            interval = WakesConfig.colorIntervals[i];
            if (interval.lower <= clampedRange && clampedRange <= interval.upper) {
                return interval.color;
            }
        }
        return WHITE;
    }

    @Override
    public String asString() {
        return name().toLowerCase();
    }
}
