package com.goby56.wakes.render;

import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterEntities {
    @Override
    public void afterEntities(WorldRenderContext context) {
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getNearby(context.camera().getPos());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());
            renderTexture(Identifier.of("minecraft", "textures/block/andesite.png"), matrix, (float) pos.x, (float) pos.y, (float) pos.z, (float) (pos.x + 1), (float) pos.y, (float) (pos.z + 1));
        }
    }

    private static void renderTexture(Identifier texture, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.enableDepthTest();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x0, y0, z0).texture(0, 0).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).texture(0, 1).next();
        buffer.vertex(matrix, x1, y1, z1).texture(1, 1).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).texture(1, 0).next();
        Tessellator.getInstance().draw();
    }
}
