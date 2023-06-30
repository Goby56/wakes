package com.goby56.wakes.config;

import com.goby56.wakes.render.WakeTextureRenderer;
import com.goby56.wakes.render.debug.WakeDebugRenderer;
import com.goby56.wakes.utils.WakeNode;
import com.goby56.wakes.utils.WakesUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;

public class ClothConfigScreen {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(WakesUtils.translatable("title", "config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory debug = builder.getOrCreateCategory(WakesUtils.translatable("category", "debug"));

        debug.addEntry(entryBuilder.startFloatField(WakesUtils.translatable("option", "wave_speed"), WakeNode.waveSpeed)
                .setDefaultValue(WakeNode.waveSpeed)
                .setSaveConsumer(WakeNode::setWaveSpeed)
                .build());

        debug.addEntry(entryBuilder.startFloatField(WakesUtils.translatable("option", "wave_initial_strength"), WakeNode.initialStrength)
                .setDefaultValue(WakeNode.initialStrength)
                .setSaveConsumer(strength -> WakeNode.initialStrength = strength)
                .build());

        debug.addEntry(entryBuilder.startFloatField(WakesUtils.translatable("option", "wave_decay"), WakeNode.waveDecay)
                .setDefaultValue(WakeNode.waveDecay)
                .setSaveConsumer(decay -> WakeNode.waveDecay = decay)
                .build());

        debug.addEntry(entryBuilder.startIntField(WakesUtils.translatable("option", "flood_fill_distance"), WakeNode.floodFillDistance)
                .setDefaultValue(WakeNode.floodFillDistance)
                .setSaveConsumer(dst -> WakeNode.floodFillDistance = dst)
                .build());

        debug.addEntry(entryBuilder.startBooleanToggle(WakesUtils.translatable("option", "use_9_point_stencil"), WakeNode.use9PointStencil)
                .setDefaultValue(WakeNode.use9PointStencil)
                .setSaveConsumer(b -> WakeNode.use9PointStencil = b)
                .build());

        debug.addEntry(entryBuilder.startBooleanToggle(WakesUtils.translatable("option", "draw_debug_boxes"), WakeDebugRenderer.drawDebugBoxes)
                .setDefaultValue(WakeDebugRenderer.drawDebugBoxes)
                .setSaveConsumer(b -> WakeDebugRenderer.drawDebugBoxes = b)
                .build());

        debug.addEntry(entryBuilder.startFloatField(WakesUtils.translatable("option", "ticks_before_fill"), WakeNode.ticksBeforeFill)
                .setDefaultValue(WakeNode.ticksBeforeFill)
                .setSaveConsumer(ticks -> WakeNode.ticksBeforeFill = ticks)
                .build());

        return builder.build();
    }
}
