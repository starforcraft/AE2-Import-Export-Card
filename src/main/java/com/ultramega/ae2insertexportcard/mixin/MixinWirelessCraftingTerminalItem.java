package com.ultramega.ae2insertexportcard.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.Future;

@Mixin(WirelessCraftingTerminalItem.class)
public abstract class MixinWirelessCraftingTerminalItem extends Item {

    public MixinWirelessCraftingTerminalItem(Properties properties) {
        super(properties);
    }

    @Unique
    private Future<ICraftingPlan> ae2insertExportCard$craftingJob;

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
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
