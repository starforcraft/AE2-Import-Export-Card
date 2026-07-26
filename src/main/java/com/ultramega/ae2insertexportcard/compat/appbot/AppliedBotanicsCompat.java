package com.ultramega.ae2insertexportcard.compat.appbot;

import appbot.ae2.ManaContainerItemStrategy;
import appbot.ae2.ManaKey;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.util.ConfigInventory;
import com.ultramega.ae2insertexportcard.compat.ContainerTransferHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AppliedBotanicsCompat {
    private static final ManaContainerItemStrategy STRATEGY = new ManaContainerItemStrategy();

    private AppliedBotanicsCompat() {
    }

    public static boolean isKey(AEKey key) {
        return key instanceof ManaKey;
    }

    public static boolean importFromItem(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IActionSource source, int inventorySlot, ItemStack itemStack, ConfigInventory filterConfig,
            FuzzyMode fuzzyMode, boolean fuzzyInstalled, boolean invertFilter) {
        return ContainerTransferHelper.importFromItem(STRATEGY, ManaKey.class, player, grid, energySource,
                source, inventorySlot, itemStack, filterConfig, fuzzyMode, fuzzyInstalled, invertFilter);
    }

    public static boolean exportToItem(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IActionSource source, int inventorySlot, ItemStack itemStack, AEKey key, long requestedAmount) {
        return ContainerTransferHelper.exportToItem(STRATEGY, ManaKey.class, player, grid, energySource,
                source, inventorySlot, itemStack, key, requestedAmount);
    }
}
