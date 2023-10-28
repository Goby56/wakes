package com.goby56.wakes.render;

import com.goby56.wakes.render.debug.DebugUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;

public class SplashPlaneRenderer implements ClientLifecycleEvents.ClientStarted {

    public static int resolution = 5;
    public static float height = 1f;
    public static float width = 1.5f;
    public static float depth = 1f;

    public static float c = 0.5f;
    public static float k = 1f;

    public static float[] samples;

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
//        RenderSystem.setShader(GameRenderer::getRenderTypeEntitySolidProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

//        Vector3f origin = entity.getPos().toVector3f();
        Vector3f origin = new Vector3f(0,0,0);

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < (resolution - 1) * (resolution - 1) * 3; i += 3) {
            addVertex(origin, buffer, matrix, i);
            addVertex(origin, buffer, matrix, i + 3);
            addVertex(origin, buffer, matrix, i + resolution * 3);
            addVertex(origin, buffer, matrix, i + resolution * 3 + 3);
        }

        Tessellator.getInstance().draw();
        matrices.pop();
    }

    private static void addVertex(Vector3f origin, BufferBuilder buffer, Matrix4f matrix, int index) {
        buffer.vertex(matrix,
                samples[index] + origin.x,
                samples[index + 1] + origin.y,
                samples[index + 2] + origin.z)
                .color(1f, 0f, 0f, 1f)
                .next();
    }

    private static void calculateSamples() {
        samples = new float[resolution*resolution*3];
        for (float i = 0; i < resolution; i++) {
            float x = i / resolution;
            for (float j = 0; j < resolution; j++) {
                float y = j / resolution;
                int index = (int) (i * resolution + j);
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
        return Math.pow(Math.sqrt(x), k);
    }

    @Override
    public void onClientStarted(MinecraftClient client) {
        SplashPlaneRenderer.calculateSamples();
    }
}
