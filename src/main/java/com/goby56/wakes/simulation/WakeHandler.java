package com.goby56.wakes.simulation;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.render.FrustumManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.util.*;

public class WakeHandler {
    private static WakeHandler INSTANCE;
    public Level world;

    private final HashMap<WakeChunkPos, WakeChunk> wakeChunks = new HashMap<>();
    private final QueueSet<WakeNode> toBeInserted;
    private final ArrayList<SplashPlaneParticle> splashPlanes;

    public static Resolution resolution = WakesConfig.wakeResolution;

    public static boolean resolutionResetScheduled = false;

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
            scheduleResolutionChange(WakesConfig.wakeResolution);
        }
        if (resolutionResetScheduled) {
            toBeInserted.clear();
        }

        ArrayList<WakeChunkPos> toBeRemovedChunks = new ArrayList<>();
        for (WakeChunk chunk : wakeChunks.values()) {
            boolean wakesPresent = chunk.tick();
            if (!wakesPresent) {
                toBeRemovedChunks.add(chunk.chunkPos);
            }
        }
        for (WakeChunkPos pos : toBeRemovedChunks) {
            wakeChunks.remove(pos);
        }

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

        for (int i = this.splashPlanes.size() - 1; i >= 0; i--) {
            if (!this.splashPlanes.get(i).isAlive()) {
                this.splashPlanes.remove(i);
            }
        }
        if (resolutionResetScheduled) {
            this.changeResolution();
        }
    }

    public WakeChunk getChunk(WakeChunkPos pos) {
        return wakeChunks.get(pos);
    }

    public void recolorWakes() {
        for (WakeChunk chunk : wakeChunks.values()) {
            chunk.populatePixels();
        }
        for (var splashPlane : this.splashPlanes) {
            if (splashPlane != null) {
                splashPlane.populatePixels();
            }
        }
    }

    public void registerSplashPlane(SplashPlaneParticle splashPlane) {
        this.splashPlanes.add(splashPlane);
    }

    public void insert(WakeNode node) {
        if (resolutionResetScheduled) return;

        if (node.validPos(world)) {
            this.toBeInserted.add(node);
        }
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

    public static void scheduleResolutionChange(Resolution newRes) {
        resolutionResetScheduled = true;
    }

    private void changeResolution() {
        this.reset();
        WakeHandler.resolution = WakesConfig.wakeResolution;
        resolutionResetScheduled = false;
    }

    private void reset() {
        wakeChunks.clear();
        toBeInserted.clear();
    }
}
