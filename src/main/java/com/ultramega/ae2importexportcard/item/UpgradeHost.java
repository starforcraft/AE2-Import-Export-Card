package com.ultramega.ae2importexportcard.item;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKeyType;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.registry.ModDataComponents;
import com.ultramega.ae2importexportcard.util.CardConfigManager;
import com.ultramega.ae2importexportcard.util.UpgradeType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

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

        ItemContainerContents upgrades = itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(type, stack)) {
                this.upgradeStack = stack;

                this.configManager = CardConfigManager.builder(type, itemStack, stack)
                        .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
                        .build();

                filterConfig.readFromChildTag(stack.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag()), "", player.registryAccess());
            }
        }
    }

    private void updateFilter() {
        ItemContainerContents upgrades = itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(type, stack)) {
                CompoundTag tag = stack.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag());
                filterConfig.writeToChildTag(tag, "", player.registryAccess());
                stack.set(ModDataComponents.FILTER_CONFIG, tag);
            }
            stacks.add(stack);
        }

        itemStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(stacks));
    }

    public void setSelectedInventorySlots(int[] selectedInventorySlots) {
        ItemContainerContents upgrades = itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(type, stack)) {
                stack.set(ModDataComponents.SELECTED_INVENTORY_SLOTS, new IntArrayList(selectedInventorySlots));
            }
            stacks.add(stack);
        }

        itemStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(stacks));
    }

    public int[] getSelectedInventorySlots() {
        ItemContainerContents upgrades = itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (AE2ImportExportCard.isImportOrExportCard(type, stack)) {
                return stack.getOrDefault(ModDataComponents.SELECTED_INVENTORY_SLOTS, new IntArrayList(new int[36])).toIntArray();
            }
        }

        return new int[36];
    }

    public IUpgradeInventory getUpgrades() {
        return UpgradeInventories.forItem(upgradeStack, type == UpgradeType.EXPORT ? 3 : 2, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        if(upgrades instanceof AppEngInternalInventory internalInventory) {
            stack.set(AEComponents.UPGRADES, internalInventory.toItemContainerContents());
        }
        ItemContainerContents itemContainerContents = itemStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        ArrayList<ItemStack> newUpgrades = new ArrayList<>();
        for(int i = 0; i < itemContainerContents.getSlots(); i++) {
            ItemStack currentStack = itemContainerContents.getStackInSlot(i);

            if(currentStack.is(stack.getItem())) {
                newUpgrades.add(stack);
            } else {
                newUpgrades.add(currentStack);
            }
        }
        itemStack.set(AEComponents.UPGRADES, ItemContainerContents.fromItems(newUpgrades));
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }
}
