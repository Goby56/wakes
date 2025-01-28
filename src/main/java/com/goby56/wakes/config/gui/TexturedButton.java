package com.goby56.wakes.config.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TexturedButton extends ButtonWidget {
    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    protected TexturedButton(int width, int height, PressAction onPress, Identifier texture, int texWidth, int texHeight) {
        super(0, 0, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.texture = texture;
        this.textureWidth = texWidth;
        this.textureHeight = texHeight;
    }

    public static com.goby56.wakes.config.gui.TexturedButton.Builder builder(ButtonWidget.PressAction onPress) {
        return new com.goby56.wakes.config.gui.TexturedButton.Builder(onPress);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderButton(context, mouseX, mouseY, delta);
        int tw = this.textureWidth;
        int th = this.textureHeight;
        int x = this.getX() + this.getWidth() / 2 - this.textureWidth / 2;
        int y = this.getY() + this.getHeight() / 2 - this.textureHeight / 2;
        context.drawTexture(this.texture, x, y, 0, 0, tw, th, tw, th);
    }

    public static class Builder {
        private final ButtonWidget.PressAction onPress;
        private int width = 30;
        private int height = 30;
        private Identifier texture;
        private int textureWidth = 20;
        private int textureHeight = 20;

        public Builder(ButtonWidget.PressAction onPress) {
            this.onPress = onPress;
        }

        public com.goby56.wakes.config.gui.TexturedButton.Builder dimension(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public com.goby56.wakes.config.gui.TexturedButton.Builder texture(Identifier texture, int width, int height) {
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
