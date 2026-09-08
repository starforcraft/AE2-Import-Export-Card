package com.ultramega.ae2importexportcard.util;

import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.compat.ae2wtlib.Ae2WtlibUtil;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BlockPickerHandler {
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;

    private BlockPickerHandler() {
    }

    public static void handle(final ServerPlayer player, final BlockPos blockPos) {
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL || !player.level().isLoaded(blockPos)) {
            return;
        }

        final double reach = player.blockInteractionRange() + 1.0;
        if (new AABB(blockPos).distanceToSqr(player.getEyePosition()) > reach * reach) {
            return;
        }

        final BlockState blockState = player.level().getBlockState(blockPos);
        if (blockState.isAir()) {
            return;
        }

        final ItemStack pickedStack = blockState.getCloneItemStack(blockPos, player.level(), false, player);
        final Inventory inventory = player.getInventory();
        if (pickedStack.isEmpty() || contains(inventory, pickedStack)) {
            return;
        }

        final int destinationSlot = inventory.getFreeSlot();
        if (destinationSlot == Inventory.NOT_FOUND_INDEX) {
            return;
        }

        final AEItemKey itemKey = AEItemKey.of(pickedStack);
        if (itemKey == null) {
            return;
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (tryExtract(player, inventory.getItem(slot), slot, destinationSlot, itemKey)) {
                inventory.pickSlot(destinationSlot);
                player.connection.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
                player.inventoryMenu.broadcastChanges();
                return;
            }
        }
    }

    private static boolean tryExtract(final ServerPlayer player, final ItemStack terminalStack, final int terminalSlot,
                                      final int destinationSlot, final AEItemKey itemKey) {
        if (!(terminalStack.getItem() instanceof WirelessTerminalItem terminalItem) || BlockPickerCardConfig.findCard(terminalStack).isEmpty()) {
            return false;
        }

        IGrid grid = null;
        if (AE2ImportExportCard.AE2WTLIB_INSTALLED) {
            grid = Ae2WtlibUtil.getGridFromStack(terminalItem, player, terminalStack);
        }
        if (grid == null) {
            grid = terminalItem.getLinkedGrid(terminalStack, player.level(), null);
        }
        if (grid == null
            || !(terminalItem.getMenuHost(player, MenuLocators.forInventorySlot(terminalSlot), null) instanceof WirelessTerminalMenuHost<?> host)
            || host.getActionableNode() == null) {
            return false;
        }

        final int capacity = getAvailableCapacity(player.getInventory(), itemKey);
        final int requested = Math.min(BlockPickerCardConfig.getAmountFromTerminal(terminalStack), capacity);
        if (requested <= 0) {
            return false;
        }

        final long extracted = StorageHelper.poweredExtraction(host, grid.getStorageService().getInventory(), itemKey, requested, new PlayerSource(player, host), Actionable.MODULATE);
        if (extracted <= 0) {
            return false;
        }

        insertIntoInventory(player.getInventory(), itemKey, (int) extracted, destinationSlot);
        return true;
    }

    private static int getAvailableCapacity(final Inventory inventory, final AEItemKey itemKey) {
        int capacity = 0;
        final ItemStack prototype = itemKey.toStack();
        final int slotLimit = prototype.getMaxStackSize();
        for (int slot = 0; slot < Math.min(PLAYER_INVENTORY_SLOT_COUNT, inventory.getContainerSize()); slot++) {
            final ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty()) {
                capacity += slotLimit;
            } else if (ItemStack.isSameItemSameComponents(existing, prototype)) {
                capacity += Math.max(0, Math.min(existing.getMaxStackSize(), slotLimit) - existing.getCount());
            }
        }
        return capacity;
    }

    private static void insertIntoInventory(final Inventory inventory, final AEItemKey itemKey, final int amount, final int firstSlot) {
        int remaining = insertIntoSlot(inventory, itemKey, amount, firstSlot);
        for (int slot = 0; slot < Math.min(PLAYER_INVENTORY_SLOT_COUNT, inventory.getContainerSize()) && remaining > 0; slot++) {
            if (slot != firstSlot) {
                remaining = insertIntoSlot(inventory, itemKey, remaining, slot);
            }
        }
    }

    private static int insertIntoSlot(final Inventory inventory, final AEItemKey itemKey, final int amount, final int slot) {
        final ItemStack existing = inventory.getItem(slot);
        final ItemStack prototype = itemKey.toStack();
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, prototype)) {
            return amount;
        }

        final int currentAmount = existing.isEmpty() ? 0 : existing.getCount();
        final int inserted = Math.min(amount, prototype.getMaxStackSize() - currentAmount);
        if (inserted <= 0) {
            return amount;
        }

        if (existing.isEmpty()) {
            inventory.setItem(slot, itemKey.toStack(inserted));
        } else {
            existing.grow(inserted);
        }
        return amount - inserted;
    }

    public static boolean contains(final Inventory inventory, final ItemStack pickedStack) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (ItemStack.isSameItemSameComponents(inventory.getItem(slot), pickedStack)) {
                return true;
            }
        }
        return false;
    }
}
