package com.goby56.wakes.render;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class SplashPlaneRenderer implements WorldRenderEvents.AfterTranslucent {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3d> vertices;
    private static ArrayList<Vec3d> normals;

    public static Map<Resolution, WakeTexture> wakeTextures = null;

    private static void initTextures() {
        wakeTextures = Map.of(
                Resolution.EIGHT, new WakeTexture(Resolution.EIGHT.res, false),
                Resolution.SIXTEEN, new WakeTexture(Resolution.SIXTEEN.res, false),
                Resolution.THIRTYTWO, new WakeTexture(Resolution.THIRTYTWO.res, false)
        );
    }

    private static final double SQRT_8 = Math.sqrt(8);

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        if (WakeHandler.getInstance().isEmpty()) {
            return;
        }
        WakeHandler wakeHandler = WakeHandler.getInstance().get();
        for (SplashPlaneParticle particle : wakeHandler.getVisible(context.frustum(), SplashPlaneParticle.class)) {
            if (particle.isRenderReady) {
                SplashPlaneRenderer.render(particle.owner, particle, context, context.matrixStack());
            }
        }
    }


    public static <T extends Entity> void render(T entity, SplashPlaneParticle splashPlane, WorldRenderContext context, MatrixStack matrices) {
        if (wakeTextures == null) initTextures();
        if (WakesConfig.disableMod || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }

        matrices.push();
        splashPlane.translateMatrix(context, matrices);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(splashPlane.lerpedYaw + 180f));
        float velocity = (float) Math.floor(((ProducesWake) entity).wakes$getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesConfig.maxSplashPlaneVelocity);
        float scalar = (float) (WakesConfig.splashPlaneScale * Math.sqrt(entity.getWidth() * Math.max(1f, progress) + 1) / 3f);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        wakeTextures.get(WakeHandler.resolution).loadTexture(splashPlane.pixels);
        renderSurface(matrix, wakeTextures.get(WakeHandler.resolution));

        matrices.pop();
    }

    private static void renderSurface(Matrix4f matrix, WakeTexture texture) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
        // TODO IMPROVE ANIMATION (WATER TRAVELS IN AN OUTWARDS DIRECTION)
        // AND ADD A BOUNCY FEEL TO IT (BOBBING UP AND DOWN) WAIT IT IS JUST THE BOAT THAT IS DOING THAT
        // MAYBE ADD TO BLAZINGLY FAST BOATS?
        // https://streamable.com/tz0gp
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i++) {
                Vec3d vertex = vertices.get(i);
                Vec3d normal = normals.get(i);
                bufferBuilder.vertex(matrix,
                                (float) (s * (vertex.x * WakesConfig.splashPlaneWidth + WakesConfig.splashPlaneGap)),
                                (float) (vertex.z * WakesConfig.splashPlaneHeight),
                                (float) (vertex.y * WakesConfig.splashPlaneDepth))
                        .texture((float) (vertex.x), (float) (vertex.y))
                        .color(1f, 1f, 1f, 1f)
                        .normal((float) normal.x, (float) normal.y, (float) normal.z);
            }
        }

        BuiltBuffer builtBuffer = bufferBuilder.end();
        var buffer = RenderSystem.getDevice().createBuffer(() -> "Wakes wake quad buffer", BufferType.VERTICES, BufferUsage.STATIC_READ, builtBuffer.getBuffer());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(texture.texture, OptionalInt.of(0xffffffff))) {
            pass.setPipeline(RenderPipelines.TRANSLUCENT);
            pass.setVertexBuffer(0, buffer);
            pass.draw(0, buffer.size);
        }
    }

    private static double upperBound(double x) {
        return - 2 * x * x + SQRT_8 * x;
    }

    private static double lowerBound(double x) {
        return (SQRT_8 - 2) * x * x;
    }

    private static double height(double x, double y) {
        return 4 * (x * (SQRT_8 - x) -y - x * x) / SQRT_8;
    }

    private static Vec3d normal(double x, double y) {
        double nx = SQRT_8 / (4 * (4 * x + y - SQRT_8));
        double ny = SQRT_8 / (4 * (2 * x * x - SQRT_8 + 1));
        return Vec3d.fromPolar((float) Math.tan(nx), (float) Math.tan(ny));
    }

    private static void distributePoints() {
        int res = WakesConfig.splashPlaneResolution;
        points = new ArrayList<>();

        for (float i = 0; i < res; i++) {
            double x = i / (res - 1);
            double h = upperBound(x) - lowerBound(x);
            int n_points = (int) Math.max(1, Math.floor(h * res));
            for (float j = 0; j < n_points + 1; j++) {
                float y = (float) ((j / n_points) * h + lowerBound(x));
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
                double x = vec.x, y = vec.y;
                vertices.add(new Vec3d(x, y, height(x, y)));
                normals.add(normal(x, y));
            }
        }
    }

    public static void initSplashPlane() {
        distributePoints();
        generateMesh();
    }
}
