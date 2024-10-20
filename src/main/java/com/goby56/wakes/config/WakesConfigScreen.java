package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.gui.ColorPicker;
import com.goby56.wakes.config.gui.GradientSlider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class WakesConfigScreen extends Screen {

    public WakesConfigScreen() {
        super(Text.literal("Wakes Config"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(new GradientSlider(
                this,
                (int) (width / 2 - width * 0.8f / 2), 10,
                (int) (width * 0.8f), 40,
                Text.literal("Gradient slider"),
                new ArrayList<>(List.of(0.1f, 0.5f, 0.7f))));
    }

    @Override
    protected void applyBlur(float delta) {
        // No Song 2
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Resident evil"), width / 2, height / 2, 0xffffff);
    }

    public void addWidget(ClickableWidget widget) {
        this.addDrawableChild(widget);
    }
}
