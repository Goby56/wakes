package com.goby56.wakes.particle;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.particle.custom.WakeParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static WakeParticleType WAKE_SPLASH;

    public static void registerParticles() {
        WAKE_SPLASH = Registry.register(Registry.PARTICLE_TYPE, new Identifier(WakesClient.MOD_ID, "wake"), new WakeParticleType(true));
        ParticleFactoryRegistry.getInstance().register(WAKE_SPLASH, WakeParticle.Factory::new);
    }
}
