package com.ultramega.ae2importexportcard.mixin;

import com.ultramega.ae2importexportcard.network.BlockPickerData;
import com.ultramega.ae2importexportcard.util.BlockPickerHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    public LocalPlayer player;
    @Shadow
    public ClientLevel level;
    @Shadow
    public MultiPlayerGameMode gameMode;
    @Shadow
    public HitResult hitResult;

    @Inject(method = "pickBlockOrEntity", at = @At("TAIL"))
    private void ae2ImportExportCard$pickBlock(final CallbackInfo ci) {
        if (this.player == null || this.level == null || this.gameMode == null || this.gameMode.getPlayerMode() != GameType.SURVIVAL || !(this.hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        final BlockState blockState = this.level.getBlockState(blockHitResult.getBlockPos());
        final ItemStack pickedStack = blockState.getCloneItemStack(blockHitResult.getBlockPos(), this.level, false, this.player);
        if (!pickedStack.isEmpty()
            && !BlockPickerHandler.contains(this.player.getInventory(), pickedStack)
            && this.player.getInventory().getFreeSlot() != Inventory.NOT_FOUND_INDEX
        ) {
            ClientPacketDistributor.sendToServer(new BlockPickerData(blockHitResult.getBlockPos()));
        }
    }
}
