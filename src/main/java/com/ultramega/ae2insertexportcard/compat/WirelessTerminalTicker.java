package com.ultramega.ae2insertexportcard.compat;

import appeng.api.networking.IGrid;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.ultramega.ae2insertexportcard.AE2InsertExportCard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class WirelessTerminalTicker {
    private WirelessTerminalTicker() {
    }

    public static void tick(ItemStack stack, ServerPlayer player, int slotId) {
        if (!stack.hasTag() || !(stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            return;
        }

        var menuHost = wirelessTerminalItem.getMenuHost(player, slotId, stack, null);
        if (!(menuHost instanceof WirelessTerminalMenuHost host) || !host.rangeCheck()) {
            return;
        }

        var node = host.getActionableNode();
        IGrid grid = node == null ? null : node.getGrid();
        if (grid != null) {
            AE2InsertExportCard.tickWireless(stack, grid, host, player);
        }
    }
}
