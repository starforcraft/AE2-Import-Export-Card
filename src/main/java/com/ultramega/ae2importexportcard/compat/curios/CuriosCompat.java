package com.ultramega.ae2importexportcard.compat.curios;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;
import top.theillusivec4.curios.api.common.inventory.CuriosResourceHandler;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

final class CuriosCompat {
    private CuriosCompat() {
    }

    static boolean isEquipped(Player player, ItemStack stack) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventoryOrNull(player);
        if (inventory == null || stack.isEmpty()) {
            return false;
        }
        for (ICurioStacksHandler stacks : inventory.getCurios().values()) {
            if (!stacks.isVisible()) {
                continue;
            }
            IDynamicStackHandler handler = stacks.getStacks();
            NonNullList<Boolean> active = stacks.getActiveStates();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (i < active.size() && !active.get(i)) {
                    continue;
                }
                if (handler.getStackInSlot(i) == stack) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<CuriosBridge.Slot> getSlots(Player player) {
        List<CuriosBridge.Slot> slots = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            inventory.getCurios().forEach((identifier, stacks) -> {
                if (!stacks.isVisible()) {
                    return;
                }
                CuriosResourceHandler handler = new CuriosResourceHandler(stacks.getStacks());
                Identifier icon = CuriosSlotTypes.getSlotType(identifier, player.level().isClientSide()).getIcon();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    NonNullList<Boolean> active = stacks.getActiveStates();
                    if (i < active.size() && !active.get(i)) {
                        continue;
                    }
                    slots.add(new CuriosBridge.Slot(identifier, i, stacks.getStacks().getStackInSlot(i), handler, icon));
                }
            });
        });
        return slots;
    }
}
