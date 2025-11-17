package com.goby56.wakes.config.gui;

import com.goby56.wakes.utils.WakesUtils;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class WakesConfigScreen extends Screen {

    public WakesConfigScreen() {
        super(WakesUtils.translatable("gui", "title"));
    }

    @Override
    protected void init() {
        int y = this.height / 4 + 48;
        int ySpacing = 24;
        this.addRenderableWidget(Button.builder(WakesUtils.translatable("gui", "configButton"), (btn) -> {
            minecraft.setScreen(MidnightConfig.getScreen(this, "wakes"));
        }).bounds(this.width / 2 - 100, y + ySpacing, 200, 20).build());
        this.addRenderableWidget(Button.builder(WakesUtils.translatable("gui", "colorConfigButton"), (btn) -> {
            minecraft.setScreen(new ColorPickerScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, this.title, width / 2, 10, 0xffffff);
    }

}
