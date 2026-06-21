package com.goby56.wakes.render;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.debug.WakesDebugInfo;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.vertex.*;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class SplashPlaneRenderer implements LevelRenderEvents.AfterTranslucentTerrain, LevelRenderEvents.BeforeTranslucentTerrain {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3> vertices;
    private static ArrayList<Vec3> normals;

    private static final double SQRT_8 = Math.sqrt(8);

    private static boolean cameraSubmerged(LevelRenderContext context) {
        return context.gameRenderer().getMainCamera().getFluidInCamera()
                == net.minecraft.world.level.material.FogType.WATER;
    }

    // Splash planes are 3-D sheets rising above the water. Normally render them after the
    // translucent water (unchanged behavior above water). But when the camera is underwater,
    // the water surface would occlude them, so render BEFORE translucent water instead: opaque
    // terrain (already drawn) still occludes them correctly, while the water draws afterwards
    // and, being translucent, lets the splash plane show through from below.
    @Override
    public void afterTranslucentTerrain(LevelRenderContext context) {
        if (cameraSubmerged(context)) return;
        renderAll(context);
    }

    @Override
    public void beforeTranslucentTerrain(LevelRenderContext context) {
        if (!cameraSubmerged(context)) return;
        renderAll(context);
    }

    private void renderAll(LevelRenderContext context) {
        if (WakeHandler.getInstance().isEmpty()) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        List<SplashPlaneParticle> planes = wakeHandler.getVisibleSplashPlanes();
        if (planes.isEmpty()) return;

        wakeHandler.getTextureAtlas().dynamicTexture.uploadIfDirty();

        MultiBufferSource.BufferSource bufferSource = context.bufferSource();
        RenderType type = RenderTypes.entityTranslucent(WakeTextureAtlas.ATLAS_ID, false);
        VertexConsumer vc = bufferSource.getBuffer(type);

        for (SplashPlaneParticle particle : planes) {
            SplashPlaneRenderer.render(particle.owner, particle, context, new PoseStack(), vc);
        }
        bufferSource.endBatch(type);
        WakesDebugInfo.splashPlanes = planes.size();
    }


    public static <T extends Entity> void render(T entity, SplashPlaneParticle splashPlane, LevelRenderContext context, PoseStack matrices, VertexConsumer vc) {
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                !WakesConfig.firstPersonEffects &&
                splashPlane.owner instanceof LocalPlayer) {
            return;
        }
        if (splashPlane.drawContext == null) return;

        splashPlane.updateYaw(context.gameRenderer().getMainCamera().getCameraEntityPartialTicks(net.minecraft.client.Minecraft.getInstance().getDeltaTracker()));

        matrices.pushPose();
        splashPlane.translateMatrix(context.gameRenderer().getMainCamera(), matrices);
        matrices.mulPose(Axis.YP.rotationDegrees(splashPlane.lerpedYaw + 180f));
        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesConfig.maxSplashPlaneVelocity);
        float scalar = (float) (WakesConfig.splashPlaneScale * Math.sqrt(entity.getBbWidth() * Math.max(1f, progress) + 1) / 3f);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.last().pose();
        matrices.popPose();

        ClientLevel world = Minecraft.getInstance().level;
        int light = LevelRenderer.getLightCoords(world, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()));
        renderSurface(matrix, splashPlane.drawContext, vc, light);
    }

    private static void renderSurface(Matrix4f matrix, WakeTextureAtlas.DrawContext drawContext, VertexConsumer vc, int light) {
        UVPair uv = drawContext.getUV();
        float uvOffset = drawContext.getUVOffset();
        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i += 3) {
                Vec3 v0 = vertices.get(i);
                Vec3 n0 = normals.get(i);
                Vec3 v1 = vertices.get(i + 1);
                Vec3 n1 = normals.get(i + 1);
                Vec3 v2 = vertices.get(i + 2);
                Vec3 n2 = normals.get(i + 2);
                addDegenerateQuad(vc, matrix, s, v0, n0, v1, n1, v2, n2, uv, uvOffset, light);
                addDegenerateQuad(vc, matrix, s, v0, n0, v2, n2, v1, n1, uv, uvOffset, light);
            }
        }
    }

    private static void addVertex(VertexConsumer vc, Matrix4f matrix, int side, Vec3 vertex, Vec3 normal, UVPair uv, float uvOffset, int light) {
        vc.addVertex(matrix,
                        (float) (side * (vertex.x * WakesConfig.splashPlaneWidth + WakesConfig.splashPlaneGap)),
                        (float) (vertex.z * WakesConfig.splashPlaneHeight),
                        (float) (vertex.y * WakesConfig.splashPlaneDepth))
                .setColor(1f, 1f, 1f, 1f)
                .setUv(uv.u() + (float) vertex.x * uvOffset, uv.v() + (float) vertex.y * uvOffset)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void addDegenerateQuad(VertexConsumer vc, Matrix4f matrix, int side, Vec3 a, Vec3 an, Vec3 b, Vec3 bn, Vec3 c, Vec3 cn, UVPair uv, float uvOffset, int light) {
        addVertex(vc, matrix, side, a, an, uv, uvOffset, light);
        addVertex(vc, matrix, side, b, bn, uv, uvOffset, light);
        addVertex(vc, matrix, side, c, cn, uv, uvOffset, light);
        addVertex(vc, matrix, side, c, cn, uv, uvOffset, light);
    }

    private static double upperBound(double x) {
        return - 2 * x * x + SQRT_8 * x;
    }

    private static double lowerBound(double x) {
        return (SQRT_8 - 2) * x * x;
    }

    private static double height(double x, double y) {
        return 4 * (x * (SQRT_8 - x) - y - x * x) / SQRT_8;
    }

    private static Vec3 normal(double x, double y) {
        double nx = SQRT_8 / (4 * (4 * x + y - SQRT_8));
        double ny = SQRT_8 / (4 * (2 * x * x - SQRT_8 + 1));
        return Vec3.directionFromRotation((float) Math.tan(nx), (float) Math.tan(ny));
    }

    private static void distributePoints() {
        int res = WakesConfig.splashPlaneResolution;
        points = new ArrayList<>();

        for (float i = 0; i < res; i++) {
            double x = i / (res - 1);
            double h = upperBound(x) - lowerBound(x);
            int nPoints = (int) Math.max(1, Math.floor(h * res));
            for (float j = 0; j < nPoints + 1; j++) {
                float y = (float) ((j / nPoints) * h + lowerBound(x));
                points.add(new Vector2D(x, y));
            }
        }
    }

    private static void generateMesh() {
        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        try {
            DelaunayTriangulator delaunay = new DelaunayTriangulator(points);
            delaunay.triangulate();
            triangles = delaunay.getTriangles();
        } catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }
        for (Triangle2D tri : triangles) {
            for (Vector2D vec : new Vector2D[] {tri.a, tri.b, tri.c}) {
                double x = vec.x;
                double y = vec.y;
                vertices.add(new Vec3(x, y, height(x, y)));
                normals.add(normal(x, y));
            }
        }
    }

    public static void initSplashPlane() {
        distributePoints();
        generateMesh();
    }
}
