package com.ultramega.ae2importexportcard.mixin;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.compat.ae2wtlib.Ae2WtlibUtil;
import com.ultramega.ae2importexportcard.compat.mekanism.MekanismBridge;
import com.ultramega.ae2importexportcard.registry.ModDataComponents;
import com.ultramega.ae2importexportcard.registry.ModItems;
import com.ultramega.ae2importexportcard.util.AEKeyFilterUtil;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static com.ultramega.ae2importexportcard.item.UpgradeHost.SELECTED_INVENTORY_SLOT_COUNT;
import static com.ultramega.ae2importexportcard.item.UpgradeHost.normalizeSelectedInventorySlots;

@Mixin(WirelessTerminalItem.class)
public abstract class MixinWirelessTerminalItem extends Item {
    @Unique
    private static final int ae2importExportCard$FILTER_SIZE = 18;

    @Unique
    private static final int ae2importExportCard$UPGRADE_CARD_SLOT_COUNT = 3;

    @Unique
    private Future<ICraftingPlan> ae2importExportCard$craftingJob;

    public MixinWirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack terminalStack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(terminalStack, level, entity, slotId, isSelected);

        if (level.isClientSide()
            || !(entity instanceof ServerPlayer player)
            || !(terminalStack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            return;
        }

        // This also checks if we are in access point range
        IGrid grid = this.ae2importExportCard$getGrid(wirelessTerminalItem, player, terminalStack, level);
        if (grid == null || grid.getStorageService() == null) {
            return;
        }

        WirelessTerminalMenuHost<?> host = this.ae2importExportCard$getHost(wirelessTerminalItem, player, terminalStack);
        if (host == null || host.getActionableNode() == null) {
            return;
        }

        this.ae2importExportCard$tickUpgradeCards(terminalStack, player, level, grid, host);
    }

    @Unique
    private IGrid ae2importExportCard$getGrid(WirelessTerminalItem wirelessTerminalItem, ServerPlayer player, ItemStack terminalStack, Level level) {
        IGrid grid = null;

        if (AE2ImportExportCard.AE2WTLIB_INSTALLED) {
            grid = Ae2WtlibUtil.getGridFromStack(wirelessTerminalItem, player, terminalStack);
        }

        if (grid == null) {
            grid = wirelessTerminalItem.getLinkedGrid(terminalStack, level, null);
        }

        return grid;
    }

    @Unique
    private WirelessTerminalMenuHost<?> ae2importExportCard$getHost(WirelessTerminalItem wirelessTerminalItem, ServerPlayer player, ItemStack terminalStack) {
        if (wirelessTerminalItem.getMenuHost(player, MenuLocators.forStack(terminalStack), null) instanceof WirelessTerminalMenuHost<?> host) {
            return host;
        }

        return null;
    }

    @Unique
    private void ae2importExportCard$tickUpgradeCards(ItemStack terminalStack, ServerPlayer player, Level level, IGrid grid, WirelessTerminalMenuHost<?> host) {
        ItemContainerContents upgrades = terminalStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);

        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack upgradeStack = upgrades.getStackInSlot(i);

            boolean isImportUpgrade = upgradeStack.getItem() == ModItems.IMPORT_CARD.get();
            boolean isExportUpgrade = upgradeStack.getItem() == ModItems.EXPORT_CARD.get();
            if (!isImportUpgrade && !isExportUpgrade) {
                continue;
            }

            this.ae2importExportCard$tickUpgradeCard(terminalStack, upgradeStack, player, level, grid, host, isImportUpgrade);
        }
    }

    @Unique
    private void ae2importExportCard$tickUpgradeCard(ItemStack terminalStack,
                                                     ItemStack upgradeStack,
                                                     ServerPlayer player,
                                                     Level level,
                                                     IGrid grid,
                                                     WirelessTerminalMenuHost<?> host,
                                                     boolean importMode) {
        int[] selectedInventorySlots = normalizeSelectedInventorySlots(upgradeStack.getOrDefault(ModDataComponents.SELECTED_INVENTORY_SLOTS,
            new IntArrayList(new int[SELECTED_INVENTORY_SLOT_COUNT])).toIntArray());
        ConfigInventory filterConfig = this.ae2importExportCard$getFilterConfig(upgradeStack, player);
        IUpgradeInventory upgradeInventory = UpgradeInventories.forItem(upgradeStack, ae2importExportCard$UPGRADE_CARD_SLOT_COUNT, null);

        FuzzyMode fuzzyMode = this.ae2importExportCard$getFuzzyMode(upgradeStack);
        boolean fuzzy = upgradeInventory.isInstalled(AEItems.FUZZY_CARD);
        boolean invertFilter = upgradeInventory.isInstalled(AEItems.INVERTER_CARD);

        IActionSource source = new PlayerSource(player);
        ActionHostEnergySource energySource = new ActionHostEnergySource(host);

        for (int inventorySlot = 0; inventorySlot < selectedInventorySlots.length; inventorySlot++) {
            int selectedFilterSlot = selectedInventorySlots[inventorySlot];

            if (selectedFilterSlot < 1) {
                continue;
            }

            ItemStack itemInInventory = player.getInventory().getItem(inventorySlot);

            // Do not import/export the wireless terminal itself
            if (itemInInventory == terminalStack) {
                continue;
            }

            if (importMode) {
                this.ae2importExportCard$importFromPlayerSlot(player, grid, energySource, source, inventorySlot,
                    itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
            } else {
                this.ae2importExportCard$exportToPlayerSlot(player, level, grid, energySource, source, inventorySlot,
                    itemInInventory, selectedFilterSlot, filterConfig, upgradeInventory, fuzzyMode);
            }
        }
    }

    @Unique
    private ConfigInventory ae2importExportCard$getFilterConfig(ItemStack upgradeStack, ServerPlayer player) {
        ConfigInventory filterConfig = ConfigInventory.configTypes(ae2importExportCard$FILTER_SIZE)
            .changeListener(null)
            .build();
        filterConfig.readFromChildTag(upgradeStack.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag()), "", player.registryAccess());

        return filterConfig;
    }

    @Unique
    private FuzzyMode ae2importExportCard$getFuzzyMode(ItemStack upgradeStack) {
        IConfigManager configManager = IConfigManager.builder(upgradeStack)
            .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
            .build();

        return configManager.getSetting(Settings.FUZZY_MODE);
    }

    @Unique
    private void ae2importExportCard$importFromPlayerSlot(ServerPlayer player,
                                                          IGrid grid,
                                                          ActionHostEnergySource energySource,
                                                          IActionSource source,
                                                          int inventorySlot,
                                                          ItemStack itemInInventory,
                                                          ConfigInventory filterConfig,
                                                          FuzzyMode fuzzyMode,
                                                          boolean fuzzy,
                                                          boolean invertFilter) {
        if (itemInInventory.isEmpty()) {
            return;
        }

        this.ae2importExportCard$importFluidFromItem(player, grid, energySource, source, inventorySlot,
            itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);

        MekanismBridge.importChemicalFromItem(player, grid, energySource, source, inventorySlot,
            itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);

        this.ae2importExportCard$importItem(player, grid, energySource, source, inventorySlot, itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
    }

    @Unique
    private void ae2importExportCard$importItem(ServerPlayer player,
                                                IGrid grid,
                                                ActionHostEnergySource energySource,
                                                IActionSource source,
                                                int inventorySlot,
                                                ItemStack itemInInventory,
                                                ConfigInventory filterConfig,
                                                FuzzyMode fuzzyMode,
                                                boolean fuzzy,
                                                boolean invertFilter) {
        AEItemKey what = AEItemKey.of(itemInInventory);
        if (what == null) {
            return;
        }

        if (!AEKeyFilterUtil.passesFilter(what, filterConfig, fuzzyMode, fuzzy, invertFilter)) {
            return;
        }

        long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), what,
            itemInInventory.getCount(), source, Actionable.SIMULATE);
        if (insertable <= 0) {
            return;
        }

        int amountToMove = (int) Math.min(insertable, itemInInventory.getCount());
        long inserted = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), what, amountToMove, source, Actionable.MODULATE);
        if (inserted <= 0) {
            return;
        }

        itemInInventory.shrink((int) inserted);

        if (itemInInventory.isEmpty()) {
            player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
        } else {
            player.getInventory().setItem(inventorySlot, itemInInventory);
        }

        player.containerMenu.broadcastChanges();
    }

    @Unique
    private void ae2importExportCard$importFluidFromItem(ServerPlayer player,
                                                         IGrid grid,
                                                         ActionHostEnergySource energySource,
                                                         IActionSource source,
                                                         int inventorySlot,
                                                         ItemStack itemInInventory,
                                                         ConfigInventory filterConfig,
                                                         FuzzyMode fuzzyMode,
                                                         boolean fuzzy,
                                                         boolean invertFilter) {
        var fluidHandler = itemInInventory.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) {
            return;
        }

        FluidStack simulatedDrain = fluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedDrain.isEmpty()) {
            return;
        }

        AEFluidKey fluidKey = AEFluidKey.of(simulatedDrain);
        if (fluidKey == null || !AEKeyFilterUtil.passesFilter(fluidKey, filterConfig, fuzzyMode, fuzzy, invertFilter)) {
            return;
        }

        long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), fluidKey,
            simulatedDrain.getAmount(), source, Actionable.SIMULATE);
        if (insertable <= 0) {
            return;
        }

        FluidStack drained = fluidHandler.drain((int) insertable, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }

        AEFluidKey drainedKey = AEFluidKey.of(drained);
        if (drainedKey == null) {
            return;
        }

        StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), drainedKey, drained.getAmount(), source, Actionable.MODULATE);
        player.getInventory().setItem(inventorySlot, fluidHandler.getContainer());
        player.containerMenu.broadcastChanges();
    }

    @Unique
    private void ae2importExportCard$exportToPlayerSlot(ServerPlayer player,
                                                        Level level,
                                                        IGrid grid,
                                                        ActionHostEnergySource energySource,
                                                        IActionSource source,
                                                        int inventorySlot,
                                                        ItemStack itemInInventory,
                                                        int selectedFilterSlot,
                                                        ConfigInventory filterConfig,
                                                        IUpgradeInventory upgradeInventory,
                                                        FuzzyMode fuzzyMode) {
        int filterIndex = selectedFilterSlot - 1;
        if (filterIndex < 0 || filterIndex >= filterConfig.size()) {
            return;
        }

        GenericStack filter = filterConfig.getStack(filterIndex);
        if (filter == null) {
            return;
        }

        AEKey exportKey = this.ae2importExportCard$resolveExportKey(grid, filter.what(), upgradeInventory, fuzzyMode);
        if (exportKey == null) {
            return;
        }

        IItemHandler playerInventory = player.getCapability(Capabilities.ItemHandler.ENTITY);
        if (playerInventory == null) {
            return;
        }

        if (exportKey instanceof AEItemKey itemKey) {
            this.ae2importExportCard$exportItemToPlayerSlot(player, level, grid, energySource, source, inventorySlot,
                itemInInventory, itemKey, filter.what(), playerInventory, upgradeInventory);
        } else if (exportKey instanceof AEFluidKey fluidKey) {
            this.ae2importExportCard$exportFluidToPlayerSlot(player, grid, energySource, source, inventorySlot,
                itemInInventory, fluidKey, upgradeInventory);
        }else if (MekanismBridge.isChemicalKey(exportKey)) {
            long chemicalAmount = upgradeInventory.isInstalled(AEItems.SPEED_CARD)
                ? AEFluidKey.AMOUNT_BUCKET * 64
                : AEFluidKey.AMOUNT_BUCKET;

            boolean canAcceptChemical = MekanismBridge.canAcceptChemical(itemInInventory, exportKey, chemicalAmount);
            if (!canAcceptChemical) {
                return;
            }

            boolean exported = MekanismBridge.exportChemicalToItem(player, grid, energySource, source, inventorySlot, itemInInventory, exportKey, chemicalAmount);
            if (!exported) {
                this.ae2importExportCard$requestCraftingIfPossible(level, grid, filter.what(), (int) chemicalAmount, upgradeInventory);
            }
        }
    }

    @Unique
    private AEKey ae2importExportCard$resolveExportKey(IGrid grid, AEKey filterKey, IUpgradeInventory upgradeInventory, FuzzyMode fuzzyMode) {
        if (!upgradeInventory.isInstalled(AEItems.FUZZY_CARD)) {
            return filterKey;
        }

        return grid.getStorageService()
            .getCachedInventory()
            .findFuzzy(filterKey, fuzzyMode)
            .stream()
            .findFirst()
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    @Unique
    private void ae2importExportCard$exportItemToPlayerSlot(ServerPlayer player,
                                                            Level level,
                                                            IGrid grid,
                                                            ActionHostEnergySource energySource,
                                                            IActionSource source,
                                                            int inventorySlot,
                                                            ItemStack itemInInventory,
                                                            AEItemKey itemKey,
                                                            AEKey craftingKey,
                                                            IItemHandler playerInventory,
                                                            IUpgradeInventory upgradeInventory) {
        ItemStack prototype = itemKey.toStack(1);

        int slotLimit = playerInventory.getSlotLimit(inventorySlot);
        int maxStackSize = Math.min(slotLimit, prototype.getMaxStackSize());
        int currentCount = itemInInventory.isEmpty() ? 0 : itemInInventory.getCount();
        int remainingSpace = maxStackSize - currentCount;
        if (remainingSpace <= 0) {
            return;
        }

        int stackInteractionSize = upgradeInventory.isInstalled(AEItems.SPEED_CARD) ? 64 : 1;
        int requestedAmount = Math.min(stackInteractionSize, remainingSpace);

        long extractable = StorageHelper.poweredExtraction(
            energySource,
            grid.getStorageService().getInventory(),
            itemKey,
            requestedAmount,
            source,
            Actionable.SIMULATE
        );

        if (extractable <= 0) {
            this.ae2importExportCard$requestCraftingIfPossible(level, grid, craftingKey, requestedAmount, upgradeInventory);
            return;
        }

        int amountToInsert = (int) Math.min(extractable, requestedAmount);
        ItemStack simulatedInsertStack = itemKey.toStack(amountToInsert);
        ItemStack remainder = playerInventory.insertItem(inventorySlot, simulatedInsertStack, true);
        int acceptedAmount = simulatedInsertStack.getCount() - remainder.getCount();
        if (acceptedAmount <= 0) {
            return;
        }

        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), itemKey, acceptedAmount, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return;
        }

        playerInventory.insertItem(inventorySlot, itemKey.toStack((int) extracted), false);
        player.containerMenu.broadcastChanges();
    }

    @Unique
    private void ae2importExportCard$exportFluidToPlayerSlot(ServerPlayer player,
                                                             IGrid grid,
                                                             ActionHostEnergySource energySource,
                                                             IActionSource source,
                                                             int inventorySlot,
                                                             ItemStack itemInInventory,
                                                             AEFluidKey fluidKey,
                                                             IUpgradeInventory upgradeInventory) {
        var fluidHandler = itemInInventory.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) {
            return;
        }

        int stackInteractionSize = upgradeInventory.isInstalled(AEItems.SPEED_CARD)
            ? AEFluidKey.AMOUNT_BUCKET * 64
            : AEFluidKey.AMOUNT_BUCKET;

        long extractable = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), fluidKey,
            stackInteractionSize, source, Actionable.SIMULATE);
        if (extractable <= 0) {
            return;
        }

        int fillableAmount = fluidHandler.fill(fluidKey.toStack((int) extractable), IFluidHandler.FluidAction.SIMULATE);
        if (fillableAmount <= 0) {
            return;
        }

        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), fluidKey, fillableAmount, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return;
        }

        fluidHandler.fill(fluidKey.toStack((int) extracted), IFluidHandler.FluidAction.EXECUTE);
        player.getInventory().setItem(inventorySlot, fluidHandler.getContainer());
        player.containerMenu.broadcastChanges();
    }

    @Unique
    private void ae2importExportCard$requestCraftingIfPossible(Level level, IGrid grid, AEKey what, int amount, IUpgradeInventory upgradeInventory) {
        if (!upgradeInventory.isInstalled(AEItems.CRAFTING_CARD)) {
            return;
        }

        var craftingService = grid.getCraftingService();
        if (!craftingService.isCraftable(what) || craftingService.getRequestedAmount(what) > 0) {
            return;
        }

        MachineSource source = new MachineSource(grid::getPivot);

        if (this.ae2importExportCard$craftingJob != null) {
            if (!this.ae2importExportCard$craftingJob.isDone()) {
                return;
            }

            try {
                ICraftingPlan job = this.ae2importExportCard$craftingJob.get();
                if (job != null) {
                    craftingService.submitJob(job, null, null, false, source);
                }
            } catch (InterruptedException | ExecutionException ignored) {
            } finally {
                this.ae2importExportCard$craftingJob = null;
            }
        }

        this.ae2importExportCard$craftingJob = craftingService.beginCraftingCalculation(level, () -> source, what, amount, CalculationStrategy.CRAFT_LESS);
    }
}
