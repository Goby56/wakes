package com.goby56.wakes.event;

import com.goby56.wakes.config.WakesConfig;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import java.util.Random;

public class PickBoat implements ClientPickBlockGatherCallback {
    @Override
    public ItemStack pick(PlayerEntity player, HitResult result) {
        if (WakesConfig.pickBoat) {
            if (player.raycast(5, 0, false).getType().equals(HitResult.Type.BLOCK)) return ItemStack.EMPTY;
            if (player.raycast(5, 0, true) instanceof BlockHitResult fluidHit &&
                    fluidHit.getType().equals(HitResult.Type.BLOCK)) {
                if (player.getWorld().getFluidState(fluidHit.getBlockPos()).isOf(Fluids.WATER)) {
                    var boatTypes = BoatEntity.Type.values();
                    for (BoatEntity.Type boatType : boatTypes) {
                        ItemStack stack = getBoatFromType(boatType);
                        if (player.getInventory().contains(stack)) {
                            return stack;
                        }
                    }
                    if (player.isCreative()) {
                        int i = new Random().nextInt(boatTypes.length);
                        return getBoatFromType(boatTypes[i]);
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getBoatFromType(BoatEntity.Type type) {
        String waterCraft = type == BoatEntity.Type.BAMBOO ? "raft" : "boat";
        return new ItemStack(Registries.ITEM.get(new Identifier(type.toString() + "_" + waterCraft)));

    }
}
