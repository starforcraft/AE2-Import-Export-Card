package com.ultramega.ae2importexportcard.mixin;

import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.util.BlockPickerCardConfig;
import com.ultramega.ae2importexportcard.util.UpgradeInterface;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import appeng.api.storage.ITerminalHost;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageMenu.class)
public abstract class MEStorageMenuMixin extends AEBaseMenu implements UpgradeInterface {
    @Unique
    private static final String IMPORT_MENU = "importMenu";
    @Unique
    private static final String EXPORT_MENU = "exportMenu";
    @Unique
    private static final String SET_BLOCK_PICKER_AMOUNT = "setBlockPickerAmount";

    public MEStorageMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/api/storage/ITerminalHost;Z)V")
    protected void MEStorageMenuConstructor(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host, boolean bindInventory, CallbackInfo ci) {
        this.registerClientAction(IMPORT_MENU, () -> this.ae2ImportExportCard$openMenu(UpgradeType.IMPORT));
        this.registerClientAction(EXPORT_MENU, () -> this.ae2ImportExportCard$openMenu(UpgradeType.EXPORT));
        this.registerClientAction(SET_BLOCK_PICKER_AMOUNT, Integer.class, this::ae2ImportExportCard$setBlockPickerAmount);
    }

    @Unique
    @Override
    public void ae2ImportExportCard$openMenu(UpgradeType type) {
        if (this.isClientSide()) {
            this.sendClientAction(type == UpgradeType.IMPORT ? IMPORT_MENU : EXPORT_MENU);
            return;
        }
        MenuOpener.open(type == UpgradeType.IMPORT ? UpgradeContainerMenu.TYPE_IMPORT : UpgradeContainerMenu.TYPE_EXPORT, this.getPlayer(), this.getLocator());
    }

    @Unique
    @Override
    public int ae2ImportExportCard$getBlockPickerAmount() {
        if (((MEStorageMenu) (Object) this).getHost() instanceof WirelessTerminalMenuHost<?> host) {
            return BlockPickerCardConfig.getAmountFromTerminal(host.getItemStack());
        }
        return BlockPickerCardConfig.DEFAULT_AMOUNT;
    }

    @Unique
    @Override
    public void ae2ImportExportCard$setBlockPickerAmount(final int amount) {
        if (this.isClientSide()) {
            if (((MEStorageMenu) (Object) this).getHost() instanceof WirelessTerminalMenuHost<?> host) {
                BlockPickerCardConfig.setAmountOnTerminal(host.getItemStack(), amount);
            }
            this.sendClientAction(SET_BLOCK_PICKER_AMOUNT, amount);
            return;
        }
        if (((MEStorageMenu) (Object) this).getHost() instanceof WirelessTerminalMenuHost<?> host) {
            if (BlockPickerCardConfig.setAmountOnTerminal(host.getItemStack(), amount)) {
                this.broadcastChanges();
            }
        }
    }
}
