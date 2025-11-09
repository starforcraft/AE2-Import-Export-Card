package com.ultramega.ae2importexportcard.item;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.registry.ModDataComponents;
import com.ultramega.ae2importexportcard.util.CardConfigManager;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.ids.AEComponents;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class UpgradeHost implements IConfigurableObject {
    public final ConfigInventory filterConfig = ConfigInventory.configTypes(18)
            .changeListener(this::updateFilter)
            .build();

    private IConfigManager configManager;

    private final UpgradeType type;
    private final Player player;
    private final ItemStack itemStack;
    private ItemStack upgradeStack;

    public UpgradeHost(UpgradeType type, int id, Inventory inventory, WirelessTerminalMenuHost<?> host) {
        this.type = type;
        this.player = inventory.player;
        this.itemStack = host.getItemStack();

        ItemContainerContents upgrades = this.itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(type, stack)) {
                this.upgradeStack = stack;

                this.configManager = CardConfigManager.builder(type, this.itemStack, stack)
                        .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
                        .build();

                this.filterConfig.readFromChildTag(stack.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag()), "", this.player.registryAccess());
            }
        }
    }

    private void updateFilter() {
        ItemContainerContents upgrades = this.itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(this.type, stack)) {
                CompoundTag tag = stack.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag());
                this.filterConfig.writeToChildTag(tag, "", this.player.registryAccess());
                stack.set(ModDataComponents.FILTER_CONFIG, tag);
            }
            stacks.add(stack);
        }

        this.itemStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(stacks));
    }

    public void setSelectedInventorySlots(int[] selectedInventorySlots) {
        ItemContainerContents upgrades = this.itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(this.type, stack)) {
                stack.set(ModDataComponents.SELECTED_INVENTORY_SLOTS, new IntArrayList(selectedInventorySlots));
            }
            stacks.add(stack);
        }

        this.itemStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(stacks));
    }

    public int[] getSelectedInventorySlots() {
        ItemContainerContents upgrades = this.itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(this.type, stack)) {
                return stack.getOrDefault(ModDataComponents.SELECTED_INVENTORY_SLOTS, new IntArrayList(new int[36])).toIntArray();
            }
        }

        return new int[36];
    }

    public IUpgradeInventory getUpgrades() {
        return UpgradeInventories.forItem(this.upgradeStack, this.type == UpgradeType.EXPORT ? 3 : 2, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        if (upgrades instanceof AppEngInternalInventory internalInventory) {
            stack.set(AEComponents.UPGRADES, internalInventory.toItemContainerContents());
        }
        ItemContainerContents itemContainerContents = this.itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        ArrayList<ItemStack> newUpgrades = new ArrayList<>();
        for (int i = 0; i < itemContainerContents.getSlots(); i++) {
            ItemStack currentStack = itemContainerContents.getStackInSlot(i);

            if (currentStack.is(stack.getItem())) {
                newUpgrades.add(stack);
            } else {
                newUpgrades.add(currentStack);
            }
        }
        this.itemStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(newUpgrades));
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }
}
