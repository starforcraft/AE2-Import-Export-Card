package com.ultramega.ae2insertexportcard.compat;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageHelper;
import appeng.util.ConfigInventory;
import com.ultramega.ae2insertexportcard.util.AEKeyFilterUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ContainerTransferHelper {
    private ContainerTransferHelper() {
    }

    public static <K extends AEKey, C> boolean importFromItem(ContainerItemStrategy<K, C> strategy,
            Class<K> keyClass, ServerPlayer player, IGrid grid, IEnergySource energySource, IActionSource source,
            int inventorySlot, ItemStack itemStack, ConfigInventory filterConfig, FuzzyMode fuzzyMode,
            boolean fuzzyInstalled, boolean invertFilter) {
        GenericStack content = strategy.getContainedStack(itemStack);
        if (content == null || content.amount() <= 0 || !keyClass.isInstance(content.what())) {
            return false;
        }

        K key = keyClass.cast(content.what());
        if (!AEKeyFilterUtil.passesFilter(key, filterConfig, fuzzyMode, fuzzyInstalled, invertFilter)) {
            return false;
        }

        C context = strategy.findPlayerSlotContext(player, inventorySlot);
        if (context == null) {
            return false;
        }

        long extractable = strategy.extract(context, key, content.amount(), Actionable.SIMULATE);
        if (extractable <= 0) {
            return false;
        }

        long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), key,
                extractable, source, Actionable.SIMULATE);
        if (insertable <= 0) {
            return false;
        }

        long extracted = strategy.extract(context, key, Math.min(extractable, insertable), Actionable.MODULATE);
        if (extracted <= 0) {
            return false;
        }

        long inserted = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), key,
                extracted, source, Actionable.MODULATE);
        if (inserted < extracted) {
            strategy.insert(context, key, extracted - inserted, Actionable.MODULATE);
        }

        if (inserted > 0) {
            player.getInventory().setItem(inventorySlot, itemStack);
            player.containerMenu.broadcastChanges();
            return true;
        }
        return false;
    }

    public static <K extends AEKey, C> boolean exportToItem(ContainerItemStrategy<K, C> strategy,
            Class<K> keyClass, ServerPlayer player, IGrid grid, IEnergySource energySource, IActionSource source,
            int inventorySlot, ItemStack itemStack, AEKey key, long requestedAmount) {
        if (!keyClass.isInstance(key) || requestedAmount <= 0) {
            return false;
        }

        K typedKey = keyClass.cast(key);
        C context = strategy.findPlayerSlotContext(player, inventorySlot);
        if (context == null) {
            return false;
        }

        long insertable = strategy.insert(context, typedKey, requestedAmount, Actionable.SIMULATE);
        if (insertable <= 0) {
            return false;
        }

        long extractable = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(),
                typedKey, insertable, source, Actionable.SIMULATE);
        if (extractable <= 0) {
            return false;
        }

        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(),
                typedKey, Math.min(insertable, extractable), source, Actionable.MODULATE);
        if (extracted <= 0) {
            return false;
        }

        long inserted = strategy.insert(context, typedKey, extracted, Actionable.MODULATE);
        if (inserted < extracted) {
            StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), typedKey,
                    extracted - inserted, source, Actionable.MODULATE);
        }

        if (inserted > 0) {
            player.getInventory().setItem(inventorySlot, itemStack);
            player.containerMenu.broadcastChanges();
            return true;
        }
        return false;
    }
}
