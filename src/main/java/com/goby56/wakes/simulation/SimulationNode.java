package com.goby56.wakes.simulation;

import com.goby56.wakes.config.WakesConfig;
import com.goby56.wakes.render.WakeColor;
import com.goby56.wakes.utils.WakesUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public abstract class SimulationNode {
    public float[][][] u;
    public float[][] initialValues;
    public final int res;

    public SimulationNode() {
        this.res = WakeHandler.resolution.res;
        this.u = new float[3][res+2][res+2];
        this.initialValues = new float[res+2][res+2];
    }

    public void setInitialValue(long pos, int val) {
        float resFactor = res / 16f;
        int[] xz = WakesUtils.longAsPos(pos);
        if (xz[0] < 0) xz[0] += res;
        if (xz[1] < 0) xz[1] += res;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                this.initialValues[xz[1]+i+1][xz[0]+j+1] = val * resFactor;
            }
        }
    }

    public int getPixelColor(int x, int z, int fluidCol, int lightCol, float opacity) {
        float waveEqAvg = (this.u[0][z + 1][x + 1] + this.u[1][z + 1][x + 1] + this.u[2][z + 1][x + 1]) / 3;
        if (WakesConfig.debugColors) {
            int clampedRange = (int) (255 * (2 / (1 + Math.exp(-0.1 * waveEqAvg)) - 1));
            return new WakeColor(Math.max(-clampedRange, 0), Math.max(clampedRange, 0), 0, 255).abgr;
        }
        return WakeColor.sampleColor(waveEqAvg, fluidCol, lightCol, opacity);
    }

    public abstract void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST);

    public static class WakeSimulation extends SimulationNode {

        @Override
        public void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            float time = 20f; // ticks
            // TODO CHANGE "16" TO ACTUAL RES? MAYBE?
            float alpha = (float) Math.pow(WakesConfig.wavePropagationFactor * 16f / time, 2);
            float beta = (float) (Math.log(10 * WakesConfig.waveDecayFactor + 10) / Math.log(20)); // Logarithmic scale

            for (int i = 2; i >= 1; i--) {
                if (NORTH != null) this.u[i][0] = NORTH.u[i][res];
                if (SOUTH != null) this.u[i][res+1] = SOUTH.u[i][1];
                for (int z = 0; z < res+2; z++) {
                    if (EAST == null && WEST == null) break;
                    if (EAST != null) this.u[i][z][res+1] = EAST.u[i][z][1];
                    if (WEST != null) this.u[i][z][0] = WEST.u[i][z][res];
                }
            }

            for (int z = 1; z < res+1; z++) {
                for (int x = 1; x < res+1; x++) {
                    this.u[0][z][x] += this.initialValues[z][x];
                    this.initialValues[z][x] = 0;

                    this.u[2][z][x] = this.u[1][z][x];
                    this.u[1][z][x] = this.u[0][z][x];
                }
            }

            for (int z = 1; z < res+1; z++) {
                for (int x = 1; x < res+1; x++) {
                    this.u[0][z][x] = (float) (alpha * (0.5*u[1][z-1][x] + 0.25*u[1][z-1][x+1] + 0.5*u[1][z][x+1]
                            + 0.25*u[1][z+1][x+1] + 0.5*u[1][z+1][x] + 0.25*u[1][z+1][x-1]
                            + 0.5*u[1][z][x-1] + 0.25*u[1][z-1][x-1] - 3*u[1][z][x])
                            + 2*u[1][z][x] - u[2][z][x]);
                    this.u[0][z][x] *= beta;
                }
            }
        }
    }

    public static class SplashPlaneSimulation extends SimulationNode {

        @Override
        public void tick(@Nullable Float velocity, @Nullable SimulationNode NORTH, @Nullable SimulationNode SOUTH, @Nullable SimulationNode EAST, @Nullable SimulationNode WEST) {
            double t = System.currentTimeMillis() / (double) 1000;
            if (velocity == null) return;
            int p = (int) (14 * Math.min(1f, 2 * velocity / WakesConfig.maxSplashPlaneVelocity));
            for (int z = 1; z < res+1; z++) {
                for (int x = 1; x < res+1; x++) {
                    this.u[0][z][x] = 0;
                    double v = Math.atan((z - 16f) / x);
                    double d = Math.sqrt(Math.pow(z - 16, 2) + Math.pow(x, 2)) + 0.5 * Math.sin(10 * v - 2 * Math.PI * t);

                    if (d < p) {
                        this.u[0][z][x] = (float) (200 * Math.pow(d - p, 2) / (d*d));
                    }
                }
            }
        }
    }
}
