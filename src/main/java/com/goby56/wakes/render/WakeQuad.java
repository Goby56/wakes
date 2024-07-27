package com.goby56.wakes.render;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.render.enums.WakeColor;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.world.World;
import org.lwjgl.system.MemoryUtil;

import java.util.List;
import java.util.Objects;

public class WakeQuad {
    public final int x;
    public final int z;
    public final int w;
    public final int h;
    public final WakeNode[][] nodes;

    public WakeQuad(int x, int z, int w, int h, WakeNode[][] affectedNodes) {
        this.x = x;
        this.z = z;
        this.w = w;
        this.h = h;
        this.nodes = affectedNodes;
    }

    public void populatePixels(WakeTexture texture, World world) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                WakeNode node = nodes[i][j];
                long nodeOffset = (((i*(long) h)+j)*w);
                int waterCol = BiomeColors.getWaterColor(world, node.blockPos());
                float opacity = (float) ((-Math.pow(node.t, 2) + 1) * WakesClient.CONFIG_INSTANCE.wakeOpacity);
                for (int r = 0; r < texture.res; r++) {
                    for (int c = 0; c < texture.res; c++) {
                        float avg = 0;
                        avg += (node.u[0][r + 1][c + 1] + node.u[1][r + 1][c + 1] + node.u[2][r + 1][c + 1]) / 3;
                        int color = WakeColor.getColor(avg, waterCol, opacity);
                        long pixelOffset = (((r*(long) texture.res)+c)*4);
                        MemoryUtil.memPutInt(texture.imgPtr + nodeOffset + pixelOffset, color);
                    }
                }
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, w, h);
    }
}
