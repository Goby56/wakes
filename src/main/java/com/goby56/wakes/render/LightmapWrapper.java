package com.goby56.wakes.render;

import com.goby56.wakes.duck.LightmapAccess;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class LightmapWrapper {
    private static int[][] color = new int[16][16];
    private static int[][] tick = new int[16][16];

    private static float mix(float x, float y, float a) {
        return x * (1.0f - a) + y * a;
    }

    private static float get_brightness(float level, float AmbientLightFactor) {
        float curved_level = level / (4.0f - 3.0f * level);
        return mix(curved_level, 1.0f, AmbientLightFactor);
    }

    public static Vector3f notGamma(Vector3f v) {
        float nx0 = 1.0f - v.x;
        float nx1 = 1.0f - v.y;
        float nx2 = 1.0f - v.z;
        return new Vector3f(
                1.0f - nx0 * nx0 * nx0 * nx0,
                1.0f - nx1 * nx1 * nx1 * nx1,
                1.0f - nx2 * nx2 * nx2 * nx2);
    }

    private static Vector3f mix3(Vector3f color, Vector3f c2, float v) {
        return new Vector3f(mix(color.x, c2.x, v),
                mix(color.y, c2.y, v),
                mix(color.z, c2.z, v));
    }

    private static int calculatePixel(LightmapInfo info, int block, int sky) {
        float block_brightness = get_brightness(block / 15.0f, info.AmbientLightFactor()) * info.BlockFactor();
        float sky_brightness = get_brightness(sky / 15.0f, info.AmbientLightFactor()) * info.SkyFactor();

        // cubic nonsense, dips to yellowish in the middle, white when fully saturated
        Vector3f color = new Vector3f(
                block_brightness,
                block_brightness * ((block_brightness * 0.6f + 0.4f) * 0.6f + 0.4f),
                block_brightness * (block_brightness * block_brightness * 0.6f + 0.4f));

        if (info.UseBrightLightmap()) {
            color = mix3(color, new Vector3f(0.99f, 1.12f, 1.0f), 0.25f);
            color = clamp3(color, 0.0, 1.0);
        } else {
            color.add(info.skyLightColor().mul(sky_brightness, new Vector3f()));
            color = mix3(color, new Vector3f(0.75f), 0.04f);

            Vector3f darkened_color = color.mul(new Vector3f(0.7f, 0.6f, 0.6f), new Vector3f());
            color = mix3(color, darkened_color, info.DarkenWorldFactor());
        }

        if (info.NightVisionFactor() > 0.0) {
            // scale up uniformly until 1.0 is hit by one of the colors
            float max_component = Math.max(color.x, Math.max(color.y, color.z));
            if (max_component < 1.0) {
                Vector3f bright_color = color.div(max_component, new Vector3f());
                color = mix3(color, bright_color, info.NightVisionFactor());
            }
        }

        if (!info.UseBrightLightmap()) {
            color = clamp3(color.sub(new Vector3f(info.DarknessScale()), new Vector3f()), 0.0, 1.0);
        }

        Vector3f notGamma = notGamma(color);
        color = mix3(color, notGamma, info.BrightnessFactor());
        color = mix3(color, new Vector3f(0.75f), 0.04f);
        color = clamp3(color, 0.0, 1.0);

        return ColorHelper.fromFloats(1.0f, color.x, color.y, color.z);
    }

    private static Vector3f clamp3(Vector3f color, double v, double v1) {
        return color.set(MathHelper.clamp(color.x, v, v1), MathHelper.clamp(color.y, v, v1),
                MathHelper.clamp(color.z, v, v1));
    }

    public static int readPixel(int block, int sky) {
        LightmapInfo info = ((LightmapAccess) MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager())
                .wakes$getLightmapInfo();

        // Added null check to avoid NullPointerException if info is null
        if (info == null) {
            return color[block][sky];
        }

        if (tick[block][sky] != info.currentTick()) {
            tick[block][sky] = info.currentTick();
            color[block][sky] = calculatePixel(info, block, sky);
        }

        return color[block][sky];
    }

    public static void render(Matrix4f matrix) {

    }
}
