package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.simulation.WakeHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.ladysnake.satin.api.event.PostWorldRenderCallbackV2;
import org.ladysnake.satin.api.event.ShaderEffectRenderCallback;
import org.ladysnake.satin.api.experimental.ReadableDepthFramebuffer;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import org.ladysnake.satin.api.managed.uniform.Uniform3f;
import org.ladysnake.satin.api.managed.uniform.UniformMat4;
import org.ladysnake.satin.api.util.GlMatrices;

public class WakePostEffect implements PostWorldRenderCallbackV2, ShaderEffectRenderCallback, ClientTickEvents.EndTick {
    public static final Identifier ID = Identifier.of(WakesClient.MOD_ID, "shaders/post/wakes_effect.json");

    // private WakeTexture testTexture;
    final ManagedShaderEffect SHADER = ShaderEffectManager.getInstance().manage(ID, shader -> {
        MinecraftClient client = MinecraftClient.getInstance();
        //testTexture = new WakeTexture(16, false);
        shader.setSamplerUniform("WakeSampler", ((ReadableDepthFramebuffer) client.getFramebuffer()).getStillDepthMap());
        shader.setSamplerUniform("DepthSampler", ((ReadableDepthFramebuffer) client.getFramebuffer()).getStillDepthMap());
        shader.setUniformValue("ViewPort", 0, 0, client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
    });

    private final UniformMat4 uniformInverseTransformMatrix = SHADER.findUniformMat4("InverseTransformMatrix");
    private final Uniform3f uniformCameraPosition = SHADER.findUniform3f("CameraPosition");
    private final Uniform3f uniformCenter = SHADER.findUniform3f("Center");

    public static void register(WakePostEffect instance) {
        ClientTickEvents.END_CLIENT_TICK.register(instance);
        ShaderEffectRenderCallback.EVENT.register(instance);
        PostWorldRenderCallbackV2.EVENT.register(instance);
    }

    @Override
    public void onEndTick(MinecraftClient client) {
    }

    @Override
    public void onWorldRendered(MatrixStack posingStack, Camera camera, float tickDelta) {
        uniformInverseTransformMatrix.set(GlMatrices.getInverseTransformMatrix(new Matrix4f()));
        Vec3d cameraPos = camera.getPos();
        uniformCameraPosition.set((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z);
        Entity e = camera.getFocusedEntity();
        uniformCenter.set(lerp(e.getX(), e.prevX, tickDelta), lerp(e.getY(), e.prevY, tickDelta), lerp(e.getZ(), e.prevZ, tickDelta));

        // if (WakeHandler.getInstance().isPresent()) {
        //     long ptr = WakeHandler.getInstance().get().tempTextureGetter();
        //     if (ptr == -1) {
        //         testTexture.loadTexture(ptr);
        //     }
        // }
    }

    @Override
    public void renderShaderEffects(float tickDelta) {
        SHADER.render(tickDelta);
    }

    private static float lerp(double a, double b, float t) {
        return (float) (a + t * (b - a));
    }
}
