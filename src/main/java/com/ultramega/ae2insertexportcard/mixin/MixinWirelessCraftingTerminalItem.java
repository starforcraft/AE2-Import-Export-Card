package com.ultramega.ae2insertexportcard.mixin;

import appeng.items.tools.powered.WirelessCraftingTerminalItem;
import com.ultramega.ae2insertexportcard.compat.WirelessTerminalTicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE2WTLib adds an inventoryTick override to the crafting terminal. Injecting
 * after that optional mixin preserves both behaviours without competing
 * overwrites. Without AE2WTLib there is no declared method here and the base
 * wireless-terminal mixin handles the inherited tick.
 */
@Mixin(value = WirelessCraftingTerminalItem.class, priority = 900)
public class MixinWirelessCraftingTerminalItem {
    @Inject(method = { "inventoryTick", "m_6883_" }, at = @At("HEAD"), remap = false, require = 0)
    private void ae2insertExportCard$inventoryTick(ItemStack stack, Level level, Entity entity, int slotId,
            boolean isSelected, CallbackInfo ci) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            WirelessTerminalTicker.tick(stack, player, slotId);
        }
    }
}
