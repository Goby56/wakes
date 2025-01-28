package com.goby56.wakes.config.gui;

import com.goby56.wakes.config.gui.ColorPickerScreen;
import com.goby56.wakes.utils.WakesUtils;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

public class WakesConfigScreen extends Screen {

    public WakesConfigScreen() {
        super(WakesUtils.translatable("gui", "title"));
    }

    @Override
    protected void init() {
        int y = this.height / 4 + 48;
        int ySpacing = 24;
        this.addDrawableChild(ButtonWidget.builder(WakesUtils.translatable("gui", "configButton"), (btn) -> {
            client.setScreen(MidnightConfig.getScreen(this, "wakes"));
        }).dimensions(this.width / 2 - 100, y + ySpacing, 200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(WakesUtils.translatable("gui", "colorConfigButton"), (btn) -> {
            client.setScreen(new ColorPickerScreen(this));
        }).dimensions(this.width / 2 - 100, y, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 10, 0xffffff);
    }

}
