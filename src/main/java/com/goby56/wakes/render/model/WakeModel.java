package com.goby56.wakes.render.model;

import com.goby56.wakes.WakesClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public class WakeModel<T extends Entity> extends EntityModel<T> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(new Identifier(WakesClient.MOD_ID, "wake"), "main");
    private final ModelPart wake;

    public WakeModel(ModelPart root) {
        this.wake = root.getChild("wake");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bone = modelPartData.addChild("wake", ModelPartBuilder.create().uv(36, 42).cuboid(-8.0F, 0.0F, -24.0F, 16.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 42).cuboid(-9.0F, 0.0F, -22.0F, 18.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(40, 40).cuboid(-10.0F, 0.0F, -20.0F, 20.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 40).cuboid(-10.0F, 0.0F, -18.0F, 20.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 38).cuboid(-11.0F, 0.0F, -16.0F, 22.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 36).cuboid(-11.0F, 0.0F, -14.0F, 22.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 34).cuboid(-12.0F, 0.0F, -12.0F, 24.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 32).cuboid(-12.0F, 0.0F, -10.0F, 24.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 30).cuboid(-13.0F, 0.0F, -8.0F, 26.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 28).cuboid(-13.0F, 0.0F, -6.0F, 26.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 26).cuboid(-14.0F, 0.0F, -4.0F, 28.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 24).cuboid(-14.0F, 0.0F, -2.0F, 28.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 22).cuboid(-15.0F, 0.0F, 0.0F, 30.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 20).cuboid(-15.0F, 0.0F, 2.0F, 30.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 18).cuboid(-16.0F, 0.0F, 4.0F, 32.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 16).cuboid(-16.0F, 0.0F, 6.0F, 32.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 14).cuboid(-17.0F, 0.0F, 8.0F, 34.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 12).cuboid(-17.0F, 0.0F, 10.0F, 34.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 10).cuboid(-18.0F, 0.0F, 12.0F, 36.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 8).cuboid(-18.0F, 0.0F, 14.0F, 36.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 6).cuboid(-19.0F, 0.0F, 18.0F, 38.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 4).cuboid(-19.0F, 0.0F, 16.0F, 38.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 2).cuboid(-20.0F, 0.0F, 22.0F, 40.0F, 0.0F, 2.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-20.0F, 0.0F, 20.0F, 40.0F, 0.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        wake.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}
