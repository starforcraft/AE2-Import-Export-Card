package com.ultramega.ae2insertexportcard.mixin;

import com.ultramega.ae2insertexportcard.compat.WirelessTerminalTicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "de.mari_023.ae2wtlib.wut.ItemWUT")
public class MixinWUTItem {
    @Inject(method = { "inventoryTick", "m_6883_" }, at = @At("HEAD"), remap = false)
    private void ae2insertExportCard$inventoryTick(ItemStack stack, Level level, Entity entity, int slotId,
            boolean isSelected, CallbackInfo ci) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            WirelessTerminalTicker.tick(stack, player, slotId);
        }
    }
}
