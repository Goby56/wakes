package com.goby56.wakes.particle;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.particle.custom.SplashCloudParticle;
import com.goby56.wakes.particle.custom.SplashPlaneParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModParticles {
    public static WithOwnerParticleType SPLASH_PLANE;
    public static DefaultParticleType SPLASH_CLOUD = FabricParticleTypes.simple();

    public static void registerParticles() {
        SPLASH_PLANE = Registry.register(Registry.PARTICLE_TYPE, new Identifier(WakesClient.MOD_ID, "splash_plane"), new WithOwnerParticleType(true));
        ParticleFactoryRegistry.getInstance().register(SPLASH_PLANE, SplashPlaneParticle.Factory::new);

        Registry.register(Registry.PARTICLE_TYPE, new Identifier(WakesClient.MOD_ID, "splash_cloud"), SPLASH_CLOUD);
        ParticleFactoryRegistry.getInstance().register(SPLASH_CLOUD, SplashCloudParticle.Factory::new);
    }
}
