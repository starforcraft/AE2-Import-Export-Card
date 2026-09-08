package com.ultramega.ae2importexportcard.mixin;

import com.ultramega.ae2importexportcard.registry.ModItems;
import com.ultramega.ae2importexportcard.screen.BlockPickerAmountScreen;
import com.ultramega.ae2importexportcard.screen.UpgradeItemButton;
import com.ultramega.ae2importexportcard.util.UpgradeInterface;
import com.ultramega.ae2importexportcard.util.UpgradeType;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.ultramega.ae2importexportcard.AE2ImportExportCard.makeId;

@Mixin(MEStorageScreen.class)
public abstract class MEStorageScreenMixin<C extends MEStorageMenu> extends AEBaseScreen<C> {
    @Unique
    @Final
    private final UpgradeItemButton[] ae2ImportExportCard$upgradeCardButton = new UpgradeItemButton[3];

    public MEStorageScreenMixin(final C menu, final Inventory playerInventory, final Component title, final ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    protected void MEStorageScreenConstructor(MEStorageMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        final UpgradeInterface upgradeInterface = ((UpgradeInterface) ((MEStorageScreen<?>) (Object) this).getMenu());

        this.ae2ImportExportCard$upgradeCardButton[0] = new UpgradeItemButton(_ -> upgradeInterface.ae2ImportExportCard$openMenu(UpgradeType.IMPORT, SlotSemantics.UPGRADE.id()),
            makeId("textures/gui/import_card.png"));
        this.addToLeftToolbar(this.ae2ImportExportCard$upgradeCardButton[0]);
        this.ae2ImportExportCard$upgradeCardButton[0].setMessage(Component.translatable(ModItems.IMPORT_CARD.get().getDescriptionId()));

        this.ae2ImportExportCard$upgradeCardButton[1] = new UpgradeItemButton(_ -> upgradeInterface.ae2ImportExportCard$openMenu(UpgradeType.EXPORT, SlotSemantics.UPGRADE.id()),
            makeId("textures/gui/export_card.png"));
        this.addToLeftToolbar(this.ae2ImportExportCard$upgradeCardButton[1]);
        this.ae2ImportExportCard$upgradeCardButton[1].setMessage(Component.translatable(ModItems.EXPORT_CARD.get().getDescriptionId()));

        this.ae2ImportExportCard$upgradeCardButton[2] = new UpgradeItemButton(btn -> this.switchToScreen(new BlockPickerAmountScreen<>(this,
            upgradeInterface.ae2ImportExportCard$getBlockPickerAmount(),
            upgradeInterface::ae2ImportExportCard$setBlockPickerAmount)),
            makeId("textures/gui/block_picker_card.png"));
        this.addToLeftToolbar(this.ae2ImportExportCard$upgradeCardButton[2]);
    }

    @Inject(at = @At("TAIL"), method = "updateBeforeRender")
    protected void updateBeforeRender(CallbackInfo ci) {
        final ITerminalHost host = ((MEStorageScreen<?>) (Object) this).getMenu().getHost();
        if (this.ae2ImportExportCard$upgradeCardButton[0] != null) {
            this.ae2ImportExportCard$upgradeCardButton[0].setVisibility(host.getInstalledUpgrades(ModItems.IMPORT_CARD.get()) > 0);
        }
        if (this.ae2ImportExportCard$upgradeCardButton[1] != null) {
            this.ae2ImportExportCard$upgradeCardButton[1].setVisibility(host.getInstalledUpgrades(ModItems.EXPORT_CARD.get()) > 0);
        }
        if (this.ae2ImportExportCard$upgradeCardButton[2] != null) {
            final int amount = ((UpgradeInterface) this.getMenu()).ae2ImportExportCard$getBlockPickerAmount();
            this.ae2ImportExportCard$upgradeCardButton[2].setVisibility(host.getInstalledUpgrades(ModItems.BLOCK_PICKER_CARD.get()) > 0);
            this.ae2ImportExportCard$upgradeCardButton[2].setMessage(Component.translatable("gui.ae2importexportcard.block_picker_amount.value", amount));
        }
    }
}
