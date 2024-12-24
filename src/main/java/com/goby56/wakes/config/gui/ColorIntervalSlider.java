package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.WakesConfigScreen;
import com.goby56.wakes.render.enums.WakeColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;

public class ColorIntervalSlider extends SliderWidget {
    private ArrayList<SliderHandle> handles;
    private final ColorPicker colorPicker;
    private Integer activeSection = null;

    public ColorIntervalSlider(WakesConfigScreen screenContext, int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""), 0f);
        this.handles = new ArrayList<>();
        for (float val : WakesClient.CONFIG_INSTANCE.wakeGradientRanges) {
           this.handles.add(new SliderHandle(val));
        }
        this.colorPicker = new ColorPicker(screenContext, 10, screenContext.height / 2, 100, 100);
        colorPicker.registerListener(this::onColorPicked);
    }

    private void unfocusHandles() {
        for (SliderHandle handle : handles) {
            handle.focused = false;
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        context.drawGuiTexture(this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());

        this.hovered = context.scissorContains(mouseX, mouseY) && mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        int n = handles.size();
        int y = this.getY();

        context.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        int prevX = this.getX();
        for (int i = 0; i < n; i++) {
            float value = handles.get(i).value;

            int currX = this.getX() + (int)(value * (double)(this.width));

            context.fill(prevX, y, currX, y + this.height, WakesClient.CONFIG_INSTANCE.wakeColors.get(i).argb);

            prevX = currX;
        }
        context.fill(prevX, y, this.getX() + this.width, y + this.height, WakesClient.CONFIG_INSTANCE.wakeColors.get(n).argb);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float hoveredVal = valueFromMousePos(mouseX);
        boolean correctY = mouseY >= getY() && mouseY < getY() + height;
        for (SliderHandle handle : handles) {
            boolean isHovered = handle.inProximity(hoveredVal, width, 8) && correctY;
            context.drawGuiTexture(handle.getHandleTexture(isHovered), this.getX() + (int)(handle.value * (double)(this.width - 4)), this.getY(), 8, this.getHeight());
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        float value = valueFromMousePos(mouseX);
        SliderHandle handle = closestHandle(value);
        unfocusHandles();
        if (handle.inProximity(value, width, 8)) {
            // Mouse on handle
            handle.focused = true;
        } else {
            // Do color picker
            int clickedSection = getActiveSection(value);
            if (activeSection != null && clickedSection != activeSection) {
                colorPicker.setActive(true);
            } else {
                colorPicker.setActive(!colorPicker.active);
            }
            activeSection = clickedSection;
            colorPicker.setColor(WakesClient.CONFIG_INSTANCE.wakeColors.get(activeSection));
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        unfocusHandles();
        super.onRelease(mouseX, mouseY);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        Collections.sort(handles);

        float value = valueFromMousePos(mouseX);
        for (SliderHandle handle : handles) {
            if (handle.focused) {
                if (handle.setValue(value)) {
                    this.applyValue();
                }
            }
        }
    }

    private void onColorPicked(WakeColor color) {
        if (this.activeSection != null) {
            WakesClient.CONFIG_INSTANCE.wakeColors.set(this.activeSection, color);
        }
    }

    private float valueFromMousePos(double mouseX) {
        return (float) ((mouseX - (double)(this.getX() + 4)) / (double)(this.width - 8));
    }

    private SliderHandle closestHandle(float value) {
        float min = 1f;
        SliderHandle closest = null;
        for (SliderHandle handle : handles) {
            float d = Math.abs(handle.value - value);
            if (d < min) {
                closest = handle;
                min = d;
            }
        }
        return closest;
    }

    private int getActiveSection(float value) {
        for (int i = 0; i < handles.size(); i++) {
            if (handles.get(i).value > value) {
                return i;
            }
        }
        return handles.size();
    }

    @Override
    protected void updateMessage() {
    }

    @Override
    protected void applyValue() {
        for (int i = 0; i < handles.size(); i++) {
            WakesClient.CONFIG_INSTANCE.wakeGradientRanges.set(i, handles.get(i).value);
        }
    }

}