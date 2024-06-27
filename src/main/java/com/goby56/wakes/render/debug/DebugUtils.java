package com.goby56.wakes.render.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;

import java.util.Arrays;

public class DebugUtils {
    public static Vector4i rgba = new Vector4i(255, 0, 255, 255);

    private static void addVertex(Vec3d pos, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex((float) pos.x, (float) pos.y, (float) pos.z).color(rgba.x, rgba.y, rgba.z, rgba.w);
    }

    private static void drawLines(Vec3d[] vertices, Vec3d renderContext, boolean connect) {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (Vec3d vertex : vertices) {
            Vec3d v = vertex.subtract(renderContext);
            addVertex(v, bufBuilder);
        }
        if (connect) {
            Vec3d v = vertices[0].subtract(renderContext);
            addVertex(v, bufBuilder);
        }
        BufferRenderer.drawWithGlobalProgram(bufBuilder.end());
    }

    public static void drawLine(Vec3d from, Vec3d to, Vec3d renderContext) {
        Vec3d[] vertices = {from, to};
        drawLines(vertices, renderContext);
    }

    public static void drawLines(Vec3d[] vertices, Vec3d renderContext) {
        drawLines(vertices, renderContext, false);
    }

    public static void drawPoly(Vec3d[] vertices, Vec3d renderContext) {
        drawLines(vertices, renderContext, true);
    }
}
