package com.goby56.wakes.simulation;

public record WakeChunkPos(int cx, int y, int cz) {
    public static WakeChunkPos fromWakeNode(WakeNode wakeNode) {
        return new WakeChunkPos(Math.floorDiv(wakeNode.x, WakeChunk.WIDTH), wakeNode.y, Math.floorDiv(wakeNode.z, WakeChunk.WIDTH));
    }
}
