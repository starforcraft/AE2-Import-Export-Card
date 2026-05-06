package com.ultramega.ae2importexportcard.mixin;

import com.ultramega.ae2importexportcard.container.UpgradeContainerMenu;
import com.ultramega.ae2importexportcard.util.UpgradeInterface;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import appeng.api.storage.ITerminalHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.guisync.ClientActionKey;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.network.codec.ByteBufCodecs;
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
    private static final ClientActionKey<String> IMPORT_MENU = new ClientActionKey<>("ImportMenu");

    @Unique
    private static final ClientActionKey<String> EXPORT_MENU = new ClientActionKey<>("ExportMenu");

    public MixinMEStorageMenu(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/api/storage/ITerminalHost;Z)V")
    protected void MEStorageMenuConstructor(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host, boolean bindInventory, CallbackInfo ci) {
        this.registerClientAction(IMPORT_MENU, ByteBufCodecs.STRING_UTF8, (semantic) -> this.ae2ImportExportCard$openMenu(UpgradeType.IMPORT, semantic));
        this.registerClientAction(EXPORT_MENU, ByteBufCodecs.STRING_UTF8, (semantic) -> this.ae2ImportExportCard$openMenu(UpgradeType.EXPORT, semantic));
    }

    @Unique
    @Override
    public void ae2ImportExportCard$openMenu(UpgradeType type, String semantic) {
        if (this.isClientSide()) {
            this.sendClientAction(type == UpgradeType.IMPORT ? IMPORT_MENU : EXPORT_MENU, semantic);
            return;
        }
        MenuOpener.open(type == UpgradeType.IMPORT ? UpgradeContainerMenu.TYPE_IMPORT : UpgradeContainerMenu.TYPE_EXPORT, this.getPlayer(), this.getLocator());
    }
}
