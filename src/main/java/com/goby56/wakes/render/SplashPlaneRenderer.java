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
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SplashPlaneRenderer implements ClientLifecycleEvents.ClientStarted {

    private static ArrayList<ArrayList<Vertex>> vertices;
    private static Color renderingColor = new Color();
    private static final double SQRT_8 = Math.sqrt(8);

    private static boolean DEBUG = false;

    private static class Vertex {
        public final Vector3f pos;
        public final Vector3f normal;

        public Vertex(double x, double y) {
            this.pos = new Vector3f((float) x, (float) y, (float) height(x, y));
            this.normal = normal(x, y);
        }
    }

    private static class Color {
        public float r;
        public float g;
        public float b;

        public Color(int color) {
            this.r = (float) (color >> 16 & 0xFF) / 255f;
            this.g = (float) (color >> 8 & 0xFF) / 255f;
            this.b = (float) (color & 0xFF) / 255f;
        }

        public Color() {
            this.reset();
        }

        public void reset() {
            this.r = 1f; this.g = 1f; this.b = 1f;
        }
    }

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, int light) {
        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();

        matrices.push();
        float width = entity.getWidth();
        float velocity = (float) Math.floor(entity.getVelocity().horizontalLength() * 20) / 20f;
        float scalar = (float) Math.log(width * velocity + 1);
        matrices.scale(scalar, scalar, scalar);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        if (WakesClient.CONFIG_INSTANCE.useWaterBlending) {
            renderingColor = new Color(BiomeColors.getWaterColor(entity.getWorld(), entity.getBlockPos()));
        } else {
            renderingColor.reset();
        }

        RenderSystem.setShaderTexture(0, new Identifier("wakes", "textures/splash_plane.png"));
        renderSurface(matrix, 2 * velocity, light);
//        renderingColor.reset();
//        RenderSystem.setShaderTexture(0, new Identifier("wakes", "textures/splash_plane_outline.png"));
//        renderSurface(matrix, velocity, light);

        matrices.pop();
    }

    private static void renderSurface(Matrix4f matrix, float progress, int light) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        // https://streamable.com/tz0gp
        for (int s = -1; s < 2; s++) {
            if (s == 0) continue;
            for (int i = 0; i < vertices.size() - 1; i++) {
                ArrayList<Vertex> column1 = vertices.get(i);
                ArrayList<Vertex> column2 = vertices.get(i+1);
                for (int j = 0; j < Math.min(column1.size(), column2.size()) - 1; j++) {
//                for (int j = 0; j < column1.size(); j++) {
//                    addVertex(buffer, matrix, column1.get(j), s, light, 0, 0);
                    addVertex(buffer, matrix, column1.get(j), s, light, 0, 1);
                    addVertex(buffer, matrix, column1.get(j + 1), s, light, 0, 0);
                    addVertex(buffer, matrix, column2.get(j + 1), s, light, 1, 0);
                    addVertex(buffer, matrix, column2.get(j), s, light, 1, 1);
                }
            }
        }

        RenderSystem.disableCull();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vertex vertex, int sign, int light, float u, float v) {
        buffer.vertex(matrix,
                        sign * vertex.pos.x * WakesClient.CONFIG_INSTANCE.splashPlaneWidth,
                        vertex.pos.z * WakesClient.CONFIG_INSTANCE.splashPlaneHeight,
                        vertex.pos.y * WakesClient.CONFIG_INSTANCE.splashPlaneDepth)
                .color(renderingColor.r, renderingColor.g, renderingColor.b, 1f)
                .texture(vertex.pos.x, vertex.pos.y)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(vertex.normal.x, vertex.normal.y, vertex.normal.z)
                .next();
    }

    private static double upperBound(double x) {
        return - 2 * x * x + SQRT_8 * x;
    }

    private static double lowerBound(double x) {
        return (SQRT_8 - 2) * x * x;
    }

    private static double height(double x, double y) {
        float res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        return Math.floor(res * 4 * (x * (SQRT_8 - x) - y - x * x) / SQRT_8) / res;
    }

    private static Vector3f normal(double x, double y) {
        double nx = SQRT_8 / (4 * (4 * x + y - SQRT_8));
        double ny = SQRT_8 / (4 * (2 * x * x - SQRT_8 + 1));
        return Vec3d.fromPolar((float) Math.tan(nx), (float) Math.tan(ny)).toVector3f();
    }

    private static void distributePoints() {
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        vertices = new ArrayList<>();

        for (float i = 0; i < res; i++) {
            vertices.add(new ArrayList<>());
            double x = i / (res - 1);
            double h = upperBound(x) - lowerBound(x);
            int n_points = (int) Math.max(1, Math.floor(h * res));
            if (n_points == 1) continue;
            for (float j = n_points; j >= 0; j--) {
                float y = (float) ((j / n_points) * h + lowerBound(x));
                vertices.get((int) i).add(new Vertex(x, y));
            }
        }
    }

    public static void initSplashPlane() {
        distributePoints();
    }

    @Override
    public void onClientStarted(MinecraftClient client) {
        initSplashPlane();
    }
}
