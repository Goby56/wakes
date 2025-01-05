package com.goby56.wakes.debug;

import com.goby56.wakes.event.WakeWorldTicker;
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
import net.minecraft.world.World;

import java.util.Optional;

public class DebugCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("wakes")
                .then(ClientCommandManager.literal("debug")
                        .then(ClientCommandManager.literal("light")
                                .executes(DebugCommand::lightCoordinate))
                        .then(ClientCommandManager.literal("color")
                                .executes(DebugCommand::waterColor))));
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