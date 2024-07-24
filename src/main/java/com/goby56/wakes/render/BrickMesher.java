package com.goby56.wakes.render;

import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeNode;

import java.util.ArrayList;
import java.util.List;

public class BrickMesher {
    public static List<WakeRenderer.WakeQuad> generateMesh(Brick brick) {
        ArrayList<WakeRenderer.WakeQuad> quads = new ArrayList<>();
        var ints = brick.bitMask;
        for (int i = 0; i < brick.dim; i++) {
            int j = 0;
            while (j < brick.dim) {
                j += Integer.numberOfTrailingZeros(ints[i] >> j);
                if (j >= brick.dim) continue;

                int h = Integer.numberOfTrailingZeros(~(ints[i] >> j));
                int hm = (h == 32) ? -1 : (1 << h) - 1;
                int mask = hm << j;

                int w = 1;
                while (i + w < brick.dim) {
                    int nextH = (ints[i + w] >> j) & hm;
                    if (nextH != hm) {
                        break;
                    }
                    ints[i + w] &= ~mask;
                    w++;
                }
                quads.add(new WakeRenderer.WakeQuad(i, j, w, h, getFromArea(i, j, w, h, brick)));
                j += h;
            }
        }
        return quads;
    }

    private static WakeNode[][] getFromArea(int x, int z, int w, int h, Brick brick) {
        WakeNode[][] nodes = new WakeNode[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                System.out.printf("%d+%d=%d, %d+%d=%d\n", x, i, x+i, z, j, z+j);
                nodes[i][j] = brick.get(x + i, z + j);
            }
        }
        return nodes;
    }
}
