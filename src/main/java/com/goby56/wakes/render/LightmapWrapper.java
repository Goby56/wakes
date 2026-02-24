package com.goby56.wakes.render;

import com.goby56.wakes.duck.LightmapAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class LightmapWrapper {
    private static final int[][] color = new int[16][16];
    private static final int[][] tick = new int[16][16];

    private static float mix(float x, float y, float a) {
        return x * (1.0f - a) + y * a;
    }

    private static float getBrightness(float level) {
        return level / (4.0f - 3.0f * level);
    }

    // Mirrors assets/minecraft/shaders/core/lightmap.fsh
    private static Vector3f notGamma(Vector3f color) {
        float maxComponent = Math.max(color.x, Math.max(color.y, color.z));
        if (maxComponent <= 0.0f) {
            return new Vector3f(0.0f, 0.0f, 0.0f);
        }

        float maxInverted = 1.0f - maxComponent;
        float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
        float scale = maxScaled / maxComponent;
        return new Vector3f(color).mul(scale);
    }

    private static Vector3f mix3(Vector3f color, Vector3f c2, float v) {
        return new Vector3f(
                mix(color.x, c2.x, v),
                mix(color.y, c2.y, v),
                mix(color.z, c2.z, v)
        );
    }

    private static Vector3f clamp3(Vector3f color, float min, float max) {
        return color.set(
                Mth.clamp(color.x, min, max),
                Mth.clamp(color.y, min, max),
                Mth.clamp(color.z, min, max)
        );
    }

    private static int calculatePixel(LightmapInfo info, int block, int sky) {
        float blockBrightness = getBrightness(block / 15.0f) * info.blockFactor();
        float skyBrightness = getBrightness(sky / 15.0f) * info.skyFactor();

        // cubic nonsense, dips to yellowish in the middle, white when fully saturated
        Vector3f c = new Vector3f(
                blockBrightness,
                blockBrightness * ((blockBrightness * 0.6f + 0.4f) * 0.6f + 0.4f),
                blockBrightness * (blockBrightness * blockBrightness * 0.6f + 0.4f)
        );

        c = mix3(c, info.ambientColor(), info.ambientLightFactor());
        c.add(new Vector3f(info.skyLightColor()).mul(skyBrightness));
        c = mix3(c, new Vector3f(0.75f), 0.04f);

        if (info.ambientLightFactor() == 0.0f) {
            Vector3f darkened = new Vector3f(c).mul(0.7f, 0.6f, 0.6f);
            c = mix3(c, darkened, info.darkenWorldFactor());
        }

        if (info.nightVisionFactor() > 0.0f) {
            float maxComponent = Math.max(c.x, Math.max(c.y, c.z));
            if (maxComponent > 0.0f && maxComponent < 1.0f) {
                Vector3f bright = new Vector3f(c).div(maxComponent);
                c = mix3(c, bright, info.nightVisionFactor());
            }
        }

        if (info.ambientLightFactor() == 0.0f) {
            c.sub(info.darknessScale(), info.darknessScale(), info.darknessScale());
        }

        c = clamp3(c, 0.0f, 1.0f);
        c = mix3(c, notGamma(c), info.brightnessFactor());
        c = mix3(c, new Vector3f(0.75f), 0.04f);

        return ARGB.colorFromFloat(1.0f, c.x, c.y, c.z);
    }

    public static int readPixel(int block, int sky) {
        LightmapInfo info = ((LightmapAccess) Minecraft.getInstance().gameRenderer.lightTexture())
                .wakes$getLightmapInfo();

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
