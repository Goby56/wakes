package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

public record HsvQuadGuiElementRenderState(
        Matrix3x2f pose,
        int x, int y, int width, int height,
        int color1, int color2, int color3, int color4,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds
) implements SimpleGuiElementRenderState {
    public HsvQuadGuiElementRenderState(
            Matrix3x2f pose,
            int x, int y, int width, int height,
            int color1, int color2, int color3, int color4,
            @Nullable ScreenRect scissorArea
    ) {
        this(pose, x, y, width, height, color1, color2, color3, color4, scissorArea, createBounds(x, y, x + width, y + height, pose, scissorArea));
    }

    @Nullable
    private static ScreenRect createBounds(int x1, int y1, int x2, int y2, Matrix3x2f pose, @Nullable ScreenRect scissorArea) {
        ScreenRect screenRect = new ScreenRect(x1, y1, x2 - x1, y2 - y1).transformEachVertex(pose);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }

    @Override
    public void setupVertices(VertexConsumer vertices, float depth) {
        vertices.vertex(this.pose(), (float) this.x, (float) this.y, depth).texture(0, 0).color(color1);
        vertices.vertex(this.pose(), (float) this.x, (float) this.y + this.height, depth).texture(0, 1).color(color2);
        vertices.vertex(this.pose(), (float) this.x + this.width, (float) this.y + this.height, depth).texture(1, 1).color(color3);
        vertices.vertex(this.pose(), (float) this.x + this.width, (float) this.y, depth).texture(1, 0).color(color4);
    }

    @Override
    public RenderPipeline pipeline() {
        return WakesClient.GUI_HSV_PIPELINE;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.withoutGlTexture(MinecraftClient.getInstance().getTextureManager().getTexture(Identifier.ofVanilla("textures/block/dirt.png")).getGlTextureView());
    }
}
