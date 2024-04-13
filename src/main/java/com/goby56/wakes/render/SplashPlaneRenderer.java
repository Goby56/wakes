package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.render.enums.RenderType;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SplashPlaneRenderer implements ClientLifecycleEvents.ClientStarted {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3d> vertices;
    private static ArrayList<Vec3d> normals;
    private static final Texture tex = new Texture("textures/splash_plane_animation.png", 16, 2, 8);

    private static int ticks = 0;
    private static int animationFrame = 0;

    private static final double SQRT_8 = Math.sqrt(8);

    private static class Texture {
        public final int res;
        public final int width;
        public final int height;
        public final int outlineOffset;

        public final Identifier id;
        public Vec2f uvOffset = new Vec2f(0f, 0f);

        public Texture(String path, int resolution, int frames, int stages) {
            this.id = new Identifier("wakes", path);
            this.res = resolution;
            this.width = frames;
            this.height = stages * 2;
            this.outlineOffset = stages;
        }

        public void offsetPixels(int x, int y) {
            this.uvOffset = new Vec2f(x / (float) (width * res), y / (float) (height * res));
        }
    }

    public static void tick() {
        ticks++;
        if (ticks > 10) {
            animationFrame++;
            animationFrame %= 2;
            ticks = 0;
        }
    }

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, int light) {
        if (WakesClient.CONFIG_INSTANCE.disableMod || !WakesUtils.getEffectRuleFromSource(entity).renderPlanes) {
            return;
        }
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();

        matrices.push();
        float velocity = (float) Math.floor(((ProducesWake) entity).getHorizontalVelocity() * 20) / 20f;
        float progress = Math.min(1f, velocity / WakesClient.CONFIG_INSTANCE.maxSplashPlaneVelocity);
        float scalar = (float) (WakesClient.CONFIG_INSTANCE.splashPlaneScale * Math.sqrt(entity.getWidth() * Math.max(1f, progress) + 1) / 3f);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Vector3f color = new Vector3f();
        int waterCol = BiomeColors.getWaterColor(entity.getWorld(), entity.getBlockPos());
        color.x = (float) (waterCol >> 16 & 0xFF) / 255f;
        color.y = (float) (waterCol >> 8 & 0xFF) / 255f;
        color.z = (float) (waterCol & 0xFF) / 255f;

        RenderSystem.setShaderTexture(0, tex.id);
        tex.offsetPixels(animationFrame * tex.res, (int) (progress * (tex.outlineOffset - 1)) * tex.res);
        renderSurface(matrix, color, light);

        color.set(1f, 1f, 1f);
        tex.offsetPixels(animationFrame * tex.res, ((int) (progress * (tex.outlineOffset - 1)) + tex.outlineOffset) * tex.res);
        renderSurface(matrix, color, light);

        matrices.pop();
    }

    private static void renderSurface(Matrix4f matrix, Vector3f color, int light) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        // TODO IMPROVE ANIMATION (WATER TRAVELS IN AN OUTWARDS DIRECTION)
        // AND ADD A BOUNCY FEEL TO IT (BOBBING UP AND DOWN) WAIT IT IS JUST THE BOAT THAT IS DOING THAT
        // MAYBE ADD TO BLAZINGLY FAST BOATS?
        // https://streamable.com/tz0gp
        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size(); i++) {
                Vec3d vertex = vertices.get(i);
                Vec3d normal = normals.get(i);
                buffer.vertex(matrix,
                                (float) (s * vertex.x * WakesClient.CONFIG_INSTANCE.splashPlaneWidth),
                                (float) (vertex.z * WakesClient.CONFIG_INSTANCE.splashPlaneHeight),
                                (float) (vertex.y * WakesClient.CONFIG_INSTANCE.splashPlaneDepth))
                        .color(color.x, color.y, color.z, WakesClient.CONFIG_INSTANCE.wakeOpacity)
                        .texture((float) (vertex.x / tex.width + tex.uvOffset.x), (float) (vertex.y / tex.height + tex.uvOffset.y))
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(light)
                        .normal((float) normal.x, (float) normal.y, (float) normal.z)
                        .next();
            }
        }

        RenderSystem.disableCull();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
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
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
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

    @Override
    public void onClientStarted(MinecraftClient client) {
        initSplashPlane();
    }
}
