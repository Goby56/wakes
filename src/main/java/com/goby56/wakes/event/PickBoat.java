package com.goby56.wakes.event;

import com.goby56.wakes.config.WakesConfig;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BoatItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PickBoat implements PlayerPickItemEvents.PickItemFromBlock, PlayerPickItemEvents.PickItemFromEntity {
    public ItemStack pick(PlayerEntity player) {
        if (WakesConfig.pickBoat) {
            if (player.raycast(5, 0, false).getType().equals(HitResult.Type.BLOCK)) return ItemStack.EMPTY;
            if (player.raycast(5, 0, true) instanceof BlockHitResult fluidHit &&
                    fluidHit.getType().equals(HitResult.Type.BLOCK)) {
                if (player.getWorld().getFluidState(fluidHit.getBlockPos()).isOf(Fluids.WATER)) {
                    PlayerInventory inv = player.getInventory();
                    for (int i = 0; i < inv.size(); i++) {
                        if (inv.getStack(i).getItem() instanceof BoatItem) {
                            return inv.getStack(i);
                        }
                    }
                    if (player.isCreative()) {
                        return new ItemStack(Items.OAK_BOAT);
                    }
                }
            }
        }
        // RETURN NULL MAYBE
        return ItemStack.EMPTY;
    }

    @Override
    public @Nullable ItemStack onPickItemFromBlock(ServerPlayerEntity serverPlayerEntity, BlockPos blockPos, BlockState blockState, boolean b) {
        return pick(serverPlayerEntity);
    }

    @Override
    public @Nullable ItemStack onPickItemFromEntity(ServerPlayerEntity serverPlayerEntity, Entity entity, boolean b) {
        return pick(serverPlayerEntity);
    }
}
