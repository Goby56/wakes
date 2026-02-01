package com.goby56.wakes.debug;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.render.WakeColor;
import com.goby56.wakes.simulation.WakeChunk;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Random;

public class WakeDebugRenderer {
    public static void addDebugGizmos() {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return;
        int color = new WakeColor(255, 0, 255, 128).argb;

        if (WakesConfig.drawDebugBoxes) {
            for (var node : wakeHandler.getVisibleNodes()) {
                Gizmos.cuboid(node.toBox(), GizmoStyle.fill(color));
            }
            for (var chunk : wakeHandler.getVisibleChunks()) {
                Vec3 pos = chunk.pos;
                AABB box = new AABB(pos.x, pos.y - (1 - WakeNode.WATER_OFFSET), pos.z, pos.x + WakeChunk.WIDTH, pos.y, pos.z + WakeChunk.WIDTH);
                var col = Color.getHSBColor(new Random(pos.hashCode()).nextFloat(), 1f, 1f).getRGB();
                Gizmos.cuboid(box, GizmoStyle.fill(col));
            }
        }
    }

    public static HudElement wakeAtlasHudLayer() {
        return (guiGraphics, deltaTracker) -> {

        };
    }
}
