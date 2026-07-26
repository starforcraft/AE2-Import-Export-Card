package com.ultramega.ae2insertexportcard;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.ultramega.ae2insertexportcard.network.NetworkHandler;
import com.ultramega.ae2insertexportcard.registry.ClientEventHandler;
import com.ultramega.ae2insertexportcard.registry.ModItems;
import com.ultramega.ae2insertexportcard.registry.RegistryHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AE2InsertExportCard.MOD_ID)
public class AE2InsertExportCard {
    public static final String MOD_ID = "ae2insertexportcard";

    public static final NetworkHandler NETWORK_HANDLER = new NetworkHandler();
    private static final java.util.Map<net.minecraft.world.item.ItemStack,
            java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan>> CRAFTING_JOBS =
                    java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public AE2InsertExportCard() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientEventHandler::new);

        RegistryHandler.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        Upgrades.add(ModItems.INSERT_CARD.get(), AEItems.WIRELESS_TERMINAL, 1);
        Upgrades.add(ModItems.EXPORT_CARD.get(), AEItems.WIRELESS_TERMINAL, 1);
        Upgrades.add(ModItems.INSERT_CARD.get(), AEItems.WIRELESS_CRAFTING_TERMINAL, 1);
        Upgrades.add(ModItems.EXPORT_CARD.get(), AEItems.WIRELESS_CRAFTING_TERMINAL, 1);
        if (net.minecraftforge.fml.ModList.get().isLoaded("ae2wtlib")) {
            Upgrades.add(ModItems.INSERT_CARD.get(), de.mari_023.ae2wtlib.AE2wtlib.UNIVERSAL_TERMINAL, 1);
            Upgrades.add(ModItems.EXPORT_CARD.get(), de.mari_023.ae2wtlib.AE2wtlib.UNIVERSAL_TERMINAL, 1);
        }

        Upgrades.add(AEItems.FUZZY_CARD, ModItems.INSERT_CARD.get(), 1);
        Upgrades.add(AEItems.FUZZY_CARD, ModItems.EXPORT_CARD.get(), 1);
        Upgrades.add(AEItems.INVERTER_CARD, ModItems.INSERT_CARD.get(), 1);
        Upgrades.add(AEItems.CRAFTING_CARD, ModItems.EXPORT_CARD.get(), 1);
        Upgrades.add(AEItems.SPEED_CARD, ModItems.EXPORT_CARD.get(), 1);

        AE2InsertExportCard.NETWORK_HANDLER.register();
    }

    public static void tickWireless(net.minecraft.world.item.ItemStack stack, appeng.api.networking.IGrid grid,
            appeng.helpers.WirelessTerminalMenuHost host, net.minecraft.server.level.ServerPlayer player) {

        if (stack.hasTag() && stack.getTag().contains("upgrades") && grid != null && host != null) {
            net.minecraft.nbt.ListTag tagList = stack.getTag().getList("upgrades", net.minecraft.nbt.Tag.TAG_COMPOUND);

            for (int i = 0; i < tagList.size(); i++) {
                boolean isInsertUpgrade = tagList.getCompound(i).getString("id")
                        .equals(new net.minecraft.resources.ResourceLocation(AE2InsertExportCard.MOD_ID, "insert_card")
                                .toString());
                boolean isExportUpgrade = tagList.getCompound(i).getString("id")
                        .equals(new net.minecraft.resources.ResourceLocation(AE2InsertExportCard.MOD_ID, "export_card")
                                .toString());

                if (isInsertUpgrade || isExportUpgrade) {
                    // Found card
                }

                int slot = tagList.getCompound(i).getInt("Slot");
                net.minecraft.nbt.CompoundTag tag = (net.minecraft.nbt.CompoundTag) tagList.getCompound(i).get("tag");

                if (tag != null && (isInsertUpgrade || isExportUpgrade)) {
                    if (slot < 0 || slot >= host.getUpgrades().size()) {
                        continue;
                    }
                    net.minecraft.world.item.ItemStack upgrade = host.getUpgrades().getStackInSlot(slot);
                    int[] selectedInventorySlots = tag.getIntArray(
                            com.ultramega.ae2insertexportcard.item.UpgradeHost.NBT_SELECTED_INVENTORY_SLOTS);
                    appeng.api.upgrades.IUpgradeInventory upgrades = appeng.api.upgrades.UpgradeInventories
                            .forItem(upgrade, 3, null);
                    boolean invertFilter = upgrades.isInstalled(appeng.core.definitions.AEItems.INVERTER_CARD);

                    appeng.api.config.FuzzyMode fuzzyMode;
                    final String fz = upgrade.getOrCreateTag().getString("fuzzy_mode");
                    try {
                        fuzzyMode = appeng.api.config.FuzzyMode.valueOf(fz);
                    } catch (Throwable t) {
                        fuzzyMode = appeng.api.config.FuzzyMode.IGNORE_ALL;
                    }

                    int inventorySlotCount = Math.min(selectedInventorySlots.length,
                            player.getInventory().items.size());
                    for (int j = 0; j < inventorySlotCount; j++) {
                        if (selectedInventorySlots[j] >= 1) {
                            net.minecraft.world.item.ItemStack itemInInventory = player.getInventory().getItem(j);

                            if ((isExportUpgrade || itemInInventory.getItem() != net.minecraft.world.item.Items.AIR)
                                    && itemInInventory != stack) {
                                // Get filters
                                appeng.util.ConfigInventory filterConfig = appeng.util.ConfigInventory.configTypes(18,
                                        null);
                                filterConfig.readFromChildTag(tag, "filterConfig");

                                var node = host.getActionableNode();
                                if (node == null)
                                    return;

                                appeng.api.networking.security.IActionSource source = new appeng.me.helpers.PlayerSource(
                                        player);

                                if (isInsertUpgrade) {
                                    appeng.api.stacks.AEKey what = appeng.api.stacks.AEItemKey.of(itemInInventory);
                                    if (grid.getStorageService() != null) {
                                        boolean fuzzyInstalled = upgrades
                                                .isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD);
                                        var energySource = new appeng.me.helpers.ChannelPowerSrc(node,
                                                grid.getEnergyService());

                                        if (com.ultramega.ae2insertexportcard.compat.AddonStorageBridge
                                                .importFromItem(player, grid, energySource, source, j,
                                                        itemInInventory, filterConfig, fuzzyMode, fuzzyInstalled,
                                                        invertFilter)) {
                                            continue;
                                        }

                                        // Import Fluids
                                        boolean importedFluid = false;
                                        var fluidCapability = itemInInventory.getCapability(
                                                net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                .resolve();
                                        if (fluidCapability.isPresent()) {
                                            var fluidItem = fluidCapability.get();
                                            net.minecraftforge.fluids.FluidStack fluidStack = fluidItem.drain(
                                                    Integer.MAX_VALUE,
                                                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                            appeng.api.stacks.AEFluidKey fluidKey = fluidStack.isEmpty()
                                                    ? null
                                                    : appeng.api.stacks.AEFluidKey.of(fluidStack);
                                            if (fluidKey != null
                                                    && invertFilter != filterMatches(filterConfig, fluidKey, fuzzyMode,
                                                            fuzzyInstalled)) {
                                                long accepted = appeng.api.storage.StorageHelper.poweredInsert(
                                                        new appeng.me.helpers.ChannelPowerSrc(node,
                                                                grid.getEnergyService()),
                                                        grid.getStorageService().getInventory(), fluidKey,
                                                        fluidStack.getAmount(), source,
                                                        appeng.api.config.Actionable.SIMULATE);
                                                if (accepted > 0) {
                                                    net.minecraftforge.fluids.FluidStack drained = fluidItem.drain(
                                                            (int) Math.min(accepted, Integer.MAX_VALUE),
                                                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                    if (drained.isEmpty()) {
                                                        continue;
                                                    }
                                                    long inserted = appeng.api.storage.StorageHelper.poweredInsert(
                                                            new appeng.me.helpers.ChannelPowerSrc(node,
                                                                    grid.getEnergyService()),
                                                            grid.getStorageService().getInventory(), fluidKey,
                                                            drained.getAmount(), source,
                                                            appeng.api.config.Actionable.MODULATE);
                                                    if (inserted < drained.getAmount()) {
                                                        net.minecraftforge.fluids.FluidStack rollback = drained.copy();
                                                        rollback.setAmount((int) (drained.getAmount() - inserted));
                                                        fluidItem.fill(rollback,
                                                                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                    }
                                                    if (inserted > 0) {
                                                        player.getInventory().setItem(j, fluidItem.getContainer());
                                                        player.containerMenu.broadcastChanges();
                                                        importedFluid = true;
                                                    }
                                                }
                                            }
                                        }
                                        if (importedFluid) {
                                            continue;
                                        }

                                        // Import Items
                                        if (what != null
                                                && invertFilter != filterMatches(filterConfig, what, fuzzyMode,
                                                        fuzzyInstalled)) {
                                            long amount = appeng.api.storage.StorageHelper.poweredInsert(
                                                    new appeng.me.helpers.ChannelPowerSrc(node,
                                                            grid.getEnergyService()),
                                                    grid.getStorageService().getInventory(), what,
                                                    itemInInventory.getCount(), source,
                                                    appeng.api.config.Actionable.SIMULATE);
                                            if (amount <= 0)
                                                continue;

                                            long inserted = appeng.api.storage.StorageHelper.poweredInsert(
                                                    new appeng.me.helpers.ChannelPowerSrc(node,
                                                            grid.getEnergyService()),
                                                    grid.getStorageService().getInventory(), what,
                                                    amount, source,
                                                    appeng.api.config.Actionable.MODULATE);
                                            if (inserted > 0) {
                                                itemInInventory.shrink((int) Math.min(inserted,
                                                        itemInInventory.getCount()));
                                                player.containerMenu.broadcastChanges();
                                            }
                                        }
                                    }
                                } else {
                                    for (int index = 0; index < filterConfig.size(); index++) {
                                        appeng.api.stacks.GenericStack filter = filterConfig.getStack(index);
                                        if (filter == null)
                                            continue;
                                        if (index != selectedInventorySlots[j] - 1)
                                            continue;

                                        appeng.api.stacks.AEKey addonExportKey;
                                        if (upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD)) {
                                            var fuzzy = grid.getStorageService().getCachedInventory()
                                                    .findFuzzy(filter.what(), fuzzyMode)
                                                    .stream().findFirst();
                                            addonExportKey = fuzzy.map(java.util.Map.Entry::getKey).orElse(null);
                                        } else {
                                            addonExportKey = filter.what();
                                        }

                                        if (addonExportKey != null
                                                && com.ultramega.ae2insertexportcard.compat.AddonStorageBridge
                                                        .isSupportedKey(addonExportKey)) {
                                            long operationAmount = addonExportKey.getType().getAmountPerOperation();
                                            long requestedAmount = upgrades
                                                    .isInstalled(appeng.core.definitions.AEItems.SPEED_CARD)
                                                            ? operationAmount * 64L
                                                            : operationAmount;
                                            com.ultramega.ae2insertexportcard.compat.AddonStorageBridge.exportToItem(
                                                    player, grid,
                                                    new appeng.me.helpers.ChannelPowerSrc(node,
                                                            grid.getEnergyService()),
                                                    source, j, itemInInventory, addonExportKey, requestedAmount);
                                            continue;
                                        }

                                        java.util.Optional<net.minecraftforge.items.IItemHandler> playerInventory = player
                                                .getCapability(
                                                        net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                                                        net.minecraft.core.Direction.UP)
                                                .resolve();
                                        if (playerInventory.isPresent()) {
                                            appeng.api.stacks.AEItemKey what = appeng.api.stacks.AEItemKey
                                                    .of(itemInInventory.getItem());
                                            var fluidCapability = itemInInventory
                                                    .getCapability(
                                                            net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                    .resolve();
                                            boolean acceptsFluid = fluidCapability.isPresent();

                                            if (acceptsFluid || itemInInventory.isEmpty()
                                                    || (what != null
                                                            && (upgrades.isInstalled(
                                                                    appeng.core.definitions.AEItems.FUZZY_CARD)
                                                                            ? what.fuzzyEquals(filter.what(), fuzzyMode)
                                                                            : what.equals(filter.what())))) {
                                                appeng.api.stacks.AEKey toExportKey;
                                                if (upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD)) {
                                                    var fuzzy = grid.getStorageService().getCachedInventory()
                                                            .findFuzzy(filter.what(), fuzzyMode)
                                                            .stream().findFirst();
                                                    toExportKey = fuzzy.map(java.util.Map.Entry::getKey).orElse(null);
                                                } else {
                                                    toExportKey = filter.what();
                                                }
                                                if (toExportKey != null) {
                                                    if (!acceptsFluid
                                                            && toExportKey instanceof appeng.api.stacks.AEItemKey fuzzyItem) {
                                                        int extractAmount = Math.min(
                                                                itemInInventory.getMaxStackSize()
                                                                        - itemInInventory.getCount(),
                                                                itemInInventory.getMaxStackSize());
                                                        int stackInteractionSize = upgrades
                                                                .isInstalled(appeng.core.definitions.AEItems.SPEED_CARD)
                                                                        ? 64
                                                                        : 1;
                                                        int size = Math.min(stackInteractionSize, extractAmount);

                                                        if (size <= 0)
                                                            continue;

                                                        long extracted = appeng.api.storage.StorageHelper
                                                                .poweredExtraction(
                                                                        new appeng.me.helpers.ChannelPowerSrc(node,
                                                                                grid.getEnergyService()),
                                                                        grid.getStorageService().getInventory(),
                                                                        toExportKey,
                                                                        size, source,
                                                                        appeng.api.config.Actionable.SIMULATE);
                                                        if (extracted <= 0) {
                                                            if (upgrades.isInstalled(
                                                                    appeng.core.definitions.AEItems.CRAFTING_CARD)) {
                                                                var craftingService = grid.getCraftingService();
                                                                if (craftingService.isCraftable(filter.what())
                                                                        && craftingService.getRequestedAmount(
                                                                                filter.what()) <= 0) {
                                                                    var src = new appeng.me.helpers.MachineSource(
                                                                            grid::getPivot);

                                                                    var craftingJob = CRAFTING_JOBS.get(stack);
                                                                    if (craftingJob == null) {
                                                                        CRAFTING_JOBS.put(stack, craftingService
                                                                                .beginCraftingCalculation(
                                                                                        player.getLevel(), () -> src,
                                                                                        filter.what(), size,
                                                                                        appeng.api.networking.crafting.CalculationStrategy.CRAFT_LESS));
                                                                    } else if (craftingJob.isDone()) {
                                                                        try {
                                                                            var job = craftingJob.get();
                                                                            if (job != null) {
                                                                                craftingService.submitJob(job, null,
                                                                                        null, false, src);
                                                                            }
                                                                        } catch (InterruptedException e) {
                                                                            Thread.currentThread().interrupt();
                                                                        } catch (java.util.concurrent.ExecutionException
                                                                                | java.util.concurrent.CancellationException ignored) {
                                                                        } finally {
                                                                            CRAFTING_JOBS.remove(stack);
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            continue;
                                                        }

                                                        net.minecraft.world.item.ItemStack candidate = fuzzyItem
                                                                .toStack((int) extracted);
                                                        net.minecraft.world.item.ItemStack simulatedRemainder = playerInventory
                                                                .get().insertItem(j, candidate, true);
                                                        int accepted = candidate.getCount()
                                                                - simulatedRemainder.getCount();
                                                        if (accepted <= 0) {
                                                            continue;
                                                        }

                                                        long actuallyExtracted = appeng.api.storage.StorageHelper
                                                                .poweredExtraction(
                                                                        new appeng.me.helpers.ChannelPowerSrc(node,
                                                                                grid.getEnergyService()),
                                                                        grid.getStorageService().getInventory(),
                                                                        toExportKey, accepted, source,
                                                                        appeng.api.config.Actionable.MODULATE);
                                                        if (actuallyExtracted <= 0) {
                                                            continue;
                                                        }

                                                        net.minecraft.world.item.ItemStack remainder = playerInventory
                                                                .get().insertItem(j,
                                                                        fuzzyItem.toStack((int) actuallyExtracted),
                                                                        false);
                                                        if (!remainder.isEmpty()) {
                                                            long returned = grid.getStorageService().getInventory()
                                                                    .insert(toExportKey, remainder.getCount(),
                                                                            appeng.api.config.Actionable.MODULATE,
                                                                            source);
                                                            remainder.shrink((int) returned);
                                                            if (!remainder.isEmpty()
                                                                    && !player.getInventory().add(remainder)) {
                                                                player.drop(remainder, false);
                                                            }
                                                        }
                                                        player.containerMenu.broadcastChanges();
                                                    } else if (acceptsFluid
                                                            && toExportKey instanceof appeng.api.stacks.AEFluidKey fuzzyFluid) {
                                                        int stackInteractionSize = upgrades.isInstalled(
                                                                appeng.core.definitions.AEItems.SPEED_CARD)
                                                                        ? appeng.api.stacks.AEFluidKey.AMOUNT_BUCKET
                                                                                * 64
                                                                        : appeng.api.stacks.AEFluidKey.AMOUNT_BUCKET;

                                                        long extracted = appeng.api.storage.StorageHelper
                                                                .poweredExtraction(
                                                                        new appeng.me.helpers.ChannelPowerSrc(node,
                                                                                grid.getEnergyService()),
                                                                        grid.getStorageService().getInventory(),
                                                                        toExportKey,
                                                                        stackInteractionSize, source,
                                                                        appeng.api.config.Actionable.SIMULATE);
                                                        if (extracted <= 0) {
                                                            continue;
                                                        }

                                                        var fluidItem = fluidCapability.get();
                                                        int amount = fluidItem.fill(
                                                                fuzzyFluid.toStack((int) extracted),
                                                                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                                        if (amount <= 0) {
                                                            continue;
                                                        }

                                                        long actuallyExtracted = appeng.api.storage.StorageHelper
                                                                .poweredExtraction(
                                                                        new appeng.me.helpers.ChannelPowerSrc(node,
                                                                                grid.getEnergyService()),
                                                                        grid.getStorageService().getInventory(),
                                                                        toExportKey, amount, source,
                                                                        appeng.api.config.Actionable.MODULATE);
                                                        if (actuallyExtracted <= 0) {
                                                            continue;
                                                        }

                                                        int filled = fluidItem.fill(
                                                                fuzzyFluid.toStack((int) actuallyExtracted),
                                                                net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                        if (filled < actuallyExtracted) {
                                                            grid.getStorageService().getInventory().insert(toExportKey,
                                                                    actuallyExtracted - filled,
                                                                    appeng.api.config.Actionable.MODULATE, source);
                                                        }
                                                        player.getInventory().setItem(j,
                                                                fluidItem.getContainer());
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

    private static boolean filterMatches(appeng.util.ConfigInventory filterConfig, appeng.api.stacks.AEKey key,
            appeng.api.config.FuzzyMode fuzzyMode, boolean fuzzyInstalled) {
        return filterConfig.getAvailableStacks().findFuzzy(key, fuzzyMode).stream()
                .anyMatch(entry -> fuzzyInstalled
                        ? key.fuzzyEquals(entry.getKey(), fuzzyMode)
                        : key.equals(entry.getKey()));
    }
}
