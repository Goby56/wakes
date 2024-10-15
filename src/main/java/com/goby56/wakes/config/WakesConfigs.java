package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.config.enums.Resolution;
import com.goby56.wakes.render.enums.WakeColor;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedEnumMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class WakesConfigs extends Config {
    public WakesConfigs() {
        super(Identifier.of(WakesClient.MOD_ID, "fzzy_configs"));
    }

    public float wakeOpacity = 1f;
    public boolean spawnParticles = true;
    public Resolution wakeResolution = Resolution.SIXTEEN;
    public boolean firstPersonSplashPlane = false;
    public boolean pickBoat = true;
    public boolean disableMod = false;

    public Customization customization = new Customization();
    public static class Customization extends ConfigSection {
        public Customization() {
            super();
        }

        public ValidatedInt highlight = new ValidatedInt(0, 20, 0);

        public ValidatedMap<Integer, ValidatedColor.ColorHolder> colorIntervals = new ValidatedMap<>(
                new LinkedHashMap<>(),
                new ValidatedInt(0, 50, -50),
                new ValidatedColor(100, 100, 200, 200)
        );
    }

    public Debug debug = new Debug();
    public static class Debug extends ConfigSection {
        public Debug() {
            super();
        }

        // public RenderType renderType = RenderType.AUTO;
        public boolean drawDebugBoxes = false;
        public boolean showDebugInfo = false;
        public int floodFillDistance = 2;
        public int ticksBeforeFill = 2;
        public float shaderLightPassthrough = 0.5f;

        public Simulation simulation = new Simulation();
        public static class Simulation extends ConfigSection {
            public Simulation() {
                super();
            }

            public float wavePropagationFactor = 0.95f;
            public float waveDecayFactor = 0.5f;
            public int initialStrength = 20;
            public int paddleStrength = 100;
            public int splashStrength = 100;
        }
    }
}
