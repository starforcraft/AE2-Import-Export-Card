package com.ultramega.ae2importexportcard.mixin;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.ids.AEComponents;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocators;
import appeng.util.ConfigInventory;
import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.compat.ae2wtlib.Ae2WtlibUtils;
import com.ultramega.ae2importexportcard.registry.ModDataComponents;
import com.ultramega.ae2importexportcard.registry.ModItems;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Mixin(WirelessTerminalItem.class)
public abstract class MixinWirelessTerminalItem extends Item {
    @Unique
    private Future<ICraftingPlan> ae2importExportCard$craftingJob;

    public MixinWirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // TODO: This method can definitely be improved to be more efficient and readable, let's see if I will ever do it

        if (level.isClientSide())
            return;
        if (!(entity instanceof ServerPlayer player))
            return;

        // Check if in access point range
        IGrid grid = null;
        WirelessTerminalMenuHost<?> host = null;

        if (stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem) {
            if (AE2ImportExportCard.AE2WTLIB_INSTALLED) {
                grid = Ae2WtlibUtils.getGridFromStack(wirelessTerminalItem, player, stack);
            }
            if (grid == null) {
                grid = wirelessTerminalItem.getLinkedGrid(stack, level, null);
            }

            if (wirelessTerminalItem.getMenuHost(player, MenuLocators.forStack(stack), null) instanceof WirelessTerminalMenuHost<?> menuHost) {
                host = menuHost;
            }
        }

        // Check if out of range
        if (grid == null) return;

        ItemContainerContents upgrades = stack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack upgrade = upgrades.getStackInSlot(i);
            boolean isImportUpgrade = upgrade.getItem() == ModItems.IMPORT_CARD.get();
            boolean isExportUpgrade = upgrade.getItem() == ModItems.EXPORT_CARD.get();

            if (isImportUpgrade || isExportUpgrade) {
                IConfigManager configManager = IConfigManager.builder(upgrade)
                        .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
                        .build();

                int[] selectedInventorySlots = upgrade.getOrDefault(ModDataComponents.SELECTED_INVENTORY_SLOTS, new IntArrayList(new int[36])).toIntArray();
                IUpgradeInventory upgradeInventory = UpgradeInventories.forItem(upgrade, 3, null);
                boolean invertFilter = upgradeInventory.isInstalled(AEItems.INVERTER_CARD);

                FuzzyMode fuzzyMode = configManager.getSetting(Settings.FUZZY_MODE);

                for (int j = 0; j < selectedInventorySlots.length; j++) {
                    if (selectedInventorySlots[j] >= 1) {
                        ItemStack itemInInventory = player.getInventory().getItem(j);

                        if ((isExportUpgrade || itemInInventory.getItem() != Items.AIR) && itemInInventory != stack) {
                            // Get filters
                            ConfigInventory filterConfig = ConfigInventory.configTypes(18).changeListener(null).build();
                            filterConfig.readFromChildTag(upgrade.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag()), "", player.registryAccess());

                            if (host == null) return;

                            var node = host.getActionableNode();
                            if (node == null) return;

                            IActionSource source = new PlayerSource(player);

                            if (isImportUpgrade) {
                                AEKey what = AEItemKey.of(itemInInventory);
                                if (what != null && grid.getStorageService() != null) {
                                    // Import Fluids
                                    for (int index = 0; index < filterConfig.size(); index++) {
                                        GenericStack filter = filterConfig.getStack(index);
                                        if (filter != null && filter.what() instanceof AEFluidKey) {
                                            var cap = itemInInventory.getCapability(Capabilities.FluidHandler.ITEM);
                                            if (cap != null) {
                                                FluidStack fluidStack = cap.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                                                if (fluidStack.isEmpty()) continue;

                                                AEFluidKey aeFluidKey = AEFluidKey.of(fluidStack);
                                                if (aeFluidKey == null) continue;

                                                long amount = StorageHelper.poweredInsert(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), aeFluidKey, fluidStack.getAmount(), source, Actionable.SIMULATE);
                                                if (amount <= 0) continue;

                                                cap.drain((int) amount, IFluidHandler.FluidAction.EXECUTE);
                                                StorageHelper.poweredInsert(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), aeFluidKey, amount, source, Actionable.MODULATE);
                                                player.getInventory().setItem(j, cap.getContainer());
                                                player.containerMenu.broadcastChanges();
                                            }
                                        }
                                    }

                                    // Import Items
                                    if (invertFilter != filterConfig.getAvailableStacks().findFuzzy(what, fuzzyMode).stream().anyMatch(filterKeyEntry -> upgradeInventory.isInstalled(AEItems.FUZZY_CARD) ? what.fuzzyEquals(filterKeyEntry.getKey(), fuzzyMode) : what.equals(filterKeyEntry.getKey()))) {
                                        long amount = StorageHelper.poweredInsert(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), what, itemInInventory.getCount(), source, Actionable.SIMULATE);
                                        if (amount <= 0) continue;

                                        StorageHelper.poweredInsert(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), what, itemInInventory.getCount(), source, Actionable.MODULATE);
                                        player.getInventory().setItem(j, ItemStack.EMPTY);
                                        player.containerMenu.broadcastChanges();
                                    }
                                }
                            } else {
                                for (int index = 0; index < filterConfig.size(); index++) {
                                    GenericStack filter = filterConfig.getStack(index);
                                    if (filter == null) continue;
                                    if (index != selectedInventorySlots[j] - 1) continue;

                                    IItemHandler playerInventory = player.getCapability(Capabilities.ItemHandler.ENTITY);
                                    if (playerInventory != null) {
                                        AEItemKey what = AEItemKey.of(itemInInventory.getItem());
                                        boolean acceptsFluid = false;

                                        var cap = itemInInventory.getCapability(Capabilities.FluidHandler.ITEM);
                                        if (cap != null) {
                                            acceptsFluid = true;
                                        }

                                        if (acceptsFluid || itemInInventory.isEmpty() || (upgradeInventory.isInstalled(AEItems.FUZZY_CARD) ? what.fuzzyEquals(filter.what(), fuzzyMode) : what.equals(filter.what()))) {
                                            AEKey toExportKey = null;
                                            if (upgradeInventory.isInstalled(AEItems.FUZZY_CARD)) {
                                                var fuzzy = grid.getStorageService().getCachedInventory()
                                                        .findFuzzy(filter.what(), fuzzyMode)
                                                        .stream().findFirst();
                                                if (fuzzy.isPresent()) {
                                                    toExportKey = fuzzy.get().getKey();
                                                }
                                            } else {
                                                toExportKey = filter.what();
                                            }
                                            if (toExportKey != null) {
                                                if (toExportKey instanceof AEItemKey fuzzyItem) {
                                                    int extractAmount = Math.min(itemInInventory.getMaxStackSize() - itemInInventory.getCount(), itemInInventory.getMaxStackSize());
                                                    int stackInteractionSize = upgradeInventory.isInstalled(AEItems.SPEED_CARD) ? 64 : 1;
                                                    int size = Math.min(stackInteractionSize, extractAmount);

                                                    if (size <= 0) continue;

                                                    long extracted = StorageHelper.poweredExtraction(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), toExportKey, size, source, Actionable.SIMULATE);
                                                    if (extracted <= 0) {
                                                        if (upgradeInventory.isInstalled(AEItems.CRAFTING_CARD)) {
                                                            var craftingService = grid.getCraftingService();
                                                            if (craftingService.isCraftable(filter.what()) && craftingService.getRequestedAmount(filter.what()) <= 0) {
                                                                var src = new MachineSource(grid::getPivot);

                                                                if (this.ae2importExportCard$craftingJob != null) {
                                                                    try {
                                                                        ICraftingPlan job = null;
                                                                        if (this.ae2importExportCard$craftingJob.isDone()) {
                                                                            job = this.ae2importExportCard$craftingJob.get();
                                                                        }

                                                                        // Check if job is complete
                                                                        if (job != null) {
                                                                            craftingService.submitJob(job, null, null, false, src);

                                                                            this.ae2importExportCard$craftingJob = null;
                                                                        }
                                                                    } catch (InterruptedException | ExecutionException ignored) {
                                                                    }
                                                                }

                                                                this.ae2importExportCard$craftingJob = craftingService.beginCraftingCalculation(level, () -> src, filter.what(), size, CalculationStrategy.CRAFT_LESS);
                                                            }
                                                        }

                                                        continue;
                                                    }

                                                    final ItemStack toInsert = fuzzyItem.toStack((int) extracted);
                                                    final ItemStack inserted = playerInventory.insertItem(j, toInsert, true);
                                                    if (inserted.isEmpty()) { // toInsert was accepted
                                                        StorageHelper.poweredExtraction(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), toExportKey, size, source, Actionable.MODULATE);
                                                        playerInventory.insertItem(j, toInsert, false);
                                                        player.containerMenu.broadcastChanges();
                                                    }
                                                } else if (acceptsFluid && toExportKey instanceof AEFluidKey fuzzyFluid) {
                                                    int stackInteractionSize = upgradeInventory.isInstalled(AEItems.SPEED_CARD) ? AEFluidKey.AMOUNT_BUCKET * 64 : AEFluidKey.AMOUNT_BUCKET;

                                                    long extracted = StorageHelper.poweredExtraction(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), toExportKey, stackInteractionSize, source, Actionable.SIMULATE);
                                                    if (extracted <= 0) {
                                                        continue;
                                                    }

                                                    int amount = cap.fill(fuzzyFluid.toStack((int) extracted), IFluidHandler.FluidAction.SIMULATE);
                                                    if (amount <= 0) {
                                                        continue;
                                                    }

                                                    StorageHelper.poweredExtraction(new ActionHostEnergySource(host), grid.getStorageService().getInventory(), toExportKey, amount, source, Actionable.MODULATE);
                                                    cap.fill(fuzzyFluid.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
                                                    player.getInventory().setItem(j, cap.getContainer());
                                                    player.containerMenu.broadcastChanges();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
