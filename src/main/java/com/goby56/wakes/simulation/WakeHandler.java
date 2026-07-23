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

        // Insert newly-spawned nodes (and create their chunks) before the tick/draw pass below,
        // so a node created this tick — e.g. right at a boat's current position — gets its first
        // draw() call this same tick, tested against this same tick's occlusion zones. Draining
        // this after the tick/draw loop (as before) meant a brand-new node's first draw() call
        // happened next tick, by which point a moving occluder had already moved past it, so the
        // one tick where it truly overlapped never coincided with an actual draw call.
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

    /** The exact zone list used for the most recent tick's actual texel painting — for debug
     *  visualization only, so what's drawn matches 1:1 with what really got tested, rather than
     *  a fresh (possibly render-time-skewed) recomputation. */
    public List<OcclusionZone> getLastOcclusionZones() {
        return lastOcclusionZones;
    }

    private List<OcclusionZone> computeOcclusionZones() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        List<OcclusionZone> zones = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ProducesWake producer) {
                OcclusionDimensions dims = producer.wakes$getOcclusionDimensions();
                if (dims != null) zones.add(OcclusionZone.from(entity, dims));
            }
        }
        return zones;
    }

    public void registerSplashPlane(SplashPlaneParticle splashPlane) {
        this.splashPlanes.add(splashPlane);
    }

    public void insert(WakeNode node) {
        if (node.validPos(world) && this.toBeInserted.add(node)) {
            return; // successfully queued
        }
        node.markDead(); // invalid position or deduplicated by QueueSet — free the drawContext
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
