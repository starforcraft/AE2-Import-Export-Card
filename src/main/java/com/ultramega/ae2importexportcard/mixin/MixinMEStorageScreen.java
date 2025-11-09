package com.ultramega.ae2importexportcard.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.ultramega.ae2importexportcard.AE2ImportExportCard;
import com.ultramega.ae2importexportcard.registry.ModItems;
import com.ultramega.ae2importexportcard.screen.UpgradeItemButton;
import com.ultramega.ae2importexportcard.util.UpgradeInterface;
import com.ultramega.ae2importexportcard.util.UpgradeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageScreen.class)
public abstract class MixinMEStorageScreen extends AEBaseScreen {
    @Unique
    @Final
    private final UpgradeItemButton[] ae2importExportCard$upgradeCardButton = new UpgradeItemButton[2];

    public MixinMEStorageScreen(AEBaseMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    protected void MEStorageScreenConstructor(MEStorageMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        if (menu.getType().equals(MEStorageMenu.WIRELESS_TYPE) || menu instanceof CraftingTermMenu) {
            final UpgradeInterface upgradeInterface = ((UpgradeInterface) ((MEStorageScreen<?>) (Object) this).getMenu());

            this.ae2importExportCard$upgradeCardButton[0] = new UpgradeItemButton(btn -> upgradeInterface.ae2ImportExportCard$openMenu(UpgradeType.IMPORT),
                    ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/import_card.png"));
            this.addToLeftToolbar(this.ae2importExportCard$upgradeCardButton[0]);
            this.ae2importExportCard$upgradeCardButton[0].setMessage(Component.translatable(ModItems.IMPORT_CARD.get().getDescriptionId()));

            this.ae2importExportCard$upgradeCardButton[1] = new UpgradeItemButton(btn -> upgradeInterface.ae2ImportExportCard$openMenu(UpgradeType.EXPORT),
                    ResourceLocation.fromNamespaceAndPath(AE2ImportExportCard.MODID, "textures/gui/export_card.png"));
            this.addToLeftToolbar(this.ae2importExportCard$upgradeCardButton[1]);
            this.ae2importExportCard$upgradeCardButton[1].setMessage(Component.translatable(ModItems.EXPORT_CARD.get().getDescriptionId()));
        }
    }

    @Inject(at = @At("TAIL"), method = "updateBeforeRender")
    protected void updateBeforeRender(CallbackInfo ci) {
        final ITerminalHost host = ((MEStorageScreen<?>) (Object) this).getMenu().getHost();
        if (this.ae2importExportCard$upgradeCardButton[0] != null) {
            this.ae2importExportCard$upgradeCardButton[0].setVisibility(host.getInstalledUpgrades(ModItems.IMPORT_CARD.get()) > 0);
        }
        if (this.ae2importExportCard$upgradeCardButton[1] != null) {
            this.ae2importExportCard$upgradeCardButton[1].setVisibility(host.getInstalledUpgrades(ModItems.EXPORT_CARD.get()) > 0);
        }
    }
}
