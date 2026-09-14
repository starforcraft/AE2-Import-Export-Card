package com.ultramega.ae2importexportcard.compat.curios;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface CuriosTerminalTicker {
    void ae2ImportExportCard$tickEquippedTerminal(ServerPlayer player, ItemStack stack);
}
