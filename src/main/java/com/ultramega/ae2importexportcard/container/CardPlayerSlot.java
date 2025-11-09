package com.ultramega.ae2importexportcard.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CardPlayerSlot extends Slot {
    private boolean cancelPickup = false;

    public CardPlayerSlot(Container inventory, int invSlot) {
        super(inventory, invSlot, 0, 0);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        if (this.cancelPickup) {
            this.cancelPickup = false;
            return false;
        } else {
            return true;
        }
    }

    public void setCancelPickup(boolean cancelPickup) {
        this.cancelPickup = cancelPickup;
    }
}
