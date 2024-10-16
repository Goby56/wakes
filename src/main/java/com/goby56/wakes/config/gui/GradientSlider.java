package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GradientSlider extends SliderWidget {
    private ArrayList<Float> values;

    public GradientSlider(int x, int y, int width, int height, Text text, ArrayList<Float> values) {
        super(x, y, width, height, text, 0f);
        this.values = values;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        context.drawGuiTexture(this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        context.setShaderColor(1.0F, 1.0F, 1.0F, 0.3f);

        values = new ArrayList<>(List.of(0f, 0.1f, 0.5f, 0.7f, 1f));
        for (int i = 1; i < values.size(); i++) {
            float value = values.get(i - 1);
            float diff = values.get(i) - value;
            int x = this.getX() + (int)(value * (double)(this.width - 8));
            int y = this.getY();
            context.fill(x, y + 1, (int) (x + (diff * (this.width - 8))) + 8, y + this.height - 1, WakesClient.CONFIG_INSTANCE.wakeColors.get(i - 1).argb);
            context.drawGuiTexture(this.getHandleTexture(), x, y, 8, this.getHeight());
        }
        context.drawGuiTexture(this.getHandleTexture(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.active ? 16777215 : 10526880;
        this.drawScrollableText(context, minecraftClient.textRenderer, 2, i | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.of(String.valueOf(this.value)));
    }

    @Override
    protected void applyValue() {

    }
}
