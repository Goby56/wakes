package com.goby56.wakes.particle;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import com.goby56.wakes.particle.custom.WakeParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static SplashPlaneParticleType SPLASH_PLANE;
    public static WakeParticleType WAKE_PARTICLE;

    public static void registerParticles() {
        SPLASH_PLANE = Registry.register(Registries.PARTICLE_TYPE, new Identifier(WakesClient.MOD_ID, "splash_plane"), new SplashPlaneParticleType(true));
        ParticleFactoryRegistry.getInstance().register(SPLASH_PLANE, SplashPlaneParticle.Factory::new);

        WAKE_PARTICLE = Registry.register(Registries.PARTICLE_TYPE, new Identifier(WakesClient.MOD_ID, "wake_particle"), new WakeParticleType(true));
        ParticleFactoryRegistry.getInstance().register(WAKE_PARTICLE, WakeParticle.Factory::new);
    }
}
