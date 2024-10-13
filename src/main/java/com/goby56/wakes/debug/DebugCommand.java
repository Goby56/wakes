package com.goby56.wakes.debug;

import com.goby56.wakes.particle.ModParticles;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DebugCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("wakes_debug")
                .then(ClientCommandManager.literal("light")
                        .executes(DebugCommand::lightCoordinate))
                .then(ClientCommandManager.literal("color")
                        .executes(DebugCommand::waterColor))
                .then(ClientCommandManager.literal("spawn")
                        .then(ClientCommandManager.literal("node")
                                .then(ClientCommandManager.argument("flood_level", IntegerArgumentType.integer(0, 5))
                                        .executes(DebugCommand::spawnWakeNode)))));
                        //.then(ClientCommandManager.literal("splash_cloud_particle")
                                //.executes(DebugCommand::spawnSplashCloudParticle)))));
    }

    public static int spawnWakeNode(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        WakeHandler wakeHandler = WakeHandler.getInstance().orElse(null);
        if (wakeHandler == null) return 0;
        HitResult result = cmdCtx.getSource().getPlayer().raycast(10, 0, true);
        Vec3d pos = result.getPos();
        if (!result.getType().equals(HitResult.Type.BLOCK)) return 0;
        if (!cmdCtx.getSource().getWorld().getFluidState(new BlockPos((int) pos.x, (int) Math.floor(pos.y), (int) pos.z)).isIn(FluidTags.WATER)) return 0;
        WakeNode node = new WakeNode(result.getPos(), 100);
        node.floodLevel = cmdCtx.getArgument("flood_level", Integer.class);
        wakeHandler.insert(node);
        return 1;
    }


    public static int lightCoordinate(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        World world = cmdCtx.getSource().getWorld();
        BlockPos blockPos = cmdCtx.getSource().getPlayer().getBlockPos();

        cmdCtx.getSource().sendFeedback(Text.of(String.valueOf(WorldRenderer.getLightmapCoordinates(world, blockPos))));
        return 1;
    }

    public static int waterColor(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        World world = cmdCtx.getSource().getWorld();
        BlockPos blockPos = cmdCtx.getSource().getPlayer().getBlockPos();

        int col = BiomeColors.getWaterColor(world, blockPos);
        cmdCtx.getSource().sendFeedback(Text.of(String.format("(%d, %d, %d)",
                ColorHelper.Argb.getRed(col),
                ColorHelper.Argb.getGreen(col),
                ColorHelper.Argb.getBlue(col))));
        return 1;
    }

    public static int spawnSplashCloudParticle(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        Vec3d pos = cmdCtx.getSource().getPosition();
        cmdCtx.getSource().getWorld().addParticle(ModParticles.SPLASH_CLOUD, pos.x, pos.y, pos.z, 0, 0, 0);
        return 1;
    }
}
