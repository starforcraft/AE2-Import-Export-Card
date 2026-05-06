/*package com.ultramega.ae2importexportcard.compat.mekanism;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.util.ConfigInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class MekanismBridge {
    private MekanismBridge() {
    }

    public static boolean isChemicalKey(AEKey key) {
        if (!AE2ImportExportCard.MEKANISM_INSTALLED) {
            return false;
        }

        try {
            return MekanismChemicalCompat.isChemicalKey(key);
        } catch (LinkageError ignored) {
            return false;
        }
    }

    public static void importChemicalFromItem(ServerPlayer player,
                                              IGrid grid,
                                              ActionHostEnergySource energySource,
                                              IActionSource source,
                                              int inventorySlot,
                                              ItemStack itemInInventory,
                                              ConfigInventory filterConfig,
                                              FuzzyMode fuzzyMode,
                                              boolean fuzzy,
                                              boolean invertFilter) {
        if (!AE2ImportExportCard.MEKANISM_INSTALLED) {
            return;
        }

        try {
            MekanismChemicalCompat.importChemicalFromItem(player, grid, energySource, source, inventorySlot,
                itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
        } catch (LinkageError ignored) {
        }
    }

    public static boolean canAcceptChemical(ItemStack itemStack, AEKey chemicalKey, long amount) {
        if (!AE2ImportExportCard.MEKANISM_INSTALLED) {
            return false;
        }

        try {
            return MekanismChemicalCompat.canAcceptChemical(itemStack, chemicalKey, amount);
        } catch (LinkageError ignored) {
            return false;
        }
    }

    public static boolean exportChemicalToItem(ServerPlayer player,
                                               IGrid grid,
                                               ActionHostEnergySource energySource,
                                               IActionSource source,
                                               int inventorySlot,
                                               ItemStack itemInInventory,
                                               AEKey chemicalKey,
                                               long amount) {
        if (!AE2ImportExportCard.MEKANISM_INSTALLED) {
            return false;
        }

        try {
            return MekanismChemicalCompat.exportChemicalToItem(player, grid, energySource, source, inventorySlot, itemInInventory, chemicalKey, amount);
        } catch (LinkageError ignored) {
            return false;
        }
    }
}*/
