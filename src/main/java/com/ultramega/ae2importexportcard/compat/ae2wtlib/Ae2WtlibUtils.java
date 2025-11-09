package com.ultramega.ae2importexportcard.compat.ae2wtlib;

import appeng.api.networking.IGrid;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Ae2WtlibUtils {
    public static IGrid getGridFromStack(WirelessTerminalItem wirelessTerminalItem, Player player, ItemStack stack) {
        if (wirelessTerminalItem.getMenuHost(player, MenuLocators.forStack(stack), null) instanceof WTMenuHost menuHost) {
            var node = menuHost.getActionableNode();
            if (node != null) {
                return node.getGrid();
            }
        }

        return null;
    }
}
