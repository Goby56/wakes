package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

public class SplashPlaneRenderer implements ClientLifecycleEvents.ClientStarted {

    public static int resolution = 5;

    public static float c = 0.5f;
    public static float k = 1f;

    public static float[] samples;

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, int light) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
//        RenderSystem.setShader(GameRenderer::getRenderTypeEntitySolidProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        RenderSystem.setShaderTexture(0, new Identifier("wakes", "icon.png"));
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);
//        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // https://streamable.com/tz0gp
        for (int r = 0; r < resolution - 1; r++) {
            addVertex(buffer, matrix, 0, 0, 0, 0, 0);
            addVertex(buffer, matrix, r * resolution * 3, 0, 1);
            addVertex(buffer, matrix, (r + 1) * resolution * 3, 1, 0);
        }
        for (int i = 0; i < resolution * (resolution - 1) * 3 - 3; i += 3) {
            addVertex(buffer, matrix, i, 0, 0);
            addVertex(buffer, matrix, i+3, 0, 1);
            addVertex(buffer, matrix, i+resolution * 3, 1, 0);

            addVertex(buffer, matrix, i+resolution*3, 1, 0);
            addVertex(buffer, matrix, i+3, 0, 1);
            addVertex(buffer, matrix, i+resolution*3+3, 1, 1);
        }
        for (int r = 0; r < resolution - 1; r++) {
            addVertex(buffer, matrix, r * resolution * 3 + resolution * 3 - 3, 0, 0);
            addVertex(buffer, matrix, 1f, 0, 1f, 0, 1);
            addVertex(buffer, matrix, (r + 1) * resolution * 3 + resolution * 3 - 3, 1, 0);
        }

        Tessellator.getInstance().draw();
        matrices.pop();
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, int index, int u, int v) {
        float p = index / (resolution * resolution * 3f);
        buffer.vertex(matrix,
                samples[index] * WakesClient.CONFIG_INSTANCE.splashPlaneWidth,
                samples[index + 1] * WakesClient.CONFIG_INSTANCE.splashPlaneHeight,
                samples[index + 2] * WakesClient.CONFIG_INSTANCE.splashPlaneDepth)
                .color(p, 1 - p, 0f, 1f)
                .texture(u, v)
                .next();
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int u, int v) {
        buffer.vertex(matrix,
                        x * WakesClient.CONFIG_INSTANCE.splashPlaneWidth,
                        y * WakesClient.CONFIG_INSTANCE.splashPlaneHeight,
                        z * WakesClient.CONFIG_INSTANCE.splashPlaneDepth)
                .color(0f, 0f, 1f, 1f)
                .texture(u, v)
                .next();

    }

    private static void calculateSamples() {
        samples = new float[(resolution * resolution) * 3];
        for (float r = 0; r < resolution; r++) {
            float y = (resolution - 1 - r) / resolution;
            for (float c = 0; c < resolution; c++) {
                float x = (c + 1) / (resolution + 2);
                int index = (int) (r * resolution * 3 + c * 3);
                samples[index] = x;
                samples[index + 1] = (float) sampleHeight(x, y);
                samples[index + 2] = (float) sampleDepth(x, y);
            }
        }
    }

    private static double sampleHeight(float x, float t) {
        return t * ((c - 2 - 2 * Math.sqrt(1 - c)) * x * x + (2 - 2 * c + 2 * Math.sqrt(1 - c)) * x + c);
    }

    private static double sampleDepth(float x, float t) {
        return Math.pow(Math.sqrt(x), (3 * t + 1) * k);
    }

    @Override
    public void onClientStarted(MinecraftClient client) {
        SplashPlaneRenderer.calculateSamples();
    }
}
