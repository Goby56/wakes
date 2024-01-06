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

    private static EffectSpawningRule query(boolean simulateWakes, boolean renderPlanes) {
        for (EffectSpawningRule rule : EffectSpawningRule.values()) {
            if (rule.simulateWakes == simulateWakes && rule.renderPlanes == renderPlanes) {
                return rule;
            }
        }
        return DISABLED;
    }

    public EffectSpawningRule mask(EffectSpawningRule rule) {
        return query(simulateWakes && rule.simulateWakes, this.renderPlanes && rule.simulateWakes);
    }
}
