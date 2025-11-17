package com.goby56.wakes.config.gui;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TexturedButton extends Button {
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    protected TexturedButton(int width, int height, OnPress onPress, ResourceLocation texture, int texWidth, int texHeight) {
        super(0, 0, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textureWidth = texWidth;
        this.textureHeight = texHeight;
    }

    public static com.goby56.wakes.config.gui.TexturedButton.Builder builder(Button.OnPress onPress) {
        return new com.goby56.wakes.config.gui.TexturedButton.Builder(onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        int tw = this.textureWidth;
        int th = this.textureHeight;
        int x = this.getX() + this.getWidth() / 2 - this.textureWidth / 2;
        int y = this.getY() + this.getHeight() / 2 - this.textureHeight / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, this.texture, x, y, 0, 0, tw, th, tw, th);
    }

    public static class Builder {
        private final Button.OnPress onPress;
        private int width = 30;
        private int height = 30;
        private ResourceLocation texture;
        private int textureWidth = 20;
        private int textureHeight = 20;

        public Builder(Button.OnPress onPress) {
            this.onPress = onPress;
        }

        public com.goby56.wakes.config.gui.TexturedButton.Builder dimension(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public com.goby56.wakes.config.gui.TexturedButton.Builder texture(ResourceLocation texture, int width, int height) {
            this.texture = texture;
            this.textureWidth = width;
            this.textureHeight = height;
            return this;
        }

        public TexturedButton build() {
            if (this.texture == null) {
                throw new IllegalStateException("Texture not set");
            } else {
                return new TexturedButton(this.width, this.height, this.onPress, this.texture, this.textureWidth, this.textureHeight);
            }
        }
    }
}
