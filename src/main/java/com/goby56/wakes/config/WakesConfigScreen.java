package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.gui.GradientSlider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.LayoutWidgets;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

public class WakesConfigScreen extends Screen {

    public WakesConfigScreen() {
        super(Text.literal("Wakes Config"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(new GradientSlider((int) (width / 2 - width * 0.8f / 2), 10, (int) (width * 0.8f), 40, Text.literal("Gradient slider"), WakesClient.CONFIG_INSTANCE.wakeGradientRanges));
    }

    @Override
    protected void applyBlur(float delta) {
        // No Song 2
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("You must see me"), width / 2, height / 2, 0xffffff);
    }
}
