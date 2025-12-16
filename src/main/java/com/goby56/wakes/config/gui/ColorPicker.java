package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.WakeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ColorPicker extends AbstractWidget {
    private static final Identifier FRAME_TEXTURE = Identifier.withDefaultNamespace("textures/gui/sprites/widget/slot_frame.png");
    private static final Identifier PICKER_BG_TEXTURE = Identifier.fromNamespaceAndPath(WakesClient.MOD_ID, "textures/picker_background.png");
    private static final Identifier PICKER_KNOB_TEXTURE = Identifier.fromNamespaceAndPath(WakesClient.MOD_ID, "textures/picker_knob.png");
    private static final int pickerKnobDim = 7;

    private final Map<String, Bounded> widgets = new HashMap<>();
    private final AABB colorPickerBounds;
    private Consumer<WakeColor> changedColorListener;

    private final Vector2f pickerPos = new Vector2f();
    private final Vector2f pickerCenter = new Vector2f();
    private final float pickerRadius;

    public ColorPicker(ColorPickerScreen screenContext, int x, int y, int width, int height) {
        super(x, y, width, height, Component.nullToEmpty(""));

        this.colorPickerBounds = new AABB(1/6f, 0, 5/6f, 2f / 3f, x, y, width, height);
        this.pickerCenter.x = this.colorPickerBounds.x + this.colorPickerBounds.width / 2f;
        this.pickerCenter.y = this.colorPickerBounds.y + this.colorPickerBounds.height / 2f;
        this.pickerRadius = this.colorPickerBounds.width / 2f;

        this.widgets.put("hueSlider", new ColorPickerSlider(new AABB(0f, 4f / 6f, 1f, 5f / 6f, x, y, width, height), "Hue", this, SliderUpdateType.HUE));
        this.widgets.put("alphaSlider", new ColorPickerSlider(new AABB(3f / 6f, 5f / 6f, 1f, 1f, x, y, width, height), "Opacity", this, SliderUpdateType.OPACITY));
        this.widgets.put("hexInputField", new HexInputField(new AABB(0f, 5f / 6f, 3f / 6f, 1f, x, y, width, height), this, Minecraft.getInstance().font));

        screenContext.addWidget(this);
        for (var widget : this.widgets.values()) {
            screenContext.addWidget(widget.getWidget());
        }
        this.setActive(false);
    }

    public void setActive(boolean active) {
        this.active = this.visible = active;
        for (var widget : this.widgets.values()) {
            widget.setActive(active);
        }
    }

    public void setColor(WakeColor currentColor, WidgetUpdateFlag updateFlag) {
        if (updateFlag.equals(WidgetUpdateFlag.ONLY_HEX)) {
            this.widgets.get("hexInputField").setColor(currentColor);
            return;
        }
        float[] hsv = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null);
        float x = this.colorPickerBounds.x + hsv[1] * this.colorPickerBounds.width;
        float y = this.colorPickerBounds.y + (1 - hsv[2]) * this.colorPickerBounds.height;
        this.pickerPos.set(x, y);
        for (var widgetKey : this.widgets.keySet()) {
            if (updateFlag.equals(WidgetUpdateFlag.IGNORE_HEX) && widgetKey.equals("hexInputField")) {
                this.changedColorListener.accept(currentColor);
                continue;
            }
            widgets.get(widgetKey).setColor(currentColor);
        }
    }

    public void registerListener(Consumer<WakeColor> changedListener) {
        this.changedColorListener = changedListener;
    }

    // @Override
    // public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //     AbstractWidget hexInput = this.widgets.get("hexInputField").getWidget();
    //     if (hexInput.isFocused()) {
    //         return hexInput.keyPressed(keyCode, scanCode, modifiers);
    //     }
    //     return super.keyPressed(keyCode, scanCode, modifiers);
    // }

    // @Override
    // public boolean charTyped(char chr, int modifiers) {
    //     AbstractWidget hexInput = this.widgets.get("hexInputField").getWidget();
    //     if (hexInput.isFocused()) {
    //         return hexInput.charTyped(chr, modifiers);
    //     }
    //     return super.charTyped(chr, modifiers);
    // }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
        double x = mouseButtonEvent.x(), y = mouseButtonEvent.y();
        AbstractWidget focusedWidget = null;
        for (var widget : this.widgets.values()) {
            widget.getWidget().setFocused(false);
            if (widget.getBounds().contains((int) x, (int) y)) {
                focusedWidget = widget.getWidget();
            }
        }
        if (focusedWidget != null) {
            focusedWidget.setFocused(true);
            focusedWidget.onClick(mouseButtonEvent, bl);
            return;
        }
        this.updatePickerPos(x, y);
        super.onClick(mouseButtonEvent, bl);
    }

    @Override
    public void onDrag(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        double mouseX = Math.min(this.getX() + width, Math.max(this.getX(), mouseButtonEvent.x()));
        double mouseY = Math.min(this.getY() + height, Math.max(this.getY(), mouseButtonEvent.y()));
        for (var widget : this.widgets.values()) {
            if (widget.getWidget().isFocused()) {
                widget.getWidget().onDrag(mouseButtonEvent, deltaX, deltaY);
                return;
            }
        }
        this.updatePickerPos(mouseX, mouseY);
        super.onDrag(mouseButtonEvent, deltaX, deltaY);
    }

    public Vector2f getPolarPos(double mouseX, double mouseY) {
        double x = mouseX - this.pickerCenter.x;
        double y = mouseY - this.pickerCenter.y;
        float r = (float) Math.min(Math.sqrt(x*x + y*y), this.pickerRadius * 0.90f);
        float v = (float) Math.atan2(y, x);
        return new Vector2f(r, v);
    }

    public void updatePickerPos(double mouseX, double mouseY) {
        mouseX = Math.min(this.colorPickerBounds.x + this.colorPickerBounds.width, Math.max(this.colorPickerBounds.x, mouseX));
        mouseY = Math.min(this.colorPickerBounds.y + this.colorPickerBounds.height, Math.max(this.colorPickerBounds.y, mouseY));
        this.pickerPos.set(mouseX, mouseY);
        this.updateColor();
    }

    public void updateColor() {
        float hue = ((ColorPickerSlider) this.widgets.get("hueSlider").getWidget()).getValue();
        float saturation = (pickerPos.x - this.colorPickerBounds.x) / this.colorPickerBounds.width;
        float value = 1f - (pickerPos.y - this.colorPickerBounds.y) / this.colorPickerBounds.height;
        float opacity = ((ColorPickerSlider) this.widgets.get("alphaSlider").getWidget()).getValue();
        WakeColor newColor = new WakeColor(hue, saturation, value, opacity);
        this.setColor(newColor, WidgetUpdateFlag.ONLY_HEX);
        this.changedColorListener.accept(newColor);
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!active) return;
        context.blit(RenderPipelines.GUI_TEXTURED, PICKER_BG_TEXTURE, colorPickerBounds.x, colorPickerBounds.y, 0, 0, colorPickerBounds.width, colorPickerBounds.height, colorPickerBounds.width, colorPickerBounds.height);
        context.blit(RenderPipelines.GUI_TEXTURED, FRAME_TEXTURE, colorPickerBounds.x, colorPickerBounds.y, 0, 0, colorPickerBounds.width, colorPickerBounds.height, colorPickerBounds.width, colorPickerBounds.height);

        drawPickerBox(context);
        // Draw picker knob
        int d = pickerKnobDim;
        context.blit(RenderPipelines.GUI_TEXTURED, PICKER_KNOB_TEXTURE, (int) pickerPos.x - 3, (int) pickerPos.y - 3, 0, 0, d, d, d, d);
    }

    private void drawPickerBox(GuiGraphics context) {
        int y = colorPickerBounds.y + 3;
        int x = colorPickerBounds.x + 3;
        int w = colorPickerBounds.width - 6;
        int h = colorPickerBounds.height - 6;

        int hue = (int) (((ColorPickerSlider) this.widgets.get("hueSlider").getWidget()).getValue() * 255);

        context.guiRenderState.submitGuiElement(new HsvQuadGuiElementRenderState(
                new Matrix3x2f(context.pose()),
                x, y, w, h,
                0x7F0000FF | hue << 16,
                0x7F000000 | hue << 16,
                0x7F00FF00 | hue << 16,
                0x7F00FFFF | hue << 16,
                context.scissorStack.peek()
                )
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

    public interface Bounded {
        AABB getBounds();
        AbstractWidget getWidget();

        void setActive(boolean active);

        void setColor(WakeColor currentColor);
    }

    public static class AABB {
        public int x;
        public int y;
        public int width;
        public int height;

        public AABB(float fracX1, float fracY1, float fracX2, float fracY2, int globX, int globY, int totWidth, int totHeight) {
            this.x = Math.round(fracX1 * totWidth) + globX + 1;
            this.y = Math.round(fracY1 * totHeight) + globY + 1;
            this.width = Math.round((fracX2 - fracX1) * totWidth) - 2;
            this.height = Math.round((fracY2 - fracY1) * totHeight) - 2;
        }

        public boolean contains(int x, int y) {
            return this.x <= x && x < this.x + this.width &&
                    this.y <= y && y < this.y + this.height;
        }
    }

    private static class HexInputField extends EditBox implements Bounded {
        protected AABB bounds;
        private final ColorPicker colorPicker;
        private final Pattern hexColorRegex;
        private boolean autoUpdate = false;

        public HexInputField(AABB bounds, ColorPicker colorPicker, Font textRenderer) {
            super(textRenderer, bounds.x, bounds.y, bounds.width, bounds.height, Component.empty());
            this.setMaxLength(9); // #AARRGGBB
            this.bounds = bounds;
            this.colorPicker = colorPicker;
            this.setFilter(HexInputField::validHex);
            this.hexColorRegex = Pattern.compile("#[a-f0-9]{7,9}", Pattern.CASE_INSENSITIVE);
        }

        @Override
        protected void onValueChange(String newText) {
            if (autoUpdate) {
                // Ensures color picker doesn't update itself when updating hex string
                return;
            }
            // Only manual edits to the hex field should update the color picker
            if (hexColorRegex.matcher(newText).find()) {
                this.colorPicker.setColor(new WakeColor(newText), WidgetUpdateFlag.IGNORE_HEX);
            }
            super.onValueChange(newText);
        }

        private static boolean validHex(String text) {
            if (text.length() > 9) {
                return false;
            }
            for (char c : text.toLowerCase().toCharArray()) {
                if (Character.digit(c, 16) == -1 && c != '#') {
                    return false;
                }
            }
            return true;
        }

        public void setActive(boolean active) {
            this.active = this.visible = active;
        }

        @Override
        public boolean charTyped(CharacterEvent characterEvent) {
            this.autoUpdate = false;
            return super.charTyped(characterEvent);
        }

        @Override
        public boolean keyPressed(KeyEvent keyEvent) {
            this.autoUpdate = false;
            return super.keyPressed(keyEvent);
        }

        @Override
        public void setColor(WakeColor currentColor) {
            this.autoUpdate = true;
            this.setValue(currentColor.toHex());
        }

        @Override
        public AABB getBounds() {
            return this.bounds;
        }

        @Override
        public AbstractWidget getWidget() {
            return this;
        }
    }

    private static class ColorPickerSlider extends AbstractSliderButton implements Bounded {
        protected AABB bounds;
        private final SliderUpdateType type;

        private final ColorPicker colorPicker;

        public ColorPickerSlider(AABB bounds, String text, ColorPicker colorPicker, SliderUpdateType type) {
            super(bounds.x, bounds.y, bounds.width, bounds.height, Component.nullToEmpty(text), 1f);
            this.bounds = bounds;
            this.colorPicker = colorPicker;
            this.type = type;
        }

        public void setActive(boolean active) {
            this.active = this.visible = active;
        }

        @Override
        public void setColor(WakeColor currentColor) {
            if (type.equals(SliderUpdateType.HUE)) {
                this.value = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null)[0];
            } else
            if (type.equals(SliderUpdateType.SATURATION)) {
                this.value = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null)[1];
            } else
            if (type.equals(SliderUpdateType.VALUE)) {
                this.value = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null)[2];
            } else {
                this.value = currentColor.a / 255f;
            }
        }

        public float getValue() {
            return (float) this.value;
        }

        @Override
        public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight());

            if (this.type.equals(SliderUpdateType.HUE)) {
                context.guiRenderState.submitGuiElement(new HsvQuadGuiElementRenderState(
                        new Matrix3x2f(context.pose()),
                        this.getX() + 1, this.getY() + 1, this.getWidth() - 2, this.getHeight() - 2,
                        0x4000FFFF,
                        0x4000FFFF,
                        0x40FFFFFF,
                        0x40FFFFFF,
                        context.scissorStack.peek()
                    )
                );
            }
            context.blitSprite(RenderPipelines.GUI_TEXTURED, this.getHandleSprite(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
            int i = this.active ? 0xFFFFFF : 0xA0A0A0;
            this.renderScrollingStringOverContents(context.textRenderer(), this.message, i | Mth.ceil(this.alpha * 255.0f) << 24);
        }

        @Override
        protected void updateMessage() {

        }

        @Override
        protected void applyValue() {
            colorPicker.updateColor();
        }

        @Override
        public AABB getBounds() {
            return this.bounds;
        }

        @Override
        public AbstractWidget getWidget() {
            return this;
        }
    }

    public enum SliderUpdateType {
        HUE,
        SATURATION,
        VALUE,
        OPACITY
    }

    public enum WidgetUpdateFlag {
        ALL,
        ONLY_HEX,
        IGNORE_HEX
    }
}
