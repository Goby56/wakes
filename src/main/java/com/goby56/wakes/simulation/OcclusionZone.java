package com.goby56.wakes.simulation;

import com.goby56.wakes.render.OcclusionDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A snapshot of an occluding entity's world-space position/yaw plus its resolved
 * OcclusionDimensions, computed once so the (fairly cheap, but not free) trig doesn't get redone
 * for every node/texel it's tested against. cos/sin describe the zone's local axes: (cos,sin) is
 * its "width" axis, (-sin,cos) its "length" axis, matching WakeDebugRenderer's corner rotation
 * convention (local -> world is worldX = x + lx*cos - lz*sin, worldZ = z + lx*sin + lz*cos).
 *
 * y is the wake node grid layer this zone applies to: floor(wakeHeight), not entity.getY(). A
 * sunk entity's wake still surfaces above it, and overlapsNode/contains are otherwise X/Z only,
 * so using the entity's own Y would spuriously match unrelated nodes that just share X/Z.
 */
public record OcclusionZone(double x, double z, int y, double cos, double sin, double halfWidth, double halfLength) {

    public static OcclusionZone from(Entity entity, OcclusionDimensions dimensions, float wakeHeight) {
        Vec3 pos = entity.position();
        double rad = Math.toRadians(entity.getYRot());
        return new OcclusionZone(pos.x, pos.z, (int) Math.floor(wakeHeight), Math.cos(rad), Math.sin(rad),
                paddedHalfWidth(dimensions), paddedHalfLength(dimensions));
    }

    // zones remove texels per frame so we need to interpolate it to get an accurate mask of where texels should get removed
    public static OcclusionZone fromInterpolated(Entity entity, OcclusionDimensions dimensions, float wakeHeight, float partialTick) {
        Vec3 pos = entity.getPosition(partialTick);
        double rad = Math.toRadians(entity.getViewYRot(partialTick));
        return new OcclusionZone(pos.x, pos.z, (int) Math.floor(wakeHeight), Math.cos(rad), Math.sin(rad),
                paddedHalfWidth(dimensions), paddedHalfLength(dimensions));
    }

    // texel centers are checked to be within the zone resulting in half texels still being within
    // the zone. padding makes this go away.
    public static double paddedHalfWidth(OcclusionDimensions dimensions) {
        return dimensions.width() / 2.0 + 0.5 / WakeHandler.resolution.res;
    }

    public static double paddedHalfLength(OcclusionDimensions dimensions) {
        return dimensions.length() / 2.0 + 0.5 / WakeHandler.resolution.res;
    }

    /** Narrow phase: is this exact world point inside the zone's oriented rectangle? */
    public boolean contains(double worldX, double worldZ) {
        double dx = worldX - x, dz = worldZ - z;
        double localX = dx * cos + dz * sin;
        double localZ = -dx * sin + dz * cos;
        return Math.abs(localX) <= halfWidth && Math.abs(localZ) <= halfLength;
    }

    /** Broad phase: SAT test between this oriented rectangle and the axis-aligned 1x1 footprint
     *  of the wake node at (nodeX, nodeZ)..(nodeX+1, nodeZ+1). Only 4 candidate separating axes
     *  exist for two rectangles: the node's (trivial, world X/Z) and this zone's own two
     *  (cos,sin) and (-sin,cos). */
    public boolean overlapsNode(int nodeX, int nodeY, int nodeZ) {
        if (nodeY != y) return false;

        double nodeMinX = nodeX, nodeMaxX = nodeX + 1;
        double nodeMinZ = nodeZ, nodeMaxZ = nodeZ + 1;

        // Axis 1/2: world X and world Z. The zone's own AABB on these axes is the standard
        // "AABB of a rotated rectangle" bound (sum of half-extents' projections).
        double zoneExtentX = Math.abs(halfWidth * cos) + Math.abs(halfLength * sin);
        double zoneExtentZ = Math.abs(halfWidth * sin) + Math.abs(halfLength * cos);
        if (nodeMaxX < x - zoneExtentX || nodeMinX > x + zoneExtentX) return false;
        if (nodeMaxZ < z - zoneExtentZ || nodeMinZ > z + zoneExtentZ) return false;

        // Axis 3/4: the zone's own local axes. Project the node's 4 corners onto each axis;
        // the zone's own projection is trivially [-halfWidth, halfWidth] / [-halfLength, halfLength]
        // around its center's projection.
        double centerOnWidthAxis = x * cos + z * sin;
        double centerOnLengthAxis = -x * sin + z * cos;
        double nodeMinWidth = Double.POSITIVE_INFINITY, nodeMaxWidth = Double.NEGATIVE_INFINITY;
        double nodeMinLength = Double.POSITIVE_INFINITY, nodeMaxLength = Double.NEGATIVE_INFINITY;
        for (double cx : new double[]{nodeMinX, nodeMaxX}) {
            for (double cz : new double[]{nodeMinZ, nodeMaxZ}) {
                double onWidth = cx * cos + cz * sin;
                double onLength = -cx * sin + cz * cos;
                nodeMinWidth = Math.min(nodeMinWidth, onWidth);
                nodeMaxWidth = Math.max(nodeMaxWidth, onWidth);
                nodeMinLength = Math.min(nodeMinLength, onLength);
                nodeMaxLength = Math.max(nodeMaxLength, onLength);
            }
        }
        if (nodeMaxWidth < centerOnWidthAxis - halfWidth || nodeMinWidth > centerOnWidthAxis + halfWidth) return false;
        if (nodeMaxLength < centerOnLengthAxis - halfLength || nodeMinLength > centerOnLengthAxis + halfLength) return false;

        return true;
    }
}
