package com.goby56.wakes.particle.custom;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.particle.WakeParticleType;
import com.goby56.wakes.render.model.WakeModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

public class WakeParticle extends Particle {
    Entity owner;
    Model wakeModel;
    RenderLayer wakeLayer;

    float yaw;

    protected WakeParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
        this.setMaxAge(200);
        this.wakeModel = new WakeModel<>(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(WakeModel.MODEL_LAYER));
        Identifier wakeTexture = new Identifier(WakesClient.MOD_ID, "textures/entity/wake_texture.png");
        this.wakeLayer = RenderLayer.getEntityAlpha(wakeTexture);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.owner != null) {
            Vec3d vel = this.owner.getVelocity();
            this.yaw = 90 - (float) (180 / Math.PI * Math.atan2(vel.z, vel.x));
            Vec3d ownerPos = this.owner.getPos().add(vel.rotateY((float) Math.PI).multiply(1.5f));
            this.setPos(ownerPos.x, ownerPos.y, ownerPos.z);
        }
        this.setPos(this.x, this.getWaterLevel(), this.z);
    }

    private float getWaterLevel() {
        // Taken from BoatEntity$getWaterLevelBelow
        Box box = this.owner.getBoundingBox();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable blockPos = new BlockPos.Mutable();


        // TODO FIX THIS LOOPY THING TO GIVE CORRECT HEIGHT
        yLoop:
        for (int y = minY; y < maxY; ++y) {
            float f = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = this.world.getFluidState(blockPos);
                    if (fluidState.isIn(FluidTags.WATER)) {
                        f = Math.max(f, fluidState.getHeight(this.world, blockPos));
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(String.format("Water at %f", blockPos.getY() + f)));
                    }
                    // T
                    if (f >= 1.0f) continue yLoop;
                }
            }
            if (!(f < 1.0f)) continue;
            return (float)blockPos.getY() + f;
        }
        return maxY;
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        MatrixStack modelMatrix = getMatrixStackFromCamera(camera, tickDelta);
        int light = this.getBrightness(tickDelta);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer modelConsumer = immediate.getBuffer(wakeLayer);

        modelMatrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.yaw + 180));

        this.wakeModel.render(modelMatrix, modelConsumer, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
        immediate.draw();
    }

    private MatrixStack getMatrixStackFromCamera(Camera camera, float tickDelta) {
        // Think it moves the matrix context smoothly to the camera
        // https://github.com/Ladysnake/Effective/blob/main/src/main/java/ladysnake/effective/particle/SplashParticle.java
        Vec3d cameraPos = camera.getPos();
        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(x, y, z);
        return matrixStack;
    }
    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider spriteSet) {
            this.sprites = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velX, double velY, double velZ) {
            WakeParticle wake = new WakeParticle(world, x, y, z);
            if (parameters instanceof WakeParticleType type) {
                wake.owner = type.owner;
                wake.yaw = type.owner.getYaw();
            }
            return wake;
        }
    }
}
