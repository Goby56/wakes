package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SplashPlaneRenderer implements ClientLifecycleEvents.ClientStarted {

    public static float[] samples;
    public static float[] normals;

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, int light) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(RenderType.getProgram());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//        RenderSystem.setShaderTexture(0, new Identifier("minecraft", "textures/block/water_flow.png"));
        RenderSystem.setShaderTexture(0, new Identifier("wakes", "icon.png"));
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // https://streamable.com/tz0gp
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        int normIndex = 0;
        for (int r = 0; r < res - 1; r++) {
            addVertex(buffer, matrix, light, 0, 0, 0, normIndex, 0, 0);
            addVertex(buffer, matrix, light, r * res * 3, normIndex, 1f / (res + 1), (float) r / res);
            addVertex(buffer, matrix, light, (r + 1) * res * 3, normIndex, 1f / (res + 1), (r + 1f) / res);
            normIndex += 3;
        }
        for (int i = 0; i < res * (res - 1) * 3 - 3; i += 3) {
            addVertex(buffer, matrix, light, i, normIndex);
            addVertex(buffer, matrix, light, i+3, normIndex);
            addVertex(buffer, matrix, light, i+res * 3, normIndex);
            normIndex += 1;

            addVertex(buffer, matrix, light, i+res*3, normIndex);
            addVertex(buffer, matrix, light, i+3, normIndex);
            addVertex(buffer, matrix, light, i+res*3+3, normIndex);
            normIndex += 1;
        }
        for (int r = 0; r < res - 1; r++) {
            addVertex(buffer, matrix, light, r * res * 3 + res * 3 - 3, normIndex, res / (res + 1f), (float) r / res);
            addVertex(buffer, matrix, light, 1f, 0, 1f, normIndex, 1f, 0f);
            addVertex(buffer, matrix, light, (r + 1) * res * 3 + res * 3 - 3, normIndex, res / (res + 1f), (r + 1f) / res);
            normIndex += 3;
        }

        RenderSystem.disableCull();
        Tessellator.getInstance().draw();
        RenderSystem.enableCull();
        matrices.pop();
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, int light, int index, int normIndex) {
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        addVertex(buffer, matrix, light, index, normIndex, (Math.floorMod(index / 3, res) + 1f) / (res + 1), (index / 3f) / (res * (res)));
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, int light, int index, int normIndex, float u, float v) {
        buffer.vertex(matrix,
                samples[index] * WakesClient.CONFIG_INSTANCE.splashPlaneWidth,
                samples[index + 1] * WakesClient.CONFIG_INSTANCE.splashPlaneHeight,
                samples[index + 2] * WakesClient.CONFIG_INSTANCE.splashPlaneDepth)
                .color(1f, 1f, 1f, 0.5f)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normals[normIndex], normals[normIndex + 1], normals[normIndex + 2])
                .next();
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, int light, float x, float y, float z, int normIndex, float u, float v) {
        buffer.vertex(matrix,
                        x * WakesClient.CONFIG_INSTANCE.splashPlaneWidth,
                        y * WakesClient.CONFIG_INSTANCE.splashPlaneHeight,
                        z * WakesClient.CONFIG_INSTANCE.splashPlaneDepth)
                .color(1f, 1f, 1f, 0.5f)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normals[normIndex], normals[normIndex + 1], normals[normIndex + 2])
                .next();

    }

    private static double sampleHeight(float x, float t) {
        float c = WakesClient.CONFIG_INSTANCE.c;
        return t * ((c - 2 - 2 * Math.sqrt(1 - c)) * x * x + (2 - 2 * c + 2 * Math.sqrt(1 - c)) * x + c);
    }

    private static double sampleDepth(float x, float t) {
        float k = WakesClient.CONFIG_INSTANCE.k;
        return Math.pow(Math.sqrt(x), (3 * t + 1) * k);
    }

    private static Vector3f samplesAsVector(int index) {
        return new Vector3f(samples[index], samples[index + 1], samples[index + 2]);
    }

    private static void calculateSamples() {
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        samples = new float[(res * res) * 3];
        for (float r = 0; r < res; r++) {
            float y = (res - 1 - r) / res;
            for (float c = 0; c < res; c++) {
                float x = (c + 1) / (res + 2);
                int index = (int) (r * res * 3 + c * 3);
                samples[index] = x;
                samples[index + 1] = (float) sampleHeight(x, y);
                samples[index + 2] = (float) sampleDepth(x, y);
            }
        }
    }

    private static void calculateNormals() {
        int res = WakesClient.CONFIG_INSTANCE.splashPlaneResolution;
        normals = new float[6 * res * (res - 1)];
        Vector3f normal;
        int index = 0;
        for (int r = 0; r < res - 1; r++) {
            Vector3f a = new Vector3f(0f, 0f, 0f);
            Vector3f b = samplesAsVector(r * res * 3);
            Vector3f c = samplesAsVector((r + 1) * res * 3);

            normal = (b.sub(a)).cross(c.sub(a)).normalize();
            normals[index] = normal.x;
            normals[index + 1] = normal.y;
            normals[index + 2] = normal.z;
            index += 3;
        }
        for (int i = 0; i < res * (res - 1) * 3 - 3; i += 9) {
            Vector3f a = samplesAsVector(i);
            Vector3f b = samplesAsVector(i + 3);
            Vector3f c = samplesAsVector(i + res * 3);

            normal = (b.sub(a)).cross(c.sub(a)).normalize();
            normals[index] = normal.x;
            normals[index + 1] = normal.y;
            normals[index + 2] = normal.z;
            index += 3;

            Vector3f d = samplesAsVector(i + res * 3);
            Vector3f e = samplesAsVector(i + 3);
            Vector3f f = samplesAsVector(i + res * 3 + 3);

            normal = (e.sub(d)).cross(f.sub(d)).normalize();
            normals[index] = normal.x;
            normals[index + 1] = normal.y;
            normals[index + 2] = normal.z;
            index += 3;
        }
        for (int r = 0; r < res - 1; r++) {
            Vector3f a = samplesAsVector(r * res * 3 + res * 3 - 3);
            Vector3f b = new Vector3f(1f, 0f, 1f);
            Vector3f c = samplesAsVector((r + 1) * res * 3 + res * 3 - 3);

            normal = (b.sub(a)).cross(c.sub(a)).normalize();
            normals[index] = normal.x;
            normals[index + 1] = normal.y;
            normals[index + 2] = normal.z;
            index += 3;
        }
    }

    public static void initSplashPlane() {
        calculateSamples();
        calculateNormals();
    }

    @Override
    public void onClientStarted(MinecraftClient client) {
        initSplashPlane();
    }
}
