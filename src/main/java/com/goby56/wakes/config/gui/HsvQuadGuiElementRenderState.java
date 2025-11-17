package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.render.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record HsvQuadGuiElementRenderState(
        Matrix3x2f pose,
        int x, int y, int width, int height,
        int color1, int color2, int color3, int color4,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public HsvQuadGuiElementRenderState(
            Matrix3x2f pose,
            int x, int y, int width, int height,
            int color1, int color2, int color3, int color4,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pose, x, y, width, height, color1, color2, color3, color4, scissorArea, createBounds(x, y, x + width, y + height, pose, scissorArea));
    }

    @Nullable
    private static ScreenRectangle createBounds(int x1, int y1, int x2, int y2, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle screenRect = new ScreenRectangle(x1, y1, x2 - x1, y2 - y1).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }

    @Override
    public void buildVertices(VertexConsumer vertices, float depth) {
        vertices.addVertexWith2DPose(this.pose(), (float) this.x, (float) this.y, depth).setUv(0, 0).setColor(color1);
        vertices.addVertexWith2DPose(this.pose(), (float) this.x, (float) this.y + this.height, depth).setUv(0, 1).setColor(color2);
        vertices.addVertexWith2DPose(this.pose(), (float) this.x + this.width, (float) this.y + this.height, depth).setUv(1, 1).setColor(color3);
        vertices.addVertexWith2DPose(this.pose(), (float) this.x + this.width, (float) this.y, depth).setUv(1, 0).setColor(color4);
    }

    @Override
    public RenderPipeline pipeline() {
        return WakesClient.GUI_HSV_PIPELINE;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
        // return TextureSetup.withoutGlTexture(MinecraftClient.getInstance().getTextureManager().getTexture(Identifier.ofVanilla("textures/block/dirt.png")).getGlTextureView());
    }
}
