package com.goby56.wakes.event;

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

import static com.goby56.wakes.WakesClient.CONFIG_INSTANCE;

public class PickBoat implements ClientPickBlockGatherCallback {
    @Override
    public ItemStack pick(PlayerEntity player, HitResult result) {
        if (CONFIG_INSTANCE.pickBoat) {
            if (player.raycast(5, 0, true) instanceof BlockHitResult fluidHit) {
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
        return new ItemStack(Registries.ITEM.get(Identifier.of(type.toString() + "_" + waterCraft)));
    }
}
