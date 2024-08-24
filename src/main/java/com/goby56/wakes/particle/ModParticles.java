package com.goby56.wakes.particle;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.particle.custom.SplashCloudParticle;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static WithOwnerParticleType SPLASH_PLANE;
    public static WithOwnerParticleType SPLASH_CLOUD;

    public static void registerParticles() {
        SPLASH_PLANE = Registry.register(Registries.PARTICLE_TYPE, Identifier.of(WakesClient.MOD_ID, "splash_plane"), new WithOwnerParticleType(true));
        ParticleFactoryRegistry.getInstance().register(SPLASH_PLANE, SplashPlaneParticle.Factory::new);

        SPLASH_CLOUD = Registry.register(Registries.PARTICLE_TYPE, Identifier.of(WakesClient.MOD_ID, "splash_cloud"), new WithOwnerParticleType(true));
        ParticleFactoryRegistry.getInstance().register(SPLASH_CLOUD, SplashCloudParticle.Factory::new);
    }
}
