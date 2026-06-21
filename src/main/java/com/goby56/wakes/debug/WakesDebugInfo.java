package com.goby56.wakes.debug;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.render.WakeTextureAtlas;
import com.goby56.wakes.simulation.WakeHandler;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WakesDebugInfo implements DebugScreenEntry {
    // Accumulated each tick; reset to 0 at the start of each tick
    public static double nodeLogicTime = 0;
    public static double atlasWriteTime = 0;
    public static int totalNodes = 0;

    // Set once per render frame; NOT reset each tick so display never flickers to 0
    public static int visibleNodes = 0;
    public static int splashPlanes = 0;

    // Snapshotted at the end of each tick so the displayed value is stable for a full tick
    private static double avgRenderTime = 0;
    private static ArrayList<Long> renderingTime = new ArrayList<>();

    public static void addRenderTime(long nanos) {
        renderingTime.add(nanos);
    }

    public static void reset() {
        // Snapshot render average before clearing, so display stays stable this tick
        avgRenderTime = renderingTime.isEmpty() ? avgRenderTime
                : 1e-6 * renderingTime.stream().reduce(0L, Long::sum) / renderingTime.size();
        renderingTime = new ArrayList<>();

        // These are re-accumulated during the upcoming tick
        nodeLogicTime = 0;
        atlasWriteTime = 0;
        totalNodes = 0;
        // visibleNodes and splashPlanes are intentionally NOT reset here
    }

    @Override
    public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
        if (level == null) return;
        if (WakesConfig.disableMod) {
            debugScreenDisplayer.addLine("[Wakes] Mod disabled!");
            return;
        }

        int atlasUsed = 0;
        int atlasCapacity = WakeTextureAtlas.CAPACITY;
        var wh = WakeHandler.getInstance();
        if (wh.isPresent()) atlasUsed = wh.get().getTextureAtlas().occupiedCount();

        debugScreenDisplayer.addToGroup(Identifier.fromNamespaceAndPath("wakes", "debug_category"),
                List.of(
                        String.format("[Wakes] %d quads (%d nodes total), %d splash plane%s",
                                visibleNodes, totalNodes, splashPlanes, splashPlanes == 1 ? "" : "s"),
                        String.format("[Wakes] Atlas: %d / %d slots used",
                                atlasUsed, atlasCapacity),
                        String.format("[Wakes] Logic: %.2fms/t | Write: %.2fms/t | Render: %.3fms/f",
                                1e-6 * nodeLogicTime, 1e-6 * atlasWriteTime, avgRenderTime)));
    }

    @Override
    public boolean isAllowed(boolean bl) {
        return WakesConfig.showDebugInfo;
    }
}
