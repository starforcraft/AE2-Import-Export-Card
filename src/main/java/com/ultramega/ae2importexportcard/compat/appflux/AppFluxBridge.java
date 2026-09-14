package com.ultramega.ae2importexportcard.compat.appflux;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public final class AppFluxBridge {
    private AppFluxBridge() {
    }

    public static boolean isFluxKey(AEKey key) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return false;
        }

        try {
            return AppFluxEnergyCompat.isFluxKey(key);
        } catch (LinkageError ignored) {
            return false;
        }
    }

    public static void importEnergyFromItem(IGrid grid,
                                            ActionHostEnergySource energySource,
                                            IActionSource source,
                                            ItemAccess itemAccess,
                                            ItemStack itemInInventory,
                                            ConfigInventory filterConfig,
                                            FuzzyMode fuzzyMode,
                                            boolean fuzzy,
                                            boolean invertFilter) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return;
        }

        try {
            AppFluxEnergyCompat.importEnergyFromItem(grid, energySource, source, itemAccess, itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
        } catch (LinkageError ignored) {
        }
    }

    public static boolean canAcceptEnergy(ItemAccess itemAccess, ItemStack stack, AEKey chemicalKey, long amount) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return false;
        }

        try {
            return AppFluxEnergyCompat.canAcceptEnergy(itemAccess, stack, chemicalKey, amount);
        } catch (LinkageError ignored) {
            return false;
        }
    }

    public static boolean exportEnergyToItem(IGrid grid,
                                             ActionHostEnergySource energySource,
                                             IActionSource source,
                                             ItemAccess itemAccess,
                                             ItemStack itemInInventory,
                                             AEKey chemicalKey,
                                             long amount) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return false;
        }

        try {
            return AppFluxEnergyCompat.exportEnergyToItem(grid, energySource, source, itemAccess, itemInInventory, chemicalKey, amount);
        } catch (LinkageError ignored) {
            return false;
        }
    }
}
