package com.ultramega.ae2importexportcard.util;

import com.ultramega.ae2importexportcard.registry.ModDataComponents;
import com.ultramega.ae2importexportcard.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

import appeng.api.ids.AEComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class BlockPickerCardConfig {
    public static final int DEFAULT_AMOUNT = 1;
    public static final int MAX_AMOUNT = 64;

    private BlockPickerCardConfig() {
    }

    public static int getAmount(final ItemStack card) {
        return Mth.clamp(card.getOrDefault(ModDataComponents.BLOCK_PICKER_AMOUNT, DEFAULT_AMOUNT), DEFAULT_AMOUNT, MAX_AMOUNT);
    }

    public static int getAmountFromTerminal(final ItemStack terminal) {
        final ItemStack card = findCard(terminal);
        return card.isEmpty() ? DEFAULT_AMOUNT : getAmount(card);
    }

    public static boolean setAmountOnTerminal(final ItemStack terminal, final int amount) {
        final ItemContainerContents upgrades = terminal.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        final List<ItemStack> updatedUpgrades = new ArrayList<>(upgrades.getSlots());
        boolean changed = false;

        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            final ItemStack upgrade = upgrades.getStackInSlot(slot);
            if (upgrade.is(ModItems.BLOCK_PICKER_CARD.get())) {
                upgrade.set(ModDataComponents.BLOCK_PICKER_AMOUNT, Mth.clamp(amount, DEFAULT_AMOUNT, MAX_AMOUNT));
                changed = true;
            }
            updatedUpgrades.add(upgrade);
        }

        if (changed) {
            terminal.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(updatedUpgrades));
        }
        return changed;
    }

    public static ItemStack findCard(final ItemStack terminal) {
        final ItemContainerContents upgrades = terminal.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            final ItemStack upgrade = upgrades.getStackInSlot(slot);
            if (upgrade.is(ModItems.BLOCK_PICKER_CARD.get())) {
                return upgrade;
            }
        }
        return ItemStack.EMPTY;
    }
}
