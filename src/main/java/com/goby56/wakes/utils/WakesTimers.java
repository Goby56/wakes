package com.goby56.wakes.utils;

import java.util.ArrayList;

public class WakesTimers {
    public static double meshGenerationTime = 0;
    public static double nodeLogicTime = 0;
    public static double texturingTime = 0;
    public static ArrayList<Double> wakeRenderingTime = new ArrayList<>(); // Each frame averaged every tick

    public static void reset() {
        meshGenerationTime = 0;
        nodeLogicTime = 0;
        texturingTime = 0;
        wakeRenderingTime = new ArrayList<>();
    }
}
