package com.goby56.wakes.utils;

import java.util.ArrayList;

public class WakesDebugInfo {
    public static double nodeLogicTime = 0;
    public static double texturingTime = 0;
    public static ArrayList<Long> drawingTime = new ArrayList<>();
    public static ArrayList<Long> wakeRenderingTime = new ArrayList<>(); // Frames averaged each tick
    public static int quadsRendered = 0;

    public static void reset() {
        nodeLogicTime = 0;
        texturingTime = 0;
        drawingTime = new ArrayList<>();
        wakeRenderingTime = new ArrayList<>();
    }
}
