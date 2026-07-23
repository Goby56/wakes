package com.goby56.wakes.debug;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.render.OcclusionDimensions;
import com.goby56.wakes.render.WakeColor;
import com.goby56.wakes.render.WakeTextureAtlas;
import com.goby56.wakes.simulation.OcclusionZone;
import com.goby56.wakes.simulation.WakeChunk;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;
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

        if (WakesConfig.drawOcclusionZones) {
            // Red wireframe around every node the broad-phase SAT (OcclusionZone.overlapsNode)
            // currently considers "nearby" for the exact zone list used by this tick's real
            // draw() calls — lets you directly compare the broad-phase decision against both the
            // yellow zone gizmo and the actual wake texture, instead of inferring it indirectly.
            List<OcclusionZone> zones = wakeHandler.getLastOcclusionZones();
            if (!zones.isEmpty()) {
                int overlapColor = new WakeColor(255, 0, 0, 200).argb;
                for (WakeNode node : wakeHandler.getVisibleNodes()) {
                    for (OcclusionZone zone : zones) {
                        if (zone.overlapsNode(node.x, node.z)) {
                            AABB box = new AABB(node.x, node.y, node.z, node.x + 1, node.y + 1, node.z + 1);
                            Gizmos.cuboid(box, GizmoStyle.stroke(overlapColor, 2f));
                            break;
                        }
                    }
                }
            }
        }
    }

    /** Hooked to a render event (not tick) purely so this fires every frame — NOT for smooth
     *  interpolation. It deliberately draws the exact same OcclusionZone.from(entity, dims) the
     *  real tick-time occlusion test builds: same raw entity.position()/getYRot(), same padded
     *  half-extents. No interpolation, on purpose — smoothing between two states necessarily
     *  shows something that matches neither exactly, which defeats the point of this gizmo
     *  (anything inside it must be guaranteed to actually get excluded). It'll visibly snap to
     *  each tick's position rather than glide, which is the correct tradeoff for a debug tool
     *  that has to be trustworthy over looking nice. */
    public static void addOcclusionZoneGizmos(LevelRenderContext context) {
        if (!WakesConfig.drawOcclusionZones) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ProducesWake producer) {
                OcclusionDimensions dims = producer.wakes$getOcclusionDimensions();
                Float wakeHeight = producer.wakes$wakeHeight();
                if (dims != null && wakeHeight != null) {
                    addOcclusionZoneGizmo(OcclusionZone.from(entity, dims), wakeHeight);
                }
            }
        }
    }

    /** Draws a single occlusion zone as a flat, yaw-oriented rectangle (its actual SAT footprint,
     *  not an axis-aligned approximation), raised to the wake's own render height so it lines up
     *  with the actual wake texture, not the entity's (often lower, e.g. a boat's floor) true Y
     *  position. Every other value (x, z, cos, sin, half-extents) comes directly from the same
     *  OcclusionZone object WakeNode.draw() tests texels against — nothing recomputed here. */
    private static void addOcclusionZoneGizmo(OcclusionZone zone, float wakeHeight) {
        // Matches WakeNode's own render height (floor(wakeHeight) + WATER_OFFSET) rather than
        // wakeHeight directly — wakeHeight is already a fractional world height, and wake nodes
        // re-derive their render height from its floored block Y, not the raw value.
        double y = Math.floor(wakeHeight) + WakeNode.WATER_OFFSET;
        Vec3 pos = new Vec3(zone.x(), y, zone.z());

        Vec3 c0 = rotatedCorner(pos, -zone.halfWidth(), -zone.halfLength(), zone.cos(), zone.sin());
        Vec3 c1 = rotatedCorner(pos, zone.halfWidth(), -zone.halfLength(), zone.cos(), zone.sin());
        Vec3 c2 = rotatedCorner(pos, zone.halfWidth(), zone.halfLength(), zone.cos(), zone.sin());
        Vec3 c3 = rotatedCorner(pos, -zone.halfWidth(), zone.halfLength(), zone.cos(), zone.sin());

        int fill = new WakeColor(255, 255, 0, 96).argb;
        int stroke = new WakeColor(255, 255, 0, 255).argb;
        Gizmos.rect(c0, c1, c2, c3, GizmoStyle.strokeAndFill(stroke, 2f, fill));
    }

    private static Vec3 rotatedCorner(Vec3 center, double localX, double localZ, double cos, double sin) {
        double worldX = localX * cos - localZ * sin;
        double worldZ = localX * sin + localZ * cos;
        return new Vec3(center.x + worldX, center.y, center.z + worldZ);
    }

    public static void drawAtlasOverlay(GuiGraphicsExtractor context, DeltaTracker deltaTracker) {
        if (!WakesConfig.showAtlas) return;

        WakeHandler.getInstance().ifPresent(wh -> {
            WakeTextureAtlas atlas = wh.getTextureAtlas();
            int screenH = Minecraft.getInstance().getWindow().getScreenHeight();
            context.blit(RenderPipelines.GUI_TEXTURED, WakeTextureAtlas.ATLAS_ID,
                    0, 0, 0, 0,
                    screenH, screenH,
                    atlas.resolution, atlas.resolution,
                    atlas.resolution, atlas.resolution
            );
        });

    }
}
