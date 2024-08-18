package com.goby56.wakes.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GreedyMesherTest {
    private record BasicQuad(int x, int z, int w, int h) {

    }

    @Test
    void close2x2() {
        int[] bits = patternToBits("""
                0 0 0 0 0 0
                1 1 0 0 0 0
                1 1 0 0 0 0
                0 0 0 0 0 0
                0 0 0 0 0 0
                0 0 0 0 0 0
                """, 6);
        var quads = mesher(bits, 6);
        assertEquals(1, quads.size());
        var q  = quads.get(0);
        assertEquals(0, q.x);
        assertEquals(3, q.z);
        assertEquals(2, q.w);
        assertEquals(2, q.h);
    }

    @Test
    void far2x3() {
        int[] bits = patternToBits("""
                0 0 0 0 0 0
                0 0 0 0 0 0
                0 0 0 1 1 0
                0 0 0 1 1 0
                0 0 0 1 1 0
                0 0 0 0 0 0
                """, 6);
        var quads = mesher(bits, 6);
        assertEquals(1, quads.size());
        var q  = quads.get(0);
        assertEquals(3, q.x);
        assertEquals(1, q.z);
        assertEquals(2, q.w);
        assertEquals(3, q.h);
    }

    @Test
    void complex() {
        int[] bits = patternToBits("""
                0 0 0 0 0 0
                1 1 1 0 0 0
                0 0 1 0 0 0
                0 0 0 1 1 0
                1 0 0 1 1 0
                1 0 1 0 0 0
                """, 6);
        var quads = mesher(bits, 6);
        assertEquals(5, quads.size());
        var q  = quads.get(0);
        assertEquals(0, q.x);
        assertEquals(0, q.z);
        assertEquals(1, q.w);
        assertEquals(2, q.h);
        q  = quads.get(1);
        assertEquals(0, q.x);
        assertEquals(4, q.z);
        assertEquals(3, q.w);
        assertEquals(1, q.h);
        q  = quads.get(2);
        assertEquals(2, q.x);
        assertEquals(0, q.z);
        assertEquals(1, q.w);
        assertEquals(1, q.h);
        q  = quads.get(3);
        assertEquals(2, q.x);
        assertEquals(3, q.z);
        assertEquals(1, q.w);
        assertEquals(1, q.h);
        q  = quads.get(4);
        assertEquals(3, q.x);
        assertEquals(1, q.z);
        assertEquals(2, q.w);
        assertEquals(2, q.h);
    }

    public static int[] patternToBits(String pattern, int dim) {
        List<String> rows = Arrays.stream(pattern.split("\n")).map(r -> r.replaceAll("\\s", "")).toList();

        int[] bits = new int[dim];
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                int val = Integer.parseInt(String.valueOf(rows.get(dim - j - 1).charAt(i)));
                bits[i] |= val << j;
            }
        }
        return bits;
    }

    @Test
    void patternConverter() {
        String pattern = """
                1 0 0 0 0 1
                1 1 0 1 0 1
                1 1 0 0 0 0
                0 0 0 1 0 0
                0 1 1 0 1 0
                0 1 0 0 1 1
                """;
        int[] bits = patternToBits(pattern, 6);
        assertEquals(56, bits[0]);
        assertEquals(27, bits[1]);
        assertEquals(2, bits[2]);
        assertEquals(20, bits[3]);
        assertEquals(3, bits[4]);
        assertEquals(49, bits[5]);
    }

    private static ArrayList<BasicQuad> mesher(int[] ints, int dim) {
        ArrayList<BasicQuad> quads = new ArrayList<>();
        for (int i = 0; i < dim; i++) {
            int j = 0;
            while (j < dim) {
                j += Integer.numberOfTrailingZeros(ints[i] >>> j);
                if (j >= dim) continue;

                int h = Integer.numberOfTrailingZeros(~(ints[i] >>> j));

                int hm = (h == 32) ? ~0 : (1 << h) - 1;
                int mask = hm << j;

                int w = 1;
                while (i + w < dim) {
                    int nextH = (ints[i + w] >>> j) & hm;
                    if (nextH != hm) {
                        break;
                    }
                    ints[i + w] &= ~mask;
                    w++;
                }
                quads.add(new BasicQuad(i, j, w, h));
                j += h;
            }
        }
        return quads;
    }
}
