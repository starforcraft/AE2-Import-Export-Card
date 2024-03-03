package com.ultramega.ae2insertexportcard.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.ultramega.ae2insertexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2insertexportcard.util.UpgradeInterface;
import com.ultramega.ae2insertexportcard.util.UpgradeType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageMenu.class)
public abstract class MixinMEStorageMenuMenu extends AEBaseMenu implements UpgradeInterface {
    @Unique
    private static final String INSERT_MENU = "insertMenu";
    @Unique
    private static final String EXPORT_MENU = "exportMenu";

    public MixinMEStorageMenuMenu(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/api/storage/ITerminalHost;Z)V")
    protected void MEStorageMenuConstructor(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host, boolean bindInventory, CallbackInfo ci) {
        if(menuType.equals(MEStorageMenu.WIRELESS_TYPE) || (Object)(this) instanceof CraftingTermMenu) {
            registerClientAction(INSERT_MENU, () -> ae2InsertExportCard$openMenu(UpgradeType.INSERT));
            registerClientAction(EXPORT_MENU, () -> ae2InsertExportCard$openMenu(UpgradeType.EXPORT));
        }
    }

    @Unique
    @Override
    public void ae2InsertExportCard$openMenu(UpgradeType type) {
        if (isClientSide()) {
            sendClientAction(type == UpgradeType.INSERT ? INSERT_MENU : EXPORT_MENU);
            return;
        }
        MenuOpener.open(type == UpgradeType.INSERT ? UpgradeContainerMenu.TYPE_INSERT : UpgradeContainerMenu.TYPE_EXPORT, getPlayer(), getLocator());
    }
}
