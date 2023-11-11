package com.goby56.wakes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpawnWakesCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("spawnwake")
                .then(ClientCommandManager.argument("size", IntegerArgumentType.integer(1, 100))
                .executes(SpawnWakesCommand::run)));
    }

    public static int run(CommandContext<FabricClientCommandSource> cmdCtx) throws CommandSyntaxException {
        Vec3d pos = cmdCtx.getSource().getPosition();
        int size = cmdCtx.getArgument("size", Integer.class);
        World world = cmdCtx.getSource().getWorld();
        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        Float waterHeight = null;
        for (int y = 0; y < 10; y++) {
            float f = 0.0f;
            blockPos.set(pos.x, pos.y - y, pos.z);
            FluidState fluidState = world.getFluidState(blockPos);
            if (fluidState.isIn(FluidTags.WATER)) {
                f = Math.max(f, fluidState.getHeight(world, blockPos));
            }
            if (f >= 1.0f) continue;
            waterHeight = blockPos.getY() + f;
        }
        if (waterHeight == null) {
            cmdCtx.getSource().sendFeedback(Text.of("No water nearby"));
            return -1;
        }

//        WakeNode wakeNode = new WakeNode(new Vec3d(pos.x, waterHeight, pos.z));
//        WakeHandler.getInstance().insert(wakeNode);
//        for (int x = -size; x < size; x++) {
//            for (int z = -size; z < size; z++) {
//                WakeNode wakeNode = new WakeNode(new Vec3d(pos.x + x, waterHeight, pos.z + z));
//                WakeHandler.getInstance().insert(wakeNode);
//            }
//        }

        cmdCtx.getSource().sendFeedback(Text.of(String.format("Spawning %d wake nodes", (int) Math.pow(2 * size, 2))));
        return 1;
    }
}
