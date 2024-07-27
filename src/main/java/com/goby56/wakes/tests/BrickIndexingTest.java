package com.goby56.wakes.tests;

import com.goby56.wakes.render.WakeQuad;
import com.goby56.wakes.simulation.Brick;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BrickIndexingTest {
    // @Test
    // void brickBitMaskCreation() {
    //     Brick brick = new Brick(0, 0);
    //     brick.insert(new WakeNode(0, 2));
    //     brick.insert(new WakeNode(0, 3));
    //     brick.insert(new WakeNode(1, 2));
    //     brick.insert(new WakeNode(1, 3));

    //     assertEquals(805306368, brick.bitMask[0]);
    //     assertEquals(805306368, brick.bitMask[1]);
    // }

    // @Test
    // void nodeFromQuadRetrieval() {
    //     Brick brick = new Brick(0, 0);
    //     brick.insert(new WakeNode(0, 2));
    //     brick.insert(new WakeNode(0, 3));
    //     brick.insert(new WakeNode(1, 2));
    //     brick.insert(new WakeNode(1, 3));

    //     brick.generateMesh();
    //     assertEquals(1, brick.quads.size());
    //     WakeQuad quad = brick.quads.get(0);
    //     assertEquals(0, quad.x);
    //     assertEquals(2, quad.z);
    //     assertEquals(2, quad.w);
    //     assertEquals(2, quad.h);

    //     for (int i = 0; i < quad.h; i++) {
    //         for (int j = 0; j < quad.w; j++) {
    //             WakeNode node = quad.nodes[i][j];
    //             assertEquals(quad.x + j, node.x);
    //             assertEquals(quad.z + i, node.z);
    //         }
    //     }
    // }
}
