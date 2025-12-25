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
            appeng.helpers.WirelessTerminalMenuHost host, net.minecraft.server.level.ServerPlayer player,
            java.util.function.Supplier<java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan>> jobGetter,
            java.util.function.Consumer<java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan>> jobSetter) {

        if (stack.getTag().contains("upgrades") && grid != null) {
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

                    for (int j = 0; j < selectedInventorySlots.length; j++) {
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
                                    if (what != null && grid.getStorageService() != null) {
                                        // Import Fluids
                                        for (int index = 0; index < filterConfig.size(); index++) {
                                            appeng.api.stacks.GenericStack filter = filterConfig.getStack(index);
                                            if (filter != null
                                                    && filter.what() instanceof appeng.api.stacks.AEFluidKey) {
                                                itemInInventory.getCapability(
                                                        net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                        .ifPresent((fluidItem -> {
                                                            net.minecraftforge.fluids.FluidStack fluidStack = fluidItem
                                                                    .drain(Integer.MAX_VALUE,
                                                                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                                            if (fluidStack.isEmpty())
                                                                return;

                                                            appeng.api.stacks.AEFluidKey aeFluidKey = appeng.api.stacks.AEFluidKey
                                                                    .of(fluidStack);
                                                            if (aeFluidKey == null)
                                                                return;

                                                            long amount = appeng.api.storage.StorageHelper
                                                                    .poweredInsert(
                                                                            new appeng.me.helpers.ChannelPowerSrc(node,
                                                                                    grid.getEnergyService()),
                                                                            grid.getStorageService().getInventory(),
                                                                            aeFluidKey,
                                                                            fluidStack.getAmount(), source,
                                                                            appeng.api.config.Actionable.SIMULATE);
                                                            if (amount <= 0)
                                                                return;

                                                            fluidItem.drain((int) amount,
                                                                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                                            appeng.api.storage.StorageHelper.poweredInsert(
                                                                    new appeng.me.helpers.ChannelPowerSrc(node,
                                                                            grid.getEnergyService()),
                                                                    grid.getStorageService().getInventory(), aeFluidKey,
                                                                    amount, source,
                                                                    appeng.api.config.Actionable.MODULATE);
                                                            player.containerMenu.broadcastChanges();
                                                        }));
                                            }
                                        }

                                        // Import Items
                                        final appeng.api.config.FuzzyMode finalFuzzyMode = fuzzyMode;
                                        if (invertFilter != filterConfig.getAvailableStacks().findFuzzy(what, fuzzyMode)
                                                .stream()
                                                .anyMatch(filterKeyEntry -> upgrades
                                                        .isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD)
                                                                ? what.fuzzyEquals(filterKeyEntry.getKey(),
                                                                        finalFuzzyMode)
                                                                : what.equals(filterKeyEntry.getKey()))) {
                                            long amount = appeng.api.storage.StorageHelper.poweredInsert(
                                                    new appeng.me.helpers.ChannelPowerSrc(node,
                                                            grid.getEnergyService()),
                                                    grid.getStorageService().getInventory(), what,
                                                    itemInInventory.getCount(), source,
                                                    appeng.api.config.Actionable.SIMULATE);
                                            if (amount <= 0)
                                                continue;

                                            appeng.api.storage.StorageHelper.poweredInsert(
                                                    new appeng.me.helpers.ChannelPowerSrc(node,
                                                            grid.getEnergyService()),
                                                    grid.getStorageService().getInventory(), what,
                                                    itemInInventory.getCount(), source,
                                                    appeng.api.config.Actionable.MODULATE);
                                            player.getInventory().setItem(j, net.minecraft.world.item.ItemStack.EMPTY);
                                            player.containerMenu.broadcastChanges();
                                        }
                                    }
                                } else {
                                    for (int index = 0; index < filterConfig.size(); index++) {
                                        appeng.api.stacks.GenericStack filter = filterConfig.getStack(index);
                                        if (filter == null)
                                            continue;
                                        if (index != selectedInventorySlots[j] - 1)
                                            continue;

                                        java.util.Optional<net.minecraftforge.items.IItemHandler> playerInventory = player
                                                .getCapability(
                                                        net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                                                        net.minecraft.core.Direction.UP)
                                                .resolve();
                                        if (playerInventory.isPresent()) {
                                            appeng.api.stacks.AEItemKey what = appeng.api.stacks.AEItemKey
                                                    .of(itemInInventory.getItem());
                                            boolean acceptsFluid = false;

                                            var cap = itemInInventory
                                                    .getCapability(
                                                            net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM)
                                                    .cast();
                                            if (cap.isPresent()) {
                                                acceptsFluid = true;
                                            }

                                            if (acceptsFluid || itemInInventory.isEmpty()
                                                    || (upgrades.isInstalled(appeng.core.definitions.AEItems.FUZZY_CARD)
                                                            ? what.fuzzyEquals(filter.what(), fuzzyMode)
                                                            : what.equals(filter.what()))) {
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
                                                                        appeng.api.config.Actionable.MODULATE);
                                                        if (extracted <= 0) {
                                                            if (upgrades.isInstalled(
                                                                    appeng.core.definitions.AEItems.CRAFTING_CARD)) {
                                                                var craftingService = grid.getCraftingService();
                                                                if (craftingService.isCraftable(filter.what())
                                                                        && craftingService.getRequestedAmount(
                                                                                filter.what()) <= 0) {
                                                                    var src = new appeng.me.helpers.MachineSource(
                                                                            grid::getPivot);

                                                                    java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan> ae2insertExportCard$craftingJob = jobGetter
                                                                            .get();

                                                                    if (ae2insertExportCard$craftingJob != null) {
                                                                        try {
                                                                            appeng.api.networking.crafting.ICraftingPlan job = null;
                                                                            if (ae2insertExportCard$craftingJob
                                                                                    .isDone()) {
                                                                                job = ae2insertExportCard$craftingJob
                                                                                        .get();
                                                                            }

                                                                            // Check if job is complete
                                                                            if (job != null) {
                                                                                craftingService.submitJob(job, null,
                                                                                        null, false, src);

                                                                                jobSetter.accept(null);
                                                                            }
                                                                        } catch (InterruptedException
                                                                                | java.util.concurrent.ExecutionException ignored) {
                                                                        }
                                                                    }

                                                                    jobSetter.accept(craftingService
                                                                            .beginCraftingCalculation(player.getLevel(),
                                                                                    () -> src,
                                                                                    filter.what(), size,
                                                                                    appeng.api.networking.crafting.CalculationStrategy.CRAFT_LESS));
                                                                }
                                                            }

                                                            continue;
                                                        }

                                                        playerInventory.get().insertItem(j,
                                                                fuzzyItem.toStack((int) extracted), false);
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

                                                        cap.ifPresent((o -> {
                                                            if (o instanceof net.minecraftforge.fluids.capability.IFluidHandlerItem fluidItem) {
                                                                int amount = fluidItem.fill(
                                                                        fuzzyFluid.toStack((int) extracted),
                                                                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                                                                if (amount <= 0) {
                                                                    return;
                                                                }

                                                                appeng.api.storage.StorageHelper.poweredExtraction(
                                                                        new appeng.me.helpers.ChannelPowerSrc(node,
                                                                                grid.getEnergyService()),
                                                                        grid.getStorageService().getInventory(),
                                                                        toExportKey, amount, source,
                                                                        appeng.api.config.Actionable.MODULATE);
                                                                fluidItem.fill(fuzzyFluid.toStack(amount),
                                                                        net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
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
