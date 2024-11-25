package com.goby56.wakes.config;

import com.goby56.wakes.config.gui.ColorIntervalSlider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class WakesConfigScreen extends Screen {

    public WakesConfigScreen() {
        super(Text.literal("Wakes Config"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(new ColorIntervalSlider(
                this,
                (int) (width / 2 - width * 0.8f / 2), 30,
                (int) (width * 0.8f), 40));
    }

    @Override
    protected void applyBlur(float delta) {
        // No Song 2
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Wake color gradient slider"), width / 2, 10, 0xffffff);
    }

    public void addWidget(ClickableWidget widget) {
        this.addDrawableChild(widget);
    }
}
