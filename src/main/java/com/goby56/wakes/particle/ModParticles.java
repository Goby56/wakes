package com.goby56.wakes.particle;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.particle.custom.SplashCloudParticle;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class ModParticles {
    public static WithOwnerParticleType SPLASH_PLANE;
    public static WithOwnerParticleType SPLASH_CLOUD;

    public static void registerParticles() {
        SPLASH_PLANE = Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath(WakesClient.MOD_ID, "splash_plane"), new WithOwnerParticleType(true));
        ParticleFactoryRegistry.getInstance().register(SPLASH_PLANE, SplashPlaneParticle.Factory::new);

        SPLASH_CLOUD = Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocation.fromNamespaceAndPath(WakesClient.MOD_ID, "splash_cloud"), new WithOwnerParticleType(true));
        ParticleFactoryRegistry.getInstance().register(SPLASH_CLOUD, SplashCloudParticle.Factory::new);
    }
}
