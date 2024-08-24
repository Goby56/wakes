package com.goby56.wakes.debug;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

public class WakesDebugInfo {
    public static double nodeLogicTime = 0;
    public static double texturingTime = 0;
    public static ArrayList<Long> renderingTime = new ArrayList<>(); // Frames averaged each tick
    public static int quadsRendered = 0;
    public static int nodeCount = 0;

    public static void reset() {
        nodeCount = 0;
        nodeLogicTime = 0;
        texturingTime = 0;
        renderingTime = new ArrayList<>();
    }

    public static void show(CallbackInfoReturnable<List<String>> info) {
        info.getReturnValue().add(String.format("[Wakes] Rendering %d quads for %d wake nodes", WakesDebugInfo.quadsRendered, WakesDebugInfo.nodeCount));
        info.getReturnValue().add(String.format("[Wakes] Node logic: %.2fms/t", 10e-6 * WakesDebugInfo.nodeLogicTime));
        info.getReturnValue().add(String.format("[Wakes] Texturing: %.2fms/t", 10e-6 * WakesDebugInfo.texturingTime));
        info.getReturnValue().add(String.format("[Wakes] Rendering: %.3fms/f", 10e-6 * WakesDebugInfo.renderingTime.stream().reduce(0L, Long::sum) / WakesDebugInfo.renderingTime.size()));
    }
}
