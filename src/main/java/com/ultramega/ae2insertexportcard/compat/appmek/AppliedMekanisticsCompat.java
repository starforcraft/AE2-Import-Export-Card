package com.ultramega.ae2insertexportcard.compat.appmek;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.util.ConfigInventory;
import com.ultramega.ae2insertexportcard.compat.ContainerTransferHelper;
import me.ramidzkh.mekae2.ae2.ChemicalContainerItemStrategy;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AppliedMekanisticsCompat {
    private static final ChemicalContainerItemStrategy STRATEGY = new ChemicalContainerItemStrategy();

    private AppliedMekanisticsCompat() {
    }

    public static boolean isKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    public static boolean importFromItem(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IActionSource source, int inventorySlot, ItemStack itemStack, ConfigInventory filterConfig,
            FuzzyMode fuzzyMode, boolean fuzzyInstalled, boolean invertFilter) {
        return ContainerTransferHelper.importFromItem(STRATEGY, MekanismKey.class, player, grid, energySource,
                source, inventorySlot, itemStack, filterConfig, fuzzyMode, fuzzyInstalled, invertFilter);
    }

    public static boolean exportToItem(ServerPlayer player, IGrid grid, IEnergySource energySource,
            IActionSource source, int inventorySlot, ItemStack itemStack, AEKey key, long requestedAmount) {
        return ContainerTransferHelper.exportToItem(STRATEGY, MekanismKey.class, player, grid, energySource,
                source, inventorySlot, itemStack, key, requestedAmount);
    }
}
