package com.ultramega.ae2importexportcard.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.util.UpgradeInterface;
import com.ultramega.ae2importexportcard.util.UpgradeType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageMenu.class)
public abstract class MixinMEStorageMenu extends AEBaseMenu implements UpgradeInterface {
    @Unique
    private static final String IMPORT_MENU = "importMenu";
    @Unique
    private static final String EXPORT_MENU = "exportMenu";

    public MixinMEStorageMenu(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/api/storage/ITerminalHost;Z)V")
    protected void MEStorageMenuConstructor(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host, boolean bindInventory, CallbackInfo ci) {
        if (menuType.equals(MEStorageMenu.WIRELESS_TYPE) || (Object) (this) instanceof CraftingTermMenu) {
            this.registerClientAction(IMPORT_MENU, () -> this.ae2ImportExportCard$openMenu(UpgradeType.IMPORT));
            this.registerClientAction(EXPORT_MENU, () -> this.ae2ImportExportCard$openMenu(UpgradeType.EXPORT));
        }
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
}
