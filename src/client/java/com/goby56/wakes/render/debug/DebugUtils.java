package com.goby56.wakes.render.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;

import java.util.Arrays;

public class DebugUtils {
    public static Vector4i rgba = new Vector4i(255, 0, 255, 255);

    private static void addVertex(Vec3d pos, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(pos.x, pos.y, pos.z).color(rgba.x, rgba.y, rgba.z, rgba.w).next();
    }

    private static void drawLines(Vec3d[] vertices, Vec3d renderContext, boolean connect) {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

//        System.out.println(Arrays.toString(vertices));

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (Vec3d vertex : vertices) {
//            System.out.printf("adding vertex: %f, %f, %f   ", vertex.x, vertex.y, vertex.z);
            Vec3d v = vertex.subtract(renderContext);
            addVertex(v, bufferBuilder);
        }
        if (connect) {
            Vec3d v = vertices[0].subtract(renderContext);
            addVertex(v, bufferBuilder);
        }
        tessellator.draw();
    }

    public static void drawCube(Vec3d pos, float size, Vec3d renderContext) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        Vec3d vertex = pos.subtract(renderContext);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int x = -1; x <= 1; x++) {
           for (int y = -1; y <= 1; y++) {
               for (int z = -1; z <= 1; z++) {
                   Vec3d v = vertex.add(x, y, z).multiply(size);
                   addVertex(v, bufferBuilder);
               }
           }
        }
        tessellator.draw();
    }

    public static void drawTriangle(Vec3d[] vertices, Vec3d renderContext) {

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
