package com.ultramega.ae2insertexportcard.compat;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.util.ConfigInventory;
import com.ultramega.ae2insertexportcard.compat.appbot.AppliedBotanicsCompat;
import com.ultramega.ae2insertexportcard.compat.appmek.AppliedMekanisticsCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class AddonStorageBridge {
    private AddonStorageBridge() {
    }

    public static boolean importFromItem(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IActionSource source, int inventorySlot, ItemStack itemStack, ConfigInventory filterConfig,
            FuzzyMode fuzzyMode, boolean fuzzyInstalled, boolean invertFilter) {
        if (ModList.get().isLoaded("appmek")) {
            try {
                if (AppliedMekanisticsCompat.importFromItem(player, grid, energySource, source, inventorySlot,
                        itemStack, filterConfig, fuzzyMode, fuzzyInstalled, invertFilter)) {
                    return true;
                }
            } catch (LinkageError ignored) {
            }
        }
        if (ModList.get().isLoaded("appbot")) {
            try {
                return AppliedBotanicsCompat.importFromItem(player, grid, energySource, source, inventorySlot,
                        itemStack, filterConfig, fuzzyMode, fuzzyInstalled, invertFilter);
            } catch (LinkageError ignored) {
            }
        }
        return false;
    }

    public static boolean isSupportedKey(AEKey key) {
        if (ModList.get().isLoaded("appmek")) {
            try {
                if (AppliedMekanisticsCompat.isKey(key)) {
                    return true;
                }
            } catch (LinkageError ignored) {
            }
        }
        if (ModList.get().isLoaded("appbot")) {
            try {
                return AppliedBotanicsCompat.isKey(key);
            } catch (LinkageError ignored) {
            }
        }
        return false;
    }

    public static boolean exportToItem(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IActionSource source, int inventorySlot, ItemStack itemStack, AEKey key, long requestedAmount) {
        if (ModList.get().isLoaded("appmek")) {
            try {
                if (AppliedMekanisticsCompat.isKey(key)) {
                    return AppliedMekanisticsCompat.exportToItem(player, grid, energySource, source, inventorySlot,
                            itemStack, key, requestedAmount);
                }
            } catch (LinkageError ignored) {
            }
        }
        if (ModList.get().isLoaded("appbot")) {
            try {
                if (AppliedBotanicsCompat.isKey(key)) {
                    return AppliedBotanicsCompat.exportToItem(player, grid, energySource, source, inventorySlot,
                            itemStack, key, requestedAmount);
                }
            } catch (LinkageError ignored) {
            }
        }
        return false;
    }
}
