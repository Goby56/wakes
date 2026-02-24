package com.goby56.wakes.debug;

import com.goby56.wakes.config.WakesConfig;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WakesDebugInfo implements DebugScreenEntry {
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

    @Override
    public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
        if (level != null) {
            if (WakesConfig.disableMod) {
                debugScreenDisplayer.addLine("[Wakes] Mod disabled!");
            } else {
                debugScreenDisplayer.addToGroup(Identifier.fromNamespaceAndPath("wakes", "debug_category"),
                        List.of(
                                String.format("[Wakes] Rendering %d quads for %d wake nodes", WakesDebugInfo.quadsRendered, WakesDebugInfo.nodeCount),
                                String.format("[Wakes] Node logic: %.2fms/t", 10e-6 * WakesDebugInfo.nodeLogicTime),
                                String.format("[Wakes] Texturing: %.2fms/t", 10e-6 * WakesDebugInfo.texturingTime),
                                String.format("[Wakes] Rendering: %.3fms/f", 10e-6 * WakesDebugInfo.renderingTime.stream().reduce(0L, Long::sum) / WakesDebugInfo.renderingTime.size())));
            }
        }
    }

    @Override
    public boolean isAllowed(boolean bl) {
        return WakesConfig.showDebugInfo;
    }
}
