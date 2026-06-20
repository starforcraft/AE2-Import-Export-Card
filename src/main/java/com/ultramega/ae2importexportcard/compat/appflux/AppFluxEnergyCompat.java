package com.ultramega.ae2importexportcard.compat.appflux;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
        EnergyHandler energyHandler = getEnergyHandler(player, itemInInventory, inventorySlot);
        if (energyHandler == null) {
            return;
        }

        FluxKey energyKey = FluxKey.of(EnergyType.FE);
        if (!AEKeyFilterUtil.passesFilter(energyKey, filterConfig, fuzzyMode, fuzzy, invertFilter)) {
            return;
        }

        long stored = energyHandler.getAmountAsLong();
        if (stored <= 0) {
            return;
        }

        int storedInt = toIntAmount(stored);
        if (storedInt <= 0) {
            return;
        }

        long simulatedExtract;
        try (Transaction tx = Transaction.openRoot()) {
            simulatedExtract = energyHandler.extract(storedInt, tx);
        }
        if (simulatedExtract <= 0) {
            return;
        }

        long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), energyKey, simulatedExtract, source, Actionable.SIMULATE);
        if (insertable <= 0) {
            return;
        }

        int amountToMove = toIntAmount(Math.min(simulatedExtract, insertable));
        if (amountToMove <= 0) {
            return;
        }

        try (Transaction tx = Transaction.openRoot()) {
            long extracted = energyHandler.extract(amountToMove, tx);
            if (extracted <= 0) {
                return;
            }

            long inserted = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), energyKey, extracted, source, Actionable.MODULATE);
            if (inserted <= 0) {
                return;
            }

            if (inserted < extracted) {
                int remainder = (int) (extracted - inserted);
                int returned = energyHandler.insert(remainder, tx);

                if (returned != remainder) {
                    return;
                }
            }

            tx.commit();
        }
    }

    public static boolean exportEnergyToItem(ServerPlayer player,
                                             IGrid grid,
                                             ActionHostEnergySource energySource,
                                             IActionSource source,
                                             int inventorySlot,
                                             ItemStack itemInInventory,
                                             AEKey energyKey,
                                             long amount) {
        EnergyInsertTarget target = getEnergyInsertTarget(player, inventorySlot, itemInInventory, energyKey, amount);
        if (target == null) {
            return false;
        }

        long extractable = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), target.key(),
            target.insertableAmount(), source, Actionable.SIMULATE);
        if (extractable <= 0) {
            return false;
        }

        long amountToMove = Math.min(extractable, target.insertableAmount());
        if (amountToMove <= 0) {
            return false;
        }

        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), target.key(), amountToMove, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return false;
        }

        try (Transaction tx = Transaction.openRoot()) {
            int extractedForItem = Math.toIntExact(extracted);
            long inserted = target.handler().insert(extractedForItem, tx);
            if (inserted <= 0) {
                StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), target.key(), extracted, source, Actionable.MODULATE);
                return false;
            }

            if (inserted < extracted) {
                long notInserted = extracted - inserted;
                StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), target.key(), notInserted, source, Actionable.MODULATE);
            }

            tx.commit();
        }

        return true;
    }

    public static boolean canAcceptEnergy(ServerPlayer player, int inventorySlot, ItemStack stack, AEKey chemicalKey, long amount) {
        return getEnergyInsertTarget(player, inventorySlot, stack, chemicalKey, amount) != null;
    }

    @Nullable
    private static EnergyInsertTarget getEnergyInsertTarget(ServerPlayer player, int inventorySlot, ItemStack stack, AEKey energyKey, long amount) {
        if (!(energyKey instanceof FluxKey fluxKey) || amount <= 0) {
            return null;
        }

        int amountInt = toIntAmount(amount);
        if (amountInt <= 0) {
            return null;
        }

        EnergyHandler energyHandler = getEnergyHandler(player, stack, inventorySlot);
        if (energyHandler == null) {
            return null;
        }

        long insertableAmount;
        try (Transaction tx = Transaction.openRoot()) {
            insertableAmount = energyHandler.insert(amountInt, tx);
        }
        if (insertableAmount <= 0) {
            return null;
        }

        return new EnergyInsertTarget(fluxKey, energyHandler, insertableAmount);
    }

    @Nullable
    private static EnergyHandler getEnergyHandler(ServerPlayer player, ItemStack stack, int inventorySlot) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forPlayerSlot(player, inventorySlot));
    }

    private static int toIntAmount(long amount) {
        if (amount <= 0) {
            return 0;
        }
        return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    private record EnergyInsertTarget(FluxKey key, EnergyHandler handler, long insertableAmount) {
    }
}
