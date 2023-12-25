package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class SplashPlaneRenderer implements ClientLifecycleEvents.ClientStarted {

    private static ArrayList<Vector2D> points;
    private static List<Triangle2D> triangles;
    private static ArrayList<Vec3d> vertices;
    private static ArrayList<Vec3d> normals;
    private static ArrayList<Vec2f> uvs;

    private static final double SQRT_8 = Math.sqrt(8);

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, int light) {

        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
//        RenderSystem.setShaderTexture(0, new Identifier("minecraft", "textures/block/water_flow.png"));

        matrices.push();
        float width = entity.getWidth();
        float velocity = (float) Math.floor(entity.getVelocity().horizontalLength() * 20) / 20f;
        float scalar = (float) Math.log(width * velocity + 1);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float r, g, b;
        int waterCol = BiomeColors.getWaterColor(entity.getWorld(), entity.getBlockPos());
        if (WakesClient.CONFIG_INSTANCE.useWaterBlending) {
            r = (float) (waterCol >> 16 & 0xFF) / 255f;
            g = (float) (waterCol >> 8 & 0xFF) / 255f;
            b = (float) (waterCol & 0xFF) / 255f;
        } else {
            r = 1f;
            g = 1f;
            b = 1f;
        }

        RenderSystem.setShaderTexture(0, new Identifier("wakes", "textures/splash_plane.png"));
        renderSurface(matrix, r, g, b, light);
        RenderSystem.setShaderTexture(0, new Identifier("wakes", "textures/splash_plane_outline.png"));
        renderSurface(matrix, 1f, 1f, 1f, light);

        matrices.pop();
    }

    private static void renderSurface(Matrix4f matrix, float r, float g, float b, int light) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

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
                        .color(r, g, b, 1f)
                        .texture((float) vertex.x, (float) vertex.y)
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
        uvs = new ArrayList<>();
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
                uvs.add(new Vec2f((float) x, x == 0 ? 0 : (float) ((y - lowerBound(x)) / (upperBound(x) - lowerBound(x)))));
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
