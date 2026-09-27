package com.example.factions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

class CuriosIntegration {
    static void enforce(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable stacksHandler = handler.getEquippedCurios();

            for (int i = 0; i < stacksHandler.getSlots(); i++) {
                ItemStack stack = stacksHandler.getStackInSlot(i);
                if (stack.isEmpty()) continue;

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (FactionManager.canUse(player, id)) continue;

                stacksHandler.setStackInSlot(i, ItemStack.EMPTY);
                if (!player.getInventory().add(stack.copy())) {
                    player.drop(stack.copy(), false);
                }
                FactionEventHandler.blockedMessage(player, id);
            }
        });
    }
}
