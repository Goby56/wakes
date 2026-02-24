package com.goby56.wakes.simulation;

public record WakeChunkPos(int cx, int y, int cz) {
    public static WakeChunkPos fromWakeNode(WakeNode wakeNode) {
        return new WakeChunkPos(Math.floorDiv(wakeNode.x, WakeChunk.WIDTH), wakeNode.y, Math.floorDiv(wakeNode.z, WakeChunk.WIDTH));
    }

    public enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    public WakeChunkPos offset(Direction direction) {
        return switch (direction) {
            case NORTH -> new WakeChunkPos(cx, y, cz - 1);
            case SOUTH -> new WakeChunkPos(cx, y, cz + 1);
            case EAST -> new WakeChunkPos(cx + 1, y, cz);
            case WEST -> new WakeChunkPos(cx - 1, y, cz);
        };
    }
}
