package com.ultramega.ae2insertexportcard.mixin;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
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
import appeng.core.definitions.AEItems;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import appeng.util.ConfigInventory;
import com.ultramega.ae2insertexportcard.AE2InsertExportCard;
import com.ultramega.ae2insertexportcard.item.UpgradeHost;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Mixin(WirelessTerminalItem.class)
public class MixinWirelessTerminalItem extends Item {
    @Unique
    private Future<ICraftingPlan> ae2insertExportCard$craftingJob;

    public MixinWirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide())
            return;
        if (!(entity instanceof ServerPlayer player))
            return;

        if(!stack.hasTag() || level.isClientSide)
            return;

        //Check if in access point range
        boolean inRange = false;
        IGrid grid;
        WirelessTerminalMenuHost host = null;

        if(stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem) {
            grid = wirelessTerminalItem.getLinkedGrid(stack, level, player);

            if(wirelessTerminalItem.getMenuHost(player, slotId, stack, null) instanceof WirelessTerminalMenuHost menuHost) {
                host = menuHost;
                inRange = host.rangeCheck();
            }
        } else {
            grid = null;
        }

        if(!inRange) return;

        if(stack.getTag().contains("upgrades") && grid != null) {
            ListTag tagList = stack.getTag().getList("upgrades", Tag.TAG_COMPOUND);

            for (int i = 0; i < tagList.size(); i++) {
                boolean isInsertUpgrade = tagList.getCompound(i).getString("id").equals(new ResourceLocation(AE2InsertExportCard.MOD_ID, "insert_card").toString());
                boolean isExportUpgrade = tagList.getCompound(i).getString("id").equals(new ResourceLocation(AE2InsertExportCard.MOD_ID, "export_card").toString());
                int slot = tagList.getCompound(i).getInt("Slot");
                CompoundTag tag = (CompoundTag) tagList.getCompound(i).get("tag");

                if(tag != null && (isInsertUpgrade || isExportUpgrade)) {
                    ItemStack upgrade = host.getUpgrades().getStackInSlot(slot);
                    int[] selectedInventorySlots = tag.getIntArray(UpgradeHost.NBT_SELECTED_INVENTORY_SLOTS);
                    IUpgradeInventory upgrades = UpgradeInventories.forItem(upgrade, 3, null);
                    boolean invertFilter = upgrades.isInstalled(AEItems.INVERTER_CARD);

                    FuzzyMode fuzzyMode;
                    final String fz = upgrade.getOrCreateTag().getString("fuzzy_mode");
                    try {
                        fuzzyMode = FuzzyMode.valueOf(fz);
                    } catch (Throwable t) {
                        fuzzyMode = FuzzyMode.IGNORE_ALL;
                    }

                    for(int j = 0; j < selectedInventorySlots.length; j++) {
                        if(selectedInventorySlots[j] >= 1) {
                            ItemStack itemInInventory = player.getInventory().getItem(j);

                            if((isExportUpgrade || itemInInventory.getItem() != Items.AIR) && itemInInventory != stack) {
                                // Get filters
                                ConfigInventory filterConfig = ConfigInventory.configTypes(18, null);
                                filterConfig.readFromChildTag(tag, "filterConfig");

                                var node = host.getActionableNode();
                                if(node == null) return;

                                IActionSource source = new PlayerSource(player);

                                if (isInsertUpgrade) {
                                    AEKey what = AEItemKey.of(itemInInventory);
                                    if(what != null && grid.getStorageService() != null) {
                                        // TODO: This (whole class) can definitely be improved to be more efficient and readable but this will be the last update for 1.20.1 soo this will never happen like me getting bitc~

                                        // Import Fluids
                                        for(int index = 0; index < filterConfig.size(); index++) {
                                            GenericStack filter = filterConfig.getStack(index);
                                            if(filter != null && filter.what() instanceof AEFluidKey) {
                                                itemInInventory.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent((fluidItem -> {
                                                    FluidStack fluidStack = fluidItem.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                                                    if(fluidStack.isEmpty()) return;

                                                    AEFluidKey aeFluidKey = AEFluidKey.of(fluidStack);
                                                    if(aeFluidKey == null) return;

                                                    long amount = StorageHelper.poweredInsert(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), aeFluidKey, fluidStack.getAmount(), source, Actionable.SIMULATE);
                                                    if (amount <= 0) return;

                                                    fluidItem.drain((int)amount, IFluidHandler.FluidAction.EXECUTE);
                                                    StorageHelper.poweredInsert(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), aeFluidKey, amount, source, Actionable.MODULATE);
                                                    player.containerMenu.broadcastChanges();
                                                }));
                                            }
                                        }

                                        // Import Items
                                        final FuzzyMode finalFuzzyMode = fuzzyMode;
                                        if(invertFilter != filterConfig.getAvailableStacks().findFuzzy(what, fuzzyMode).stream().anyMatch(filterKeyEntry -> upgrades.isInstalled(AEItems.FUZZY_CARD) ? what.fuzzyEquals(filterKeyEntry.getKey(), finalFuzzyMode) : what.equals(filterKeyEntry.getKey()))) {
                                            long amount = StorageHelper.poweredInsert(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), what, itemInInventory.getCount(), source, Actionable.SIMULATE);
                                            if (amount <= 0) continue;

                                            StorageHelper.poweredInsert(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), what, itemInInventory.getCount(), source, Actionable.MODULATE);
                                            player.getInventory().setItem(j, ItemStack.EMPTY);
                                            player.containerMenu.broadcastChanges();
                                        }
                                    }
                                } else {
                                    for(int index = 0; index < filterConfig.size(); index++) {
                                        GenericStack filter = filterConfig.getStack(index);
                                        if (filter == null) continue;
                                        if (index != selectedInventorySlots[j] - 1) continue;

                                        Optional<IItemHandler> playerInventory = player.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve();
                                        if (playerInventory.isPresent()) {
                                            AEItemKey what = AEItemKey.of(itemInInventory.getItem());
                                            boolean acceptsFluid = false;

                                            var cap = itemInInventory.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).cast();
                                            if(cap.isPresent()) {
                                                acceptsFluid = true;
                                            }

                                            if(acceptsFluid || itemInInventory.isEmpty() || (upgrades.isInstalled(AEItems.FUZZY_CARD) ? what.fuzzyEquals(filter.what(), fuzzyMode) : what.equals(filter.what()))) {
                                                AEKey toExportKey;
                                                if(upgrades.isInstalled(AEItems.FUZZY_CARD)) {
                                                    var fuzzy = grid.getStorageService().getCachedInventory()
                                                            .findFuzzy(filter.what(), fuzzyMode)
                                                            .stream().findFirst();
                                                    toExportKey = fuzzy.map(Map.Entry::getKey).orElse(null);
                                                } else {
                                                    toExportKey = filter.what();
                                                }
                                                if(toExportKey != null) {
                                                    if(!acceptsFluid && toExportKey instanceof AEItemKey fuzzyItem) {
                                                        int extractAmount = Math.min(itemInInventory.getMaxStackSize() - itemInInventory.getCount(), itemInInventory.getMaxStackSize());
                                                        int stackInteractionSize = upgrades.isInstalled(AEItems.SPEED_CARD) ? 64 : 1;
                                                        int size = Math.min(stackInteractionSize, extractAmount);

                                                        if(size <= 0) continue;

                                                        long extracted = StorageHelper.poweredExtraction(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), toExportKey, size, source, Actionable.MODULATE);
                                                        if(extracted <= 0) {
                                                            if(upgrades.isInstalled(AEItems.CRAFTING_CARD)) {
                                                                var craftingService = grid.getCraftingService();
                                                                if(craftingService.isCraftable(filter.what()) && craftingService.getRequestedAmount(filter.what()) <= 0) {
                                                                    var src = new MachineSource(grid::getPivot);

                                                                    if(ae2insertExportCard$craftingJob != null) {
                                                                        try {
                                                                            ICraftingPlan job = null;
                                                                            if (ae2insertExportCard$craftingJob.isDone()) {
                                                                                job = ae2insertExportCard$craftingJob.get();
                                                                            }

                                                                            // Check if job is complete
                                                                            if (job != null) {
                                                                                craftingService.submitJob(job, null, null, false, src);

                                                                                this.ae2insertExportCard$craftingJob = null;
                                                                            }
                                                                        } catch (InterruptedException | ExecutionException ignored) {
                                                                        }
                                                                    }

                                                                    this.ae2insertExportCard$craftingJob = craftingService.beginCraftingCalculation(level, () -> src, filter.what(), size, CalculationStrategy.CRAFT_LESS);
                                                                }
                                                            }

                                                            continue;
                                                        }

                                                        playerInventory.get().insertItem(j, fuzzyItem.toStack((int) extracted), false);
                                                        player.containerMenu.broadcastChanges();
                                                    } else if(acceptsFluid && toExportKey instanceof AEFluidKey fuzzyFluid) {
                                                        int stackInteractionSize = upgrades.isInstalled(AEItems.SPEED_CARD) ? AEFluidKey.AMOUNT_BUCKET * 64 : AEFluidKey.AMOUNT_BUCKET;

                                                        long extracted = StorageHelper.poweredExtraction(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), toExportKey, stackInteractionSize, source, Actionable.SIMULATE);
                                                        if(extracted <= 0) {
                                                            continue;
                                                        }

                                                        cap.ifPresent((o -> {
                                                            if(o instanceof IFluidHandlerItem fluidItem) {
                                                                int amount = fluidItem.fill(fuzzyFluid.toStack((int) extracted), IFluidHandler.FluidAction.SIMULATE);
                                                                if(amount <= 0) {
                                                                    return;
                                                                }

                                                                StorageHelper.poweredExtraction(new ChannelPowerSrc(node, grid.getEnergyService()), grid.getStorageService().getInventory(), toExportKey, amount, source, Actionable.MODULATE);
                                                                fluidItem.fill(fuzzyFluid.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
                                                                player.containerMenu.broadcastChanges();
                                                            }
                                                        }));
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
}
