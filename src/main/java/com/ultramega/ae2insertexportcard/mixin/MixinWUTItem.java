package com.ultramega.ae2insertexportcard.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Future;

@Mixin(targets = "de.mari_023.ae2wtlib.wut.ItemWUT")
public class MixinWUTItem {
    @Unique
    private Future<ICraftingPlan> ae2insertExportCard$craftingJob;

    @Inject(method = "inventoryTick", at = @At("HEAD"), remap = false)
    public void ae2insertExportCard_inventoryTick(ItemStack stack, Level level, Entity entity, int slotId,
            boolean isSelected, CallbackInfo ci) {
        if (level.isClientSide())
            return;
        if (!(entity instanceof ServerPlayer player))
            return;

        if (!stack.hasTag())
            return;

        // Check if in access point range
        boolean inRange = false;
        IGrid mutableGrid = null;
        WirelessTerminalMenuHost host = null;

        if (stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem) {
            var menuHost = wirelessTerminalItem.getMenuHost(player, slotId, stack, null);
            if (menuHost != null) {
                try {
                    host = (WirelessTerminalMenuHost) menuHost;
                    boolean check = host.rangeCheck();
                    if (check) {
                        inRange = true;
                        var node = host.getActionableNode();
                        if (node != null) {
                            mutableGrid = node.getGrid();
                        }
                    }
                } catch (ClassCastException e) {
                    // Ignore
                }
            }
        }

        final IGrid grid = mutableGrid;

        if (!inRange)
            return;

        com.ultramega.ae2insertexportcard.AE2InsertExportCard.tickWireless(stack, grid, host, player,
                () -> this.ae2insertExportCard$craftingJob,
                (job) -> this.ae2insertExportCard$craftingJob = job);
    }
}
