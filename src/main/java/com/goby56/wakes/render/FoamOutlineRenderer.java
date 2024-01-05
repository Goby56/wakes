package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.utils.WakesUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.stream.Stream;

public class FoamOutlineRenderer {

    public static <T extends Entity> void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (WakesClient.CONFIG_INSTANCE.disableMod || !WakesUtils.getEffectRuleFromSource(entity).simulateWakes || !((ProducesWake) entity).onWaterSurface()) {
            return;
        }

        float height = WakesUtils.getWaterLevel(entity.getWorld(), entity);

        String[] typeID = entity.getType().toString().split("\\.");

        EntityModelLoader modelLoader = MinecraftClient.getInstance().getEntityModelLoader();
        Stream<ModelPart> parts = EntityModelLayers.getLayers()
                .filter(layer -> layer.getId().toString().contains(typeID[typeID.length - 1]))
                .map(modelLoader::getModelPart);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getRenderTypeEntitySolidProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        RenderSystem.setShaderTexture(0, new Identifier("wakes", "icon.png"));

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Vector3f entityPos = entity.getPos().toVector3f();
        parts.forEach(modelPart -> {

            modelPart.forEachCuboid(matrices, (entry, path, index, cuboid) -> {
                Matrix4f cuboidMatrix = entry.getPositionMatrix();
                float minY = Float.POSITIVE_INFINITY;
                float maxY = Float.NEGATIVE_INFINITY;
                for (ModelPart.Quad quad : cuboid.sides) {
                    for (ModelPart.Vertex vertex : quad.vertices) {
                        Vector3f pos = getVertexAbsolutePos(vertex, modelPart, cuboidMatrix, entityPos);
                        if (pos.y < minY) minY = pos.y;
                        if (pos.y > maxY) maxY = pos.y;
                    }
                }
                if (minY > height || maxY < height) return;

                for (ModelPart.Quad quad : cuboid.sides) {
                    if (quad.direction.y == 0) continue;
                    for (int i = 0; i < 4; i++) {
                        Vector3f pos = getVertexAbsolutePos(quad.vertices[i], modelPart, cuboidMatrix, entityPos); // TODO CACHE RESULT
                        System.out.printf("writing vertex %d at %s from %s\n", i, pos, path);
                        buffer.vertex(matrix, pos.x + 0.1f * Math.signum(pos.x), height, pos.z + 0.1f * Math.signum(pos.z))
                                .color(1f, 1f, 1f, 1f)
                                .texture(i < 2 ? 0 : 1, i == 0 || i == 3 ? 0 : 1)
                                .overlay(OverlayTexture.DEFAULT_UV)
                                .light(light)
                                .normal(0, 1f, 0)
                                .next();
                    }
                    return;
                }
            });
        });

        Tessellator.getInstance().draw();
    }

    private static Vector3f getVertexAbsolutePos(ModelPart.Vertex vertex, ModelPart part, Matrix4f cuboidTransform, Vector3f origin) {
        Vector4f pos = cuboidTransform.transform(
                new Vector4f((part.pivotX + vertex.pos.x) / 16f, (part.pivotY + vertex.pos.y) / 16f, (part.pivotZ + vertex.pos.z) / 16f, 0));
        return new Vector3f(pos.x + origin.x, pos.y + origin.y, pos.z + origin.z);
    }
}
