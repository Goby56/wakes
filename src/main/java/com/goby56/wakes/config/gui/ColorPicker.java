package com.goby56.wakes.config.gui;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.enums.WakeColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ColorPicker extends ClickableWidget {
    private static final Identifier FRAME_TEXTURE = Identifier.ofVanilla("widget/slot_frame");
    private static final Identifier PICKER_KNOB_TEXTURE = Identifier.of("wakes", "textures/picker_knob.png");
    private static final int pickerKnobDim = 7;

    private final Map<String, Bounded> widgets = new HashMap<>();
    private final AABB bounds;
    private Consumer<WakeColor> changedColorListener;

    private final Vector2f pickerPos = new Vector2f();

    public ColorPicker(ColorPickerScreen screenContext, int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""));

        this.bounds = new AABB(0, 0, 1f, 2f / 3f, x, y, width, height);

        this.widgets.put("hueSlider", new GradientSlider(new AABB(0f, 4f / 6f, 1f, 5f / 6f, x, y, width, height), "Hue", this,true));
        this.widgets.put("alphaSlider", new GradientSlider(new AABB(3f / 6f, 5f / 6f, 1f, 1f, x, y, width, height), "Opacity", this, false));
        this.widgets.put("hexInputField", new HexInputField(new AABB(0f, 5f / 6f, 3f / 6f, 1f, x, y, width, height), this, MinecraftClient.getInstance().textRenderer));

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
        this.pickerPos.set(
                this.bounds.x + hsv[1] * this.bounds.width,
                this.bounds.y + (1 - hsv[2]) * this.bounds.height);
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ClickableWidget hexInput = this.widgets.get("hexInputField").getWidget();
        if (hexInput.isFocused()) {
            return hexInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        ClickableWidget hexInput = this.widgets.get("hexInputField").getWidget();
        if (hexInput.isFocused()) {
            return hexInput.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ClickableWidget focusedWidget = null;
        for (var widget : this.widgets.values()) {
            widget.getWidget().setFocused(false);
            if (widget.getBounds().contains((int) mouseX, (int) mouseY)) {
                focusedWidget = widget.getWidget();
            }
        }
        if (focusedWidget != null) {
            focusedWidget.setFocused(true);
            focusedWidget.onClick(mouseX, mouseY);
            return;
        }
        this.updatePickerPos(mouseX, mouseY);
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        mouseX = Math.min(this.getX() + width, Math.max(this.getX(), mouseX));
        mouseY = Math.min(this.getY() + height, Math.max(this.getY(), mouseY));
        for (var widget : this.widgets.values()) {
            if (widget.getWidget().isFocused()) {
                widget.getWidget().onDrag(mouseX, mouseY, deltaX, deltaY);
                return;
            }
        }
        this.updatePickerPos(mouseX, mouseY);
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    public void updatePickerPos(double mouseX, double mouseY) {
        mouseX = Math.min(this.bounds.x + this.bounds.width, Math.max(this.bounds.x, mouseX));
        mouseY = Math.min(this.bounds.y + this.bounds.height, Math.max(this.bounds.y, mouseY));
        this.pickerPos.set(mouseX, mouseY);
        this.updateColor();
    }

    public void updateColor() {
        float hue = ((GradientSlider) this.widgets.get("hueSlider").getWidget()).getValue();
        float saturation = (pickerPos.x - this.bounds.x) / this.bounds.width;
        float value = 1f - (pickerPos.y - this.bounds.y) / this.bounds.height;
        float opacity = ((GradientSlider) this.widgets.get("alphaSlider").getWidget()).getValue();
        WakeColor newColor = new WakeColor(hue, saturation, value, opacity);
        this.setColor(newColor, WidgetUpdateFlag.ONLY_HEX);
        this.changedColorListener.accept(newColor);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!active) return;

        // Draw color spectrum
        int x = bounds.x;
        int y = bounds.y;
        int w = bounds.width;
        int h = bounds.height;

        RenderSystem.setShader(WakesClient.POSITION_TEXTURE_HSV::getProgram);
        RenderSystem.setShaderTexture(0, GradientSlider.BLANK_SLIDER_TEXTURE);
        float hue = ((GradientSlider) widgets.get("hueSlider").getWidget()).getValue();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        buffer.vertex(matrix, x, y, 5).texture(0, 0).color(hue, 0f, 1f, 1f);
        buffer.vertex(matrix, x, y + h, 5).texture(0, 1).color(hue, 0f, 0f, 1f);
        buffer.vertex(matrix, x + w, y + h, 5).texture(1, 1).color(hue, 1f, 0f, 1f);
        buffer.vertex(matrix, x + w, y, 5).texture(1, 0).color(hue, 1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Draw frame
        context.drawGuiTexture(FRAME_TEXTURE, x, y, w, h);

        // Draw picker knob
        int d = pickerKnobDim;
        int pickerX = (int) Math.min(bounds.x + bounds.width - d, Math.max(bounds.x, pickerPos.x - 3));
        int pickerY = (int) Math.min(bounds.y + bounds.height - d, Math.max(bounds.y, pickerPos.y - 3));
        context.drawTexture(PICKER_KNOB_TEXTURE, pickerX, pickerY, 0, 0, d, d, d, d);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    public interface Bounded {
        AABB getBounds();
        ClickableWidget getWidget();

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

    private static class HexInputField extends TextFieldWidget implements Bounded {
        protected AABB bounds;
        private final ColorPicker colorPicker;
        private final Pattern hexColorRegex;
        private boolean autoUpdate = false;

        public HexInputField(AABB bounds, ColorPicker colorPicker, TextRenderer textRenderer) {
            super(textRenderer, bounds.x, bounds.y, bounds.width, bounds.height, Text.empty());
            this.setMaxLength(9); // #AARRGGBB
            this.bounds = bounds;
            this.colorPicker = colorPicker;
            this.setTextPredicate(HexInputField::validHex);
            this.hexColorRegex = Pattern.compile("#[a-f0-9]{7,9}", Pattern.CASE_INSENSITIVE);
        }

        @Override
        protected void onChanged(String newText) {
            if (autoUpdate) {
                // Ensures color picker doesn't update itself when updating hex string
                return;
            }
            // Only manual edits to the hex field should update the color picker
            if (hexColorRegex.matcher(newText).find()) {
                this.colorPicker.setColor(new WakeColor(newText), WidgetUpdateFlag.IGNORE_HEX);
            }
            super.onChanged(newText);
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
        public boolean charTyped(char chr, int modifiers) {
            this.autoUpdate = false;
            return super.charTyped(chr, modifiers);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            this.autoUpdate = false;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void setColor(WakeColor currentColor) {
            this.autoUpdate = true;
            this.setText(currentColor.toHex());
        }

        @Override
        public AABB getBounds() {
            return this.bounds;
        }

        @Override
        public ClickableWidget getWidget() {
            return this;
        }
    }

    private static class GradientSlider extends SliderWidget implements Bounded {
        private static final Identifier TRANSPARENT_SLIDER_TEXTURE = Identifier.of("wakes", "textures/transparent_slider.png");
        private static final Identifier BLANK_SLIDER_TEXTURE = Identifier.of("wakes", "textures/blank_slider.png");

        protected AABB bounds;
        private final boolean colored;

        private final ColorPicker colorPicker;

        public GradientSlider(AABB bounds, String text, ColorPicker colorPicker, boolean colored) {
            super(bounds.x, bounds.y, bounds.width, bounds.height, Text.of(text), 1f);
            this.bounds = bounds;
            this.colorPicker = colorPicker;
            this.colored = colored;
        }

        public void setActive(boolean active) {
            this.active = this.visible = active;
        }

        @Override
        public void setColor(WakeColor currentColor) {
            if (colored) {
                this.value = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null)[0];
            } else {
                this.value = currentColor.a / 255f;
            }
        }

        public float getValue() {
            return (float) this.value;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            context.drawGuiTexture(this.getTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            int leftCol, rightCol;
            if (colored) {
                context.setShaderColor(1.0F, 1.0F, 1.0F, 0.3f);
                RenderSystem.setShader(WakesClient.POSITION_TEXTURE_HSV::getProgram);
                RenderSystem.setShaderTexture(0, BLANK_SLIDER_TEXTURE);

                // AAHHSSVV
                leftCol = 0xFF00FFFF;
                rightCol = 0xFFFFFFFF;

            } else {
                context.setShaderColor(1.0f, 1.0f, 1.0f, 0.6f);
                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
                RenderSystem.setShaderTexture(0, TRANSPARENT_SLIDER_TEXTURE);

                // AARRGGBB
                leftCol = 0xFFFFFFFF;
                rightCol = 0x00FFFFFF;
            }

            int x = bounds.x;
            int y = bounds.y;
            int w = bounds.width;
            int h = bounds.height;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

            buffer.vertex(matrix, x, y, 5).texture(0, 0).color(leftCol);
            buffer.vertex(matrix, x, y + h, 5).texture(0, 1).color(leftCol);
            buffer.vertex(matrix, x + w, y + h, 5).texture(1, 1).color(rightCol);
            buffer.vertex(matrix, x + w, y, 5).texture(1, 0).color(rightCol);

            BufferRenderer.drawWithGlobalProgram(buffer.end());


            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.drawGuiTexture(this.getHandleTexture(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
            int i = this.active ? 0xFFFFFF : 0xA0A0A0;
            this.drawScrollableText(context, MinecraftClient.getInstance().textRenderer, 2, i | MathHelper.ceil((float)(this.alpha * 255.0f)) << 24);
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
        public ClickableWidget getWidget() {
            return this;
        }
    }

    public enum WidgetUpdateFlag {
        ALL,
        ONLY_HEX,
        IGNORE_HEX
    }
}
