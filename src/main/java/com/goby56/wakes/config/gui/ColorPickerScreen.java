package com.goby56.wakes.config.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    public ColorPickerScreen(Screen parent) {
        super(Text.of("Configure wake colors"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(new ColorIntervalSlider(
                this,
                (int) (width / 2 - width * 0.8f / 2), 24,
                (int) (width * 0.8f), 40));
    }

    @Override
    public void close() {
        client.setScreen(this.parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 10, 0xffffff);
    }

    @Override
    protected void applyBlur(float delta) {
        // No Song 2
    }

    public void addWidget(ClickableWidget widget) {
        this.addDrawableChild(widget);
    }
}
