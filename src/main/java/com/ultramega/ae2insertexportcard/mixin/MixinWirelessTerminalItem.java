package com.ultramega.ae2insertexportcard.mixin;

import appeng.items.tools.powered.WirelessTerminalItem;
import com.ultramega.ae2insertexportcard.compat.WirelessTerminalTicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WirelessTerminalItem.class)
public abstract class MixinWirelessTerminalItem extends Item {
    public MixinWirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            WirelessTerminalTicker.tick(stack, player, slotId);
        }
    }
}
