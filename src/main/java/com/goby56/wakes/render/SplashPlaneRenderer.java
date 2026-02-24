package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.*;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class SplashPlaneRenderer implements WorldRenderEvents.EndMain {
    private static final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.BIG_BUFFER_SIZE);

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3> vertices;
    private static ArrayList<Vec3> normals;

    public static Map<Resolution, SplashPlaneTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new SplashPlaneTexture(Resolution.EIGHT.res),
                Resolution.SIXTEEN, new SplashPlaneTexture(Resolution.SIXTEEN.res),
                Resolution.THIRTYTWO, new SplashPlaneTexture(Resolution.THIRTYTWO.res)
        );
    }

    private static final double SQRT_8 = Math.sqrt(8);

    private static RenderPipeline getPipeline() {
        if (WakesClient.areShadersEnabled) {
            return RenderPipelines.TRANSLUCENT_MOVING_BLOCK;
        } else {
            return RenderPipelines.BEACON_BEAM_TRANSLUCENT;
        }
    }

    @Override
    public void endMain(WorldRenderContext context) {
        if (WakeHandler.getInstance().isEmpty()) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        for (SplashPlaneParticle particle : wakeHandler.getVisibleSplashPlanes()) {
            SplashPlaneRenderer.render(particle.owner, particle, context, context.matrices());
        }
    }


    public static <T extends Entity> void render(T entity, SplashPlaneParticle splashPlane, WorldRenderContext context, PoseStack matrices) {
        if (wakeTextures == null) initTextures();
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() &&
                !WakesConfig.firstPersonEffects &&
                splashPlane.owner instanceof LocalPlayer) {
            return;
        }
        splashPlane.updateYaw(context.gameRenderer().getMainCamera().getPartialTickTime());

        matrices.pushPose();
        splashPlane.translateMatrix(context.gameRenderer().getMainCamera(), matrices);
        matrices.mulPose(Axis.YP.rotationDegrees(splashPlane.lerpedYaw + 180f));
        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesConfig.maxSplashPlaneVelocity);
        float scalar = (float) (WakesConfig.splashPlaneScale * Math.sqrt(entity.getBbWidth() * Math.max(1f, progress) + 1) / 3f);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.last().pose();
        matrices.popPose();

        SplashPlaneTexture texture = wakeTextures.get(WakeHandler.resolution);
        if (texture.resolution != splashPlane.image.getWidth()) {
            return;
        }
        texture.loadTexture(splashPlane.image);
        renderSurface(matrix, texture);
    }

    private static void renderSurface(Matrix4f matrix, SplashPlaneTexture splashTexture) {
        RenderPipeline pipeline = getPipeline();
        BufferBuilder bb = Tesselator.getInstance().begin(pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i += 3) {
                Vec3 v0 = vertices.get(i);
                Vec3 n0 = normals.get(i);
                Vec3 v1 = vertices.get(i + 1);
                Vec3 n1 = normals.get(i + 1);
                Vec3 v2 = vertices.get(i + 2);
                Vec3 n2 = normals.get(i + 2);
                addDegenerateQuad(bb, matrix, s, v0, n0, v1, n1, v2, n2);
                addDegenerateQuad(bb, matrix, s, v0, n0, v2, n2, v1, n1);
            }
        }

        MeshData built = bb.buildOrThrow();
        MeshData.DrawState drawState = built.drawState();
        VertexFormat format = drawState.format();
        GpuBuffer vertexBuffer = format.uploadImmediateVertexBuffer(built.vertexBuffer());
        built.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
        GpuBuffer indexBuffer = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());

        GpuSampler sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Splash Plane",
                Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("Sampler0", splashTexture.getTextureView(), sampler);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, drawState.indexType());
            pass.drawIndexed(0 / format.getVertexSize(), 0, drawState.indexCount(), 1);
        }
        built.close();
    }

    private static void addVertex(BufferBuilder bb, Matrix4f matrix, int side, Vec3 vertex, Vec3 normal) {
        bb.addVertex(matrix,
                        (float) (side * (vertex.x * WakesConfig.splashPlaneWidth + WakesConfig.splashPlaneGap)),
                        (float) (vertex.z * WakesConfig.splashPlaneHeight),
                        (float) (vertex.y * WakesConfig.splashPlaneDepth))
                .setUv((float) vertex.x, (float) vertex.y)
                .setLight(LightTexture.FULL_BRIGHT)
                .setColor(1f, 1f, 1f, 1f)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void addDegenerateQuad(BufferBuilder bb, Matrix4f matrix, int side, Vec3 a, Vec3 an, Vec3 b, Vec3 bn, Vec3 c, Vec3 cn) {
        addVertex(bb, matrix, side, a, an);
        addVertex(bb, matrix, side, b, bn);
        addVertex(bb, matrix, side, c, cn);
        addVertex(bb, matrix, side, c, cn);
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
