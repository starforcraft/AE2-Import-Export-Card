package com.ultramega.ae2importexportcard.mixin;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.compat.ae2wtlib.Ae2WtlibUtil;
import com.ultramega.ae2importexportcard.compat.appflux.AppFluxBridge;
import com.ultramega.ae2importexportcard.compat.curios.CuriosBridge;
import com.ultramega.ae2importexportcard.compat.curios.CuriosTerminalTicker;
import com.ultramega.ae2importexportcard.config.ServerConfig;
import com.ultramega.ae2importexportcard.item.UpgradeHost;
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
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.cells.ICellWorkbenchItem;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static com.ultramega.ae2importexportcard.item.UpgradeHost.SELECTED_INVENTORY_SLOT_COUNT;

@Mixin(WirelessTerminalItem.class)
public abstract class WirelessTerminalItemMixin extends Item implements CuriosTerminalTicker {
    @Unique
    private Future<ICraftingPlan> ae2ImportExportCard$craftingJob;

    public WirelessTerminalItemMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(final ItemStack itemStack, final ServerLevel level, final Entity owner, @Nullable final EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);

        if (level.isClientSide() || !(owner instanceof ServerPlayer player) || !(itemStack.getItem() instanceof WirelessTerminalItem)) {
            return;
        }

        if (CuriosBridge.isEquipped(player, itemStack)) {
            return;
        }
        this.ae2ImportExportCard$tickEquippedTerminal(player, itemStack);
    }

    @Override
    public void ae2ImportExportCard$tickEquippedTerminal(ServerPlayer player, ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            return;
        }

        // This also checks if we are in access point range
        IGrid grid = this.ae2ImportExportCard$getGrid(wirelessTerminalItem, player, itemStack, player.level());
        if (grid == null || grid.getStorageService() == null) {
            return;
        }

        WirelessTerminalMenuHost<?> host = this.ae2ImportExportCard$getHost(wirelessTerminalItem, player, itemStack);
        if (host == null || host.getActionableNode() == null) {
            return;
        }

        this.ae2ImportExportCard$tickUpgradeCards(itemStack, player, player.level(), grid, host);
    }

    @Unique
    private IGrid ae2ImportExportCard$getGrid(WirelessTerminalItem wirelessTerminalItem, ServerPlayer player, ItemStack terminalStack, Level level) {
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
    private WirelessTerminalMenuHost<?> ae2ImportExportCard$getHost(WirelessTerminalItem wirelessTerminalItem, ServerPlayer player, ItemStack terminalStack) {
        if (wirelessTerminalItem.getMenuHost(player, MenuLocators.forStack(terminalStack), null) instanceof WirelessTerminalMenuHost<?> host) {
            return host;
        }

        return null;
    }

    @Unique
    private void ae2ImportExportCard$tickUpgradeCards(ItemStack terminalStack, ServerPlayer player, Level level, IGrid grid, WirelessTerminalMenuHost<?> host) {
        ItemContainerContents upgrades = terminalStack.getOrDefault(AEComponents.UPGRADES, ItemContainerContents.EMPTY);

        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack upgradeStack = upgrades.getStackInSlot(i);

            boolean isImportUpgrade = upgradeStack.getItem() == ModItems.IMPORT_CARD.get();
            boolean isExportUpgrade = upgradeStack.getItem() == ModItems.EXPORT_CARD.get();
            if (!isImportUpgrade && !isExportUpgrade) {
                continue;
            }

            this.ae2ImportExportCard$tickUpgradeCard(terminalStack, upgradeStack, player, level, grid, host, isImportUpgrade);
        }
    }

    @Unique
    private void ae2ImportExportCard$tickUpgradeCard(ItemStack terminalStack,
                                                     ItemStack upgradeStack,
                                                     ServerPlayer player,
                                                     Level level,
                                                     IGrid grid,
                                                     WirelessTerminalMenuHost<?> host,
                                                     boolean importMode) {
        int[] selectedInventorySlots = upgradeStack.getOrDefault(ModDataComponents.SELECTED_INVENTORY_SLOTS,
            new IntArrayList(new int[SELECTED_INVENTORY_SLOT_COUNT])).toIntArray();
        int upgradeSlotCount = importMode ? UpgradeHost.IMPORT_UPGRADE_SLOT_COUNT : UpgradeHost.EXPORT_UPGRADE_SLOT_COUNT;
        IUpgradeInventory upgradeInventory = UpgradeInventories.forItem(upgradeStack, upgradeSlotCount, null);
        ConfigInventory filterConfig = this.ae2ImportExportCard$getFilterConfig(upgradeStack, player, UpgradeHost.getFilterSlotCount(upgradeInventory));

        FuzzyMode fuzzyMode = this.ae2ImportExportCard$getFuzzyMode(upgradeStack);
        boolean fuzzy = upgradeInventory.isInstalled(AEItems.FUZZY_CARD);
        boolean invertFilter = upgradeInventory.isInstalled(AEItems.INVERTER_CARD);

        IActionSource source = new PlayerSource(player);
        ActionHostEnergySource energySource = new ActionHostEnergySource(host);

        for (int inventorySlot = 0; inventorySlot < Math.min(selectedInventorySlots.length, SELECTED_INVENTORY_SLOT_COUNT); inventorySlot++) {
            int selectedFilterSlot = selectedInventorySlots[inventorySlot];

            if (selectedFilterSlot < 1) {
                continue;
            }

            ItemStack itemInInventory = player.getInventory().getItem(inventorySlot);

            // Never import the terminal; exports below only allow charging it
            if (importMode && itemInInventory == terminalStack) {
                continue;
            }

            if (importMode) {
                this.ae2ImportExportCard$importFromPlayerSlot(player, grid, energySource, source, inventorySlot,
                    itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
            } else {
                this.ae2ImportExportCard$exportToPlayerSlot(level, grid, energySource, source, inventorySlot,
                    itemInInventory, selectedFilterSlot, filterConfig, upgradeInventory, fuzzyMode,
                    player.getCapability(Capabilities.Item.ENTITY, null), ItemAccess.forPlayerSlot(player, inventorySlot),
                    itemInInventory == terminalStack);
            }
        }

        if (!importMode) {
            Map<String, Integer> selectedCurios = upgradeStack.getOrDefault(ModDataComponents.SELECTED_CURIO_SLOTS, Map.of());
            // Resolve again for each transfer: equipping a curio can resize other slot groups
            for (String key : selectedCurios.keySet()) {
                CuriosBridge.getSlots(player).stream().filter(curio -> curio.key().equals(key)).findFirst().ifPresent(curio -> {
                    this.ae2ImportExportCard$exportToPlayerSlot(level, grid, energySource, source, curio.index(),
                        curio.stack(), selectedCurios.get(key), filterConfig, upgradeInventory, fuzzyMode,
                        curio.handler(), curio.access(), curio.stack() == terminalStack);
                });
            }
        }
    }

    @Unique
    private ConfigInventory ae2ImportExportCard$getFilterConfig(ItemStack upgradeStack, ServerPlayer player, int filterSize) {
        ConfigInventory filterConfig = ConfigInventory.configTypes(filterSize)
            .changeListener(null)
            .build();

        ValueInput filterInput = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(),
            upgradeStack.getOrDefault(ModDataComponents.FILTER_CONFIG, new CompoundTag()));
        filterConfig.readFromChildTag(filterInput, "");

        return filterConfig;
    }

    @Unique
    private FuzzyMode ae2ImportExportCard$getFuzzyMode(ItemStack upgradeStack) {
        IConfigManager configManager = IConfigManager.builder(upgradeStack)
            .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
            .build();

        return configManager.getSetting(Settings.FUZZY_MODE);
    }

    @Unique
    private void ae2ImportExportCard$importFromPlayerSlot(ServerPlayer player,
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

        this.ae2ImportExportCard$importFluidFromItem(player, grid, energySource, source, inventorySlot,
            itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);

        /*MekanismBridge.importChemicalFromItem(player, grid, energySource, source, inventorySlot,
            itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);*/

        AppFluxBridge.importEnergyFromItem(grid, energySource, source, ItemAccess.forPlayerSlot(player, inventorySlot),
            itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);

        this.ae2ImportExportCard$importItem(player, grid, energySource, source, inventorySlot, itemInInventory, filterConfig, fuzzyMode, fuzzy, invertFilter);
    }

    @Unique
    private void ae2ImportExportCard$importItem(ServerPlayer player,
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
    private void ae2ImportExportCard$importFluidFromItem(ServerPlayer player,
                                                         IGrid grid,
                                                         ActionHostEnergySource energySource,
                                                         IActionSource source,
                                                         int inventorySlot,
                                                         ItemStack itemInInventory,
                                                         ConfigInventory filterConfig,
                                                         FuzzyMode fuzzyMode,
                                                         boolean fuzzy,
                                                         boolean invertFilter) {
        var fluidHandler = itemInInventory.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forPlayerSlot(player, inventorySlot));
        if (fluidHandler == null) {
            return;
        }

        for (int i = 0; i < fluidHandler.size(); i++) {
            FluidResource resource = fluidHandler.getResource(i);
            if (resource.isEmpty()) {
                continue;
            }

            AEFluidKey fluidKey = AEFluidKey.of(resource);
            if (fluidKey == null || !AEKeyFilterUtil.passesFilter(fluidKey, filterConfig, fuzzyMode, fuzzy, invertFilter)) {
                continue;
            }

            int extractable;
            try (Transaction tx = Transaction.openRoot()) {
                extractable = fluidHandler.extract(resource, Integer.MAX_VALUE, tx);
            }
            if (extractable <= 0) {
                continue;
            }

            long insertable = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), fluidKey, extractable, source, Actionable.SIMULATE);
            if (insertable <= 0) {
                continue;
            }

            int amountToMove = (int) Math.min(insertable, Integer.MAX_VALUE);
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = fluidHandler.extract(resource, amountToMove, tx);
                if (extracted <= 0) {
                    continue;
                }

                long inserted = StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), fluidKey, extracted, source, Actionable.MODULATE);
                if (inserted <= 0) {
                    continue;
                }

                if (inserted < extracted) {
                    int remainder = extracted - (int) inserted;
                    int returned = fluidHandler.insert(resource, remainder, tx);

                    if (returned != remainder) {
                        continue;
                    }
                }

                tx.commit();
            }
        }
    }

    @Unique
    private void ae2ImportExportCard$exportToPlayerSlot(Level level,
                                                        IGrid grid,
                                                        ActionHostEnergySource energySource,
                                                        IActionSource source,
                                                        int inventorySlot,
                                                        ItemStack itemInInventory,
                                                        int selectedFilterSlot,
                                                        ConfigInventory filterConfig,
                                                        IUpgradeInventory upgradeInventory,
                                                        FuzzyMode fuzzyMode,
                                                        ResourceHandler<ItemResource> itemHandler,
                                                        ItemAccess itemAccess,
                                                        boolean terminalTarget) {
        int filterIndex = selectedFilterSlot - 1;
        if (filterIndex < 0 || filterIndex >= filterConfig.size()) {
            return;
        }

        GenericStack filter = filterConfig.getStack(filterIndex);
        if (filter == null || !ServerConfig.allowsExport(filter.what())) {
            return;
        }

        AEKey exportKey = this.ae2ImportExportCard$resolveExportKey(grid, filter.what(), upgradeInventory, fuzzyMode);
        if (exportKey == null || !ServerConfig.allowsExport(exportKey)) {
            return;
        }

        if (terminalTarget && !AppFluxBridge.isFluxKey(exportKey)) {
            return;
        }

        if (itemInInventory.getItem() instanceof ICellWorkbenchItem
            && this.ae2ImportExportCard$exportToCell(level, grid, energySource, source, itemAccess, exportKey, filter.what(), upgradeInventory)) {
            return;
        }

        if (exportKey instanceof AEItemKey itemKey && itemHandler != null) {
            this.ae2ImportExportCard$exportItemToPlayerSlot(level, grid, energySource, source, inventorySlot, itemKey, filter.what(), itemHandler, upgradeInventory);
        } else if (exportKey instanceof AEFluidKey fluidKey) {
            this.ae2ImportExportCard$exportFluidToPlayerSlot(grid, energySource, source, itemAccess, itemInInventory, fluidKey, upgradeInventory);
        } /*else if (MekanismBridge.isChemicalKey(exportKey)) {
            long chemicalAmount = upgradeInventory.isInstalled(AEItems.SPEED_CARD)
                ? AEFluidKey.AMOUNT_BUCKET * 64
                : AEFluidKey.AMOUNT_BUCKET;

            boolean canAcceptChemical = MekanismBridge.canAcceptChemical(itemInInventory, exportKey, chemicalAmount);
            if (!canAcceptChemical) {
                return;
            }

            boolean exported = MekanismBridge.exportChemicalToItem(player, grid, energySource, source, inventorySlot, itemInInventory, exportKey, chemicalAmount);
            if (!exported) {
                this.ae2ImportExportCard$requestCraftingIfPossible(level, grid, filter.what(), (int) chemicalAmount, upgradeInventory);
            }
        }*/
        else if (AppFluxBridge.isFluxKey(exportKey)) {
            long energyAmount = upgradeInventory.isInstalled(AEItems.SPEED_CARD)
                ? AEFluidKey.AMOUNT_BUCKET * 64
                : AEFluidKey.AMOUNT_BUCKET;

            boolean canAcceptEnergy = AppFluxBridge.canAcceptEnergy(itemAccess, itemInInventory, exportKey, energyAmount);
            if (!canAcceptEnergy) {
                return;
            }

            boolean exported = AppFluxBridge.exportEnergyToItem(grid, energySource, source, itemAccess, itemInInventory, exportKey, energyAmount);
            if (!exported) {
                this.ae2ImportExportCard$requestCraftingIfPossible(level, grid, filter.what(), (int) energyAmount, upgradeInventory);
            }
        }
    }

    @Unique
    private boolean ae2ImportExportCard$exportToCell(Level level,
                                                     IGrid grid,
                                                     ActionHostEnergySource energySource,
                                                     IActionSource source,
                                                     ItemAccess itemAccess,
                                                     AEKey exportKey,
                                                     AEKey craftingKey,
                                                     IUpgradeInventory upgradeInventory) {
        // Never duplicate a cell's contents across a stack of multiple items
        if (itemAccess.getAmount() != 1) {
            return false;
        }

        ItemStack cellStack = itemAccess.getResource().toStack();
        var cell = StorageCells.getCellInventory(cellStack, null);
        if (cell == null) {
            return false;
        }

        int operations = upgradeInventory.isInstalled(AEItems.SPEED_CARD) ? 64 : 1;
        long limit = (long) exportKey.getAmountPerOperation() * operations;
        long requested = cell.insert(exportKey, limit, Actionable.SIMULATE, source);
        if (requested <= 0) {
            return false;
        }

        var network = grid.getStorageService().getInventory();
        long extractable = StorageHelper.poweredExtraction(energySource, network, exportKey, requested, source, Actionable.SIMULATE);
        if (extractable <= 0) {
            this.ae2ImportExportCard$requestCraftingIfPossible(level, grid, craftingKey, (int) Math.min(requested, Integer.MAX_VALUE), upgradeInventory);
            return true;
        }

        long accepted = cell.insert(exportKey, extractable, Actionable.MODULATE, source);
        if (accepted <= 0) {
            return true;
        }
        cell.persist();
        try (Transaction tx = Transaction.openRoot()) {
            if (itemAccess.exchange(ItemResource.of(cellStack), 1, tx) != 1) {
                return true;
            }
        }

        long extracted = StorageHelper.poweredExtraction(energySource, network, exportKey, accepted, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return true;
        }

        cellStack = itemAccess.getResource().toStack();
        cell = StorageCells.getCellInventory(cellStack, null);
        long inserted = cell == null ? 0 : cell.insert(exportKey, extracted, Actionable.MODULATE, source);
        boolean committed = false;
        if (inserted > 0) {
            cell.persist();
            try (Transaction tx = Transaction.openRoot()) {
                if (itemAccess.exchange(ItemResource.of(cellStack), 1, tx) == 1) {
                    tx.commit();
                    committed = true;
                }
            }
        }
        long remainder = extracted - (committed ? inserted : 0);
        if (remainder > 0) {
            network.insert(exportKey, remainder, Actionable.MODULATE, source);
        }
        return true;
    }

    @Unique
    private AEKey ae2ImportExportCard$resolveExportKey(IGrid grid, AEKey filterKey, IUpgradeInventory upgradeInventory, FuzzyMode fuzzyMode) {
        if (!upgradeInventory.isInstalled(AEItems.FUZZY_CARD)) {
            return filterKey;
        }

        return grid.getStorageService()
            .getCachedInventory()
            .findFuzzy(filterKey, fuzzyMode)
            .stream()
            .filter(entry -> ServerConfig.allowsExport(entry.getKey()))
            .findFirst()
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    @Unique
    private void ae2ImportExportCard$exportItemToPlayerSlot(Level level,
                                                            IGrid grid,
                                                            ActionHostEnergySource energySource,
                                                            IActionSource source,
                                                            int inventorySlot,
                                                            AEItemKey itemKey,
                                                            AEKey craftingKey,
                                                            ResourceHandler<ItemResource> playerInventory,
                                                            IUpgradeInventory upgradeInventory) {
        ItemResource resource = itemKey.toResource();
        if (resource.isEmpty()) {
            return;
        }

        int stackInteractionSize = upgradeInventory.isInstalled(AEItems.SPEED_CARD) ? 64 : 1;

        int requestedAmount;
        try (Transaction tx = Transaction.openRoot()) {
            requestedAmount = playerInventory.insert(inventorySlot, resource, stackInteractionSize, tx);
        }
        if (requestedAmount <= 0) {
            return;
        }

        long extractable = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), itemKey, requestedAmount, source, Actionable.SIMULATE);
        if (extractable <= 0) {
            this.ae2ImportExportCard$requestCraftingIfPossible(level, grid, craftingKey, requestedAmount, upgradeInventory);
            return;
        }

        int amountToInsert = (int) Math.min(extractable, requestedAmount);
        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), itemKey, amountToInsert, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return;
        }

        try (Transaction tx = Transaction.openRoot()) {
            int inserted = playerInventory.insert(inventorySlot, resource, (int) Math.min(extracted, Integer.MAX_VALUE), tx);
            if (inserted != extracted) {
                // Roll back
                StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), itemKey, extracted, source, Actionable.MODULATE);
                return;
            }

            tx.commit();
        }
    }

    @Unique
    private void ae2ImportExportCard$exportFluidToPlayerSlot(IGrid grid,
                                                             ActionHostEnergySource energySource,
                                                             IActionSource source,
                                                             ItemAccess itemAccess,
                                                             ItemStack itemInInventory,
                                                             AEFluidKey fluidKey,
                                                             IUpgradeInventory upgradeInventory) {
        var fluidHandler = itemInInventory.getCapability(Capabilities.Fluid.ITEM, itemAccess);
        if (fluidHandler == null) {
            return;
        }

        FluidResource resource = fluidKey.toResource();
        if (resource.isEmpty()) {
            return;
        }

        int stackInteractionSize = upgradeInventory.isInstalled(AEItems.SPEED_CARD)
            ? AEFluidKey.AMOUNT_BUCKET * 64
            : AEFluidKey.AMOUNT_BUCKET;

        long extractable = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), fluidKey, stackInteractionSize, source, Actionable.SIMULATE);
        if (extractable <= 0) {
            return;
        }

        int insertable;
        try (Transaction tx = Transaction.openRoot()) {
            insertable = fluidHandler.insert(resource, (int) Math.min(extractable, Integer.MAX_VALUE), tx);
        }
        if (insertable <= 0) {
            return;
        }

        long extracted = StorageHelper.poweredExtraction(energySource, grid.getStorageService().getInventory(), fluidKey, insertable, source, Actionable.MODULATE);
        if (extracted <= 0) {
            return;
        }

        try (Transaction tx = Transaction.openRoot()) {
            int inserted = fluidHandler.insert(resource, (int) Math.min(extracted, Integer.MAX_VALUE), tx);
            if (inserted != extracted) {
                // Roll back
                StorageHelper.poweredInsert(energySource, grid.getStorageService().getInventory(), fluidKey, extracted, source, Actionable.MODULATE);
                return;
            }

            tx.commit();
        }
    }

    @Unique
    private void ae2ImportExportCard$requestCraftingIfPossible(Level level, IGrid grid, AEKey what, int amount, IUpgradeInventory upgradeInventory) {
        if (!ServerConfig.allowsExport(what) || !upgradeInventory.isInstalled(AEItems.CRAFTING_CARD)) {
            return;
        }

        var craftingService = grid.getCraftingService();
        if (!craftingService.isCraftable(what) || craftingService.getRequestedAmount(what) > 0) {
            return;
        }

        MachineSource source = new MachineSource(grid::getPivot);

        if (this.ae2ImportExportCard$craftingJob != null) {
            if (!this.ae2ImportExportCard$craftingJob.isDone()) {
                return;
            }

            try {
                ICraftingPlan job = this.ae2ImportExportCard$craftingJob.get();
                if (job != null && ServerConfig.allowsExport(job.finalOutput().what())) {
                    craftingService.submitJob(job, null, null, false, source);
                }
            } catch (InterruptedException | ExecutionException ignored) {
            } finally {
                this.ae2ImportExportCard$craftingJob = null;
            }
        }

        this.ae2ImportExportCard$craftingJob = craftingService.beginCraftingCalculation(level, () -> source, what, amount, CalculationStrategy.CRAFT_LESS);
    }
}
