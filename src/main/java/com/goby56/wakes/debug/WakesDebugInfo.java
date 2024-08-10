package com.goby56.wakes.debug;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

public class WakesDebugInfo {
    public static double nodeLogicTime = 0;
    public static double insertionTime = 0;
    public static double texturingTime = 0;
    public static ArrayList<Long> cullingTime = new ArrayList<>();
    public static ArrayList<Long> renderingTime = new ArrayList<>(); // Frames averaged each tick
    public static int quadsRendered = 0;
    public static int nodeCount = 0;

    public static void reset() {
        nodeCount = 0;
        nodeLogicTime = 0;
        insertionTime = 0;
        texturingTime = 0;
        cullingTime = new ArrayList<>();
        renderingTime = new ArrayList<>();
    }

    public static void show(CallbackInfoReturnable<List<String>> info) {
        int q = WakesDebugInfo.quadsRendered;
        info.getReturnValue().add(String.format("[Wakes] Rendering %d quads for %d wake nodes", q, WakesDebugInfo.nodeCount));
        info.getReturnValue().add(String.format("[Wakes] Node logic: %.2fms/t", 10e-6 * WakesDebugInfo.nodeLogicTime));
        info.getReturnValue().add(String.format("[Wakes] Insertion: %.2fms/t", 10e-6 * WakesDebugInfo.insertionTime));
        info.getReturnValue().add(String.format("[Wakes] Texturing: %.2fms/t", 10e-6 * WakesDebugInfo.texturingTime));
        info.getReturnValue().add(String.format("[Wakes] Culling: %.3fms/f", 10e-6 * WakesDebugInfo.cullingTime.stream().reduce(0L, Long::sum) / WakesDebugInfo.cullingTime.size()));
        info.getReturnValue().add(String.format("[Wakes] Rendering: %.3fms/f", 10e-6 * WakesDebugInfo.renderingTime.stream().reduce(0L, Long::sum) / WakesDebugInfo.renderingTime.size()));
    }
}
