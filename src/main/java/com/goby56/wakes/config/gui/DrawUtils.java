package com.goby56.wakes.config.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class DrawUtils {

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int w, int h) {
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        buffer.vertex(matrix, x, y, 5).texture(0, 0).color(0xFFFFFFFF);
        buffer.vertex(matrix, x, y + h, 5).texture(0, 1).color(0xFFFFFFFF);
        buffer.vertex(matrix, x + w, y + h, 5).texture(1, 1).color(0xFFFFFFFF);
        buffer.vertex(matrix, x + w, y, 5).texture(1, 0).color(0xFFFFFFFF);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

}
