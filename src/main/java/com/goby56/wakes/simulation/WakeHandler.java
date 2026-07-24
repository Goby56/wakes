package com.goby56.wakes.simulation;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.render.FrustumManager;
import com.goby56.wakes.render.OcclusionDimensions;
import com.goby56.wakes.render.WakeTextureAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class WakeHandler {
    private static WakeHandler INSTANCE;
    public Level world;

    private final HashMap<WakeChunkPos, WakeChunk> wakeChunks = new HashMap<>();
    private final QueueSet<WakeNode> toBeInserted;
    private final ArrayList<SplashPlaneParticle> splashPlanes;

    public static Resolution resolution = WakesConfig.wakeResolution;
    private WakeTextureAtlas textureAtlas;
    private List<OcclusionZone> lastOcclusionZones = List.of();

    // cheap per-tick heuristic that narrows the per-frame precise SAT+per-texel pass
    // down to a small candidate set instead of re-testing every wake node every frame.
    private final Set<WakeChunk> chunksNeedingFrameRefresh = new HashSet<>();

    private WakeHandler(Level world) {
        this.world = world;
        this.toBeInserted = new QueueSet<>();
        this.splashPlanes = new ArrayList<>();
    }

    public static Optional<WakeHandler> getInstance() {
        if (INSTANCE == null) {
            if (Minecraft.getInstance().level == null) {
                return Optional.empty();
            }
            INSTANCE = new WakeHandler(Minecraft.getInstance().level);
        }
        return Optional.of(INSTANCE);
    }

    public static void init(Level world) {
        INSTANCE = new WakeHandler(world);
    }

    public static void kill() {
        getInstance().ifPresent(wakeHandler -> wakeHandler.wakeChunks.clear());
        INSTANCE = null;
    }

    public void tick() {
        if (WakesConfig.wakeResolution.res != WakeHandler.resolution.res) {
            WakeHandler.resolution = WakesConfig.wakeResolution;
            textureAtlas.setResolution(resolution.res);
            reset();
        } else {
            wakeLogic();
        }
    }

    private void wakeLogic() {
        List<OcclusionZone> occlusionZones = computeOcclusionZones();
        this.lastOcclusionZones = occlusionZones;

        while (toBeInserted.peek() != null) {
            WakeNode node = toBeInserted.poll();
            WakeChunkPos pos = WakeChunkPos.fromWakeNode(node);
            WakeChunk chunk = wakeChunks.get(pos);
            if (chunk == null) {
                chunk = new WakeChunk(pos, this);
                wakeChunks.put(pos, chunk);
            }
            chunk.insert(node);
        }

        ArrayList<WakeChunkPos> toBeRemovedChunks = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            boolean wakesPresent = chunk.tick(occlusionZones);
            if (!wakesPresent) {
                chunk.destroy();
                toBeRemovedChunks.add(chunk.chunkPos);
            }
        }
        for (WakeChunkPos pos : toBeRemovedChunks) {
            wakeChunks.remove(pos);
        }

        for (int i = this.splashPlanes.size() - 1; i >= 0; i--) {
            if (!this.splashPlanes.get(i).isAlive()) {
                this.splashPlanes.remove(i);
            }
        }

    }

    public WakeChunk getChunk(WakeChunkPos pos) {
        return wakeChunks.get(pos);
    }

    public void recolorWakes() {
        List<OcclusionZone> occlusionZones = computeOcclusionZones();
        this.lastOcclusionZones = occlusionZones;
        for (WakeChunk chunk : wakeChunks.values()) {
            chunk.drawWakes(occlusionZones);
        }
        for (var splashPlane : this.splashPlanes) {
            if (splashPlane != null) {
                splashPlane.populatePixels();
            }
        }
    }

    /** The exact zone list most recently tested: tick-rate baseline, or frame-rate interpolated
     *  wherever something nearby is moving. Debug visualization only, so what's drawn matches
     *  what actually got tested. */
    public List<OcclusionZone> getLastOcclusionZones() {
        return lastOcclusionZones;
    }

    private List<OcclusionZone> computeOcclusionZones() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        List<OcclusionZone> zones = new ArrayList<>();
        chunksNeedingFrameRefresh.clear();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ProducesWake producer) {
                OcclusionDimensions dims = producer.wakes$getOcclusionDimensions();
                Float wakeHeight = producer.wakes$wakeHeight();
                if (dims != null && wakeHeight != null) {
                    OcclusionZone zone = OcclusionZone.from(entity, dims, wakeHeight);
                    zones.add(zone);

                    boolean moved = entity.getX() != entity.xo || entity.getY() != entity.yo
                            || entity.getZ() != entity.zo || entity.getYRot() != entity.yRotO;
                    if (moved) {
                        markChunksNeedingFrameRefresh(entity, dims, wakeHeight);
                    }
                }
            }
        }
        return zones;
    }

    private void markChunksNeedingFrameRefresh(Entity entity, OcclusionDimensions dims, float wakeHeight) {
        // Generous on X/Z since WakeNode.draw() does the exact test anyway. Exact on Y: use
        // wakeHeight, not entity.getY(), since a sunk entity's wake still surfaces above it.
        double reach = OcclusionZone.paddedHalfWidth(dims) + OcclusionZone.paddedHalfLength(dims) + 1.0;
        int y = (int) Math.floor(wakeHeight);
        AABB reachBox = new AABB(
                entity.getX() - reach, y, entity.getZ() - reach,
                entity.getX() + reach, y + 1, entity.getZ() + reach
        );
        for (WakeChunk chunk : wakeChunks.values()) {
            if (chunk.boundingBox.intersects(reachBox)) {
                chunksNeedingFrameRefresh.add(chunk);
            }
        }
    }

    /** Called every frame, before the atlas upload, so a moving occluder's mask tracks its
     *  interpolated position instead of sitting frozen until the next tick. No-op when nothing
     *  nearby moved this tick. */
    public void refreshInterpolatedOcclusion(float partialTick) {
        if (chunksNeedingFrameRefresh.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        List<OcclusionZone> interpolatedZones = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ProducesWake producer) {
                OcclusionDimensions dims = producer.wakes$getOcclusionDimensions();
                Float wakeHeight = producer.wakes$wakeHeight();
                if (dims != null && wakeHeight != null) {
                    interpolatedZones.add(OcclusionZone.fromInterpolated(entity, dims, wakeHeight, partialTick));
                }
            }
        }
        this.lastOcclusionZones = interpolatedZones; // keep the debug gizmo in sync

        for (WakeChunk chunk : chunksNeedingFrameRefresh) {
            chunk.drawWakes(interpolatedZones);
        }
    }

    public void registerSplashPlane(SplashPlaneParticle splashPlane) {
        this.splashPlanes.add(splashPlane);
    }

    public void insert(WakeNode node) {
        if (node.validPos(world) && this.toBeInserted.add(node)) {
            return; // successfully queued
        }
        node.markDead(); // invalid position or deduplicated by QueueSet, free the drawContext
    }

    public List<WakeNode> getVisibleNodes() {
        ArrayList<WakeNode> nodes = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            if (FrustumManager.isVisible(chunk.boundingBox)) {
                chunk.query(nodes);
            }
        }
        return nodes;
    }

    public List<WakeChunk> getVisibleChunks() {
        ArrayList<WakeChunk> chunks = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            if (FrustumManager.isVisible(chunk.boundingBox)) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public List<SplashPlaneParticle> getVisibleSplashPlanes() {
        ArrayList<SplashPlaneParticle> splashPlanes = new ArrayList<>();
        for (SplashPlaneParticle particle : this.splashPlanes) {
            if (FrustumManager.isVisible(particle.getBoundingBox())) {
                splashPlanes.add(particle);
            }
        }
        return splashPlanes;
    }

    public WakeTextureAtlas getTextureAtlas() {
        if (textureAtlas == null) {
            textureAtlas = new WakeTextureAtlas();
            textureAtlas.setResolution(resolution.res);
        }
        return textureAtlas;
    }

    private void reset() {
        for (WakeChunk chunk : wakeChunks.values()) {
            chunk.destroy();
        }
        wakeChunks.clear();
        toBeInserted.clear();
    }
}
