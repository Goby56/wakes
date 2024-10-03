package com.goby56.wakes.debug;

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
    }

    public static int spawnWakeNode(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        HitResult result = cmdCtx.getSource().getPlayer().raycast(10, 0, true);
        if (!result.getType().equals(HitResult.Type.BLOCK)) return 0;
        WakeNode node = new WakeNode(result.getPos(), 100);
        node.floodLevel = cmdCtx.getArgument("flood_level", Integer.class);
        WakeHandler.getInstance().insert(node);
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
}
