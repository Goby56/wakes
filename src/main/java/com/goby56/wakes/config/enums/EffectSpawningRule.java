package com.goby56.wakes.config.enums;

public enum EffectSpawningRule {
    SIMULATION_AND_PLANES(true, true),
    ONLY_SIMULATION(true, false),
    ONLY_PLANES(false, true),
    DISABLED(false, false);

    public final boolean simulateWakes;
    public final boolean renderPlanes;

    EffectSpawningRule(boolean simulateWakes, boolean renderPlanes) {
        this.simulateWakes = simulateWakes;
        this.renderPlanes = renderPlanes;
    }
}
