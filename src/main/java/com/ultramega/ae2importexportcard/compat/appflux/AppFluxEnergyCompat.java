/*package com.ultramega.ae2importexportcard.compat.appflux;

import com.ultramega.ae2importexportcard.util.AEKeyFilterUtil;

import javax.annotation.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.util.ConfigInventory;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AppFluxEnergyCompat {
    private AppFluxEnergyCompat() {
    }

    public static boolean isFluxKey(AEKey key) {
        return key instanceof FluxKey;
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
        ILongEnergyStorage energyHandler = getEnergyHandler(itemInInventory);
        if (energyHandler == null || !energyHandler.canExtract()) {
            return;
        }

        FluxKey energyKey = FluxKey.of(EnergyType.FE);
        if (!AEKeyFilterUtil.passesFilter(energyKey, filterConfig, fuzzyMode, fuzzy, invertFilter)) {
            return;
        }

        long stored = energyHandler.getAmount();
        if (stored <= 0) {
            return;
        }

        long simulatedExtract = energyHandler.extract(stored, true);
        if (simulatedExtract <= 0) {
            return;
        }

        long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), energyKey, simulatedExtract, source, Actionable.SIMULATE);
        if (insertable <= 0) {
            return;
        }

        long actuallyExtracted = energyHandler.extract(insertable, false);
        if (actuallyExtracted <= 0) {
            return;
        }

        long inserted = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), energyKey, actuallyExtracted, source, Actionable.MODULATE);
        if (inserted < actuallyExtracted) {
            long remainder = actuallyExtracted - inserted;

            if (remainder > 0 && energyHandler.canReceive()) {
                energyHandler.receive(remainder, false);
            }
        }

        player.getInventory().setItem(inventorySlot, itemInInventory);
        player.containerMenu.broadcastChanges();
    }

    public static boolean exportEnergyToItem(ServerPlayer player,
                                             IGrid grid,
                                             ActionHostEnergySource energySource,
                                             IActionSource source,
                                             int inventorySlot,
                                             ItemStack itemInInventory,
                                             AEKey energyKey,
                                             long amount) {
        EnergyInsertTarget target = getEnergyInsertTarget(itemInInventory, energyKey, amount);
        if (target == null) {
            return false;
        }

        long extractable = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), target.key(),
            target.insertableAmount(), source, Actionable.SIMULATE);
        if (extractable <= 0) {
            return false;
        }

        long amountToMove = Math.min(extractable, target.insertableAmount());
        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), target.key(), amountToMove, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return false;
        }

        long inserted = target.handler().receive(extracted, false);
        if (inserted < extracted) {
            long notInserted = extracted - inserted;
            StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), target.key(), notInserted, source, Actionable.MODULATE);
        }

        if (inserted <= 0) {
            return false;
        }

        player.getInventory().setItem(inventorySlot, itemInInventory);
        player.containerMenu.broadcastChanges();

        return true;
    }

    public static boolean canAcceptEnergy(ItemStack itemStack, AEKey chemicalKey, long amount) {
        return getEnergyInsertTarget(itemStack, chemicalKey, amount) != null;
    }

    @Nullable
    private static EnergyInsertTarget getEnergyInsertTarget(ItemStack itemStack, AEKey energyKey, long amount) {
        if (!(energyKey instanceof FluxKey fluxKey)) {
            return null;
        }

        ILongEnergyStorage energyHandler = getEnergyHandler(itemStack);
        if (energyHandler == null || !energyHandler.canReceive() || amount <= 0) {
            return null;
        }

        long insertableAmount = energyHandler.receive(amount, true);
        if (insertableAmount <= 0) {
            return null;
        }

        return new EnergyInsertTarget(fluxKey, energyHandler, insertableAmount);
    }

    @Nullable
    private static ILongEnergyStorage getEnergyHandler(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        return itemStack.getCapability(ILongEnergyStorage.ITEM);
    }

    private record EnergyInsertTarget(FluxKey key, ILongEnergyStorage handler, long insertableAmount) {
    }
}
*/