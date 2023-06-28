package com.goby56.wakes.render;

import com.goby56.wakes.utils.WakeHandler;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.util.ArrayList;

public class WakeTextureRenderer implements WorldRenderEvents.AfterTranslucent {

    private static void renderTexture(Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.enableDepthTest();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x0, y0, z0).texture(0, 0).next();
        buffer.vertex(matrix, x0, (y0+y1)/2, z1).texture(0, 1).next();
        buffer.vertex(matrix, x1, y1, z1).texture(1, 1).next();
        buffer.vertex(matrix, x1, (y0+y1)/2, z0).texture(1, 0).next();
        Tessellator.getInstance().draw();
        // TODO DRAW TEXTURE ON UNDER SIDE MAYBE
    }

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        ArrayList<WakeNode> nodes = WakeHandler.getInstance().getVisible(context.frustum());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        RenderSystem.enableBlend();

        for (WakeNode node : nodes) {
            Vec3d pos = node.getPos().add(context.camera().getPos().negate());

            int[] rgba = {0, 0, 0, 100};
            int val;
            // TODO FIX GL TRANSPARENCY / OPACITY
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    rgba[3] = 0;
                    for (int i = 0; i < 3; i++) {
                        val = (int) MathHelper.clamp(node.u[i][z+1][x+1], 0, 255);
                        rgba[i] = val;
                        rgba[3] += val;
                    }
                    rgba[3] /= 3;
                    MemoryUtil.memPutInt(wakeHandler.imagePointer + (z*16+x)*4, WakesUtils.rgbaArr2abgrInt(rgba));
//                    MemoryUtil.memPutInt(wakeHandler.imagePointer + (z*16+x)*4, 0xFF << 8 * 3 | (int) node.u[0][z+1][x+1]);
                }
            }

            GlStateManager._bindTexture(wakeHandler.glTexId);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
            GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, 16, 16, GlConst.GL_RGBA, GlConst.GL_UNSIGNED_BYTE, wakeHandler.imagePointer);

            RenderSystem.setShaderTexture(0, wakeHandler.glTexId);

            renderTexture(matrix, (float) pos.x, (float) pos.y, (float) pos.z, (float) (pos.x + 1), (float) pos.y, (float) (pos.z + 1));
        }
    }
}
