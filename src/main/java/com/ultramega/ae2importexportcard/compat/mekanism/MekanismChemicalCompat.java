package com.ultramega.ae2importexportcard.compat.mekanism;

import com.ultramega.ae2importexportcard.util.AEKeyFilterUtil;

import javax.annotation.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageHelper;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.util.ConfigInventory;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;

public final class MekanismChemicalCompat {
    private static final ResourceLocation CHEMICAL_HANDLER_ID = ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "chemical_handler");
    private static final ItemCapability<IChemicalHandler, Void> CHEMICAL_ITEM_CAPABILITY = ItemCapability.createVoid(CHEMICAL_HANDLER_ID, IChemicalHandler.class);

    private MekanismChemicalCompat() {
    }

    public static boolean isChemicalKey(AEKey key) {
        return key instanceof MekanismKey;
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
        var chemicalHandler = getChemicalHandler(itemInInventory);
        if (chemicalHandler == null) {
            return;
        }

        ChemicalStack simulatedExtract = chemicalHandler.extractChemical(Long.MAX_VALUE, Action.SIMULATE);
        if (simulatedExtract.isEmpty()) {
            return;
        }

        MekanismKey chemicalKey = MekanismKey.of(simulatedExtract);
        if (chemicalKey == null || !AEKeyFilterUtil.passesFilter(chemicalKey, filterConfig, fuzzyMode, fuzzy, invertFilter)) {
            return;
        }

        long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), chemicalKey,
            simulatedExtract.getAmount(), source, Actionable.SIMULATE);
        if (insertable <= 0) {
            return;
        }

        ChemicalStack actuallyExtracted = chemicalHandler.extractChemical(chemicalKey.withAmount(insertable), Action.EXECUTE);
        if (actuallyExtracted.isEmpty()) {
            return;
        }

        MekanismKey actuallyExtractedKey = MekanismKey.of(actuallyExtracted);
        if (actuallyExtractedKey == null) {
            return;
        }

        long inserted = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), actuallyExtractedKey,
            actuallyExtracted.getAmount(), source, Actionable.MODULATE);
        if (inserted < actuallyExtracted.getAmount()) {
            long remainder = actuallyExtracted.getAmount() - inserted;
            chemicalHandler.insertChemical(actuallyExtracted.copyWithAmount(remainder), Action.EXECUTE);
        }

        player.getInventory().setItem(inventorySlot, itemInInventory);
        player.containerMenu.broadcastChanges();
    }

    public static boolean exportChemicalToItem(ServerPlayer player,
                                               IGrid grid,
                                               ActionHostEnergySource energySource,
                                               IActionSource source,
                                               int inventorySlot,
                                               ItemStack itemInInventory,
                                               AEKey chemicalKey,
                                               long amount) {
        ChemicalInsertTarget target = getChemicalInsertTarget(itemInInventory, chemicalKey, amount);
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

        ChemicalStack insertRemainder = target.handler().insertChemical(target.key().withAmount(extracted), Action.EXECUTE);
        long inserted = extracted - insertRemainder.getAmount();
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

    public static boolean canAcceptChemical(ItemStack itemStack, AEKey chemicalKey, long amount) {
        return getChemicalInsertTarget(itemStack, chemicalKey, amount) != null;
    }

    @Nullable
    private static ChemicalInsertTarget getChemicalInsertTarget(ItemStack itemStack, AEKey chemicalKey, long amount) {
        if (!(chemicalKey instanceof MekanismKey mekanismKey)) {
            return null;
        }

        IChemicalHandler chemicalHandler = getChemicalHandler(itemStack);
        if (chemicalHandler == null) {
            return null;
        }

        ChemicalStack toInsert = mekanismKey.withAmount(amount);
        ChemicalStack remainder = chemicalHandler.insertChemical(toInsert, Action.SIMULATE);
        long insertableAmount = toInsert.getAmount() - remainder.getAmount();
        if (insertableAmount <= 0) {
            return null;
        }

        return new ChemicalInsertTarget(mekanismKey, chemicalHandler, insertableAmount);
    }

    @Nullable
    private static IChemicalHandler getChemicalHandler(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        return itemStack.getCapability(CHEMICAL_ITEM_CAPABILITY);
    }

    private record ChemicalInsertTarget(MekanismKey key, IChemicalHandler handler, long insertableAmount) {
    }
}
