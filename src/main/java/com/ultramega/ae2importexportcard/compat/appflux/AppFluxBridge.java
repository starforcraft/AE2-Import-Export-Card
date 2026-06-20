package com.ultramega.ae2importexportcard.compat.appflux;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.util.ConfigInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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

    public static void importEnergyFromItem(ServerPlayer player,
                                            IGrid grid,
                                            ActionHostEnergySource energySource,
                                            IActionSource source,
                                            int inventorySlot,
                                            ItemStack itemInInventory,
                                            ConfigInventory filterConfig,
                                            FuzzyMode fuzzyMode,
                                            boolean fuzzy,
                                            boolean invertFilter) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return;
        }

        try {
            AppFluxEnergyCompat.importEnergyFromItem(player, grid, energySource, source, inventorySlot,
                itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
        } catch (LinkageError ignored) {
        }
    }

    public static boolean canAcceptEnergy(ServerPlayer player, int inventorySlot, ItemStack stack, AEKey chemicalKey, long amount) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return false;
        }

        try {
            return AppFluxEnergyCompat.canAcceptEnergy(player, inventorySlot, stack, chemicalKey, amount);
        } catch (LinkageError ignored) {
            return false;
        }
    }

    public static boolean exportEnergyToItem(ServerPlayer player,
                                             IGrid grid,
                                             ActionHostEnergySource energySource,
                                             IActionSource source,
                                             int inventorySlot,
                                             ItemStack itemInInventory,
                                             AEKey chemicalKey,
                                             long amount) {
        if (!AE2ImportExportCard.APPFLUX_INSTALLED) {
            return false;
        }

        try {
            return AppFluxEnergyCompat.exportEnergyToItem(player, grid, energySource, source, inventorySlot, itemInInventory, chemicalKey, amount);
        } catch (LinkageError ignored) {
            return false;
        }
    }
}
