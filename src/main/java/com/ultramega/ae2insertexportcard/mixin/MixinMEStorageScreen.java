package com.ultramega.ae2insertexportcard.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.ultramega.ae2insertexportcard.AE2InsertExportCard;
import com.ultramega.ae2insertexportcard.registry.ModItems;
import com.ultramega.ae2insertexportcard.screen.UpgradeItemButton;
import com.ultramega.ae2insertexportcard.util.UpgradeInterface;
import com.ultramega.ae2insertexportcard.util.UpgradeType;
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
    private UpgradeItemButton[] ae2insertExportCard$upgradeCardButton = new UpgradeItemButton[2];

    public MixinMEStorageScreen(AEBaseMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    protected void MEStorageScreenConstructor(MEStorageMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        if(menu.getType().equals(MEStorageMenu.WIRELESS_TYPE) || menu instanceof CraftingTermMenu) {
            ae2insertExportCard$upgradeCardButton[0] = new UpgradeItemButton(btn -> ((UpgradeInterface) ((MEStorageScreen<?>) (Object) this).getMenu()).ae2InsertExportCard$openMenu(UpgradeType.INSERT),
                    new ResourceLocation(AE2InsertExportCard.MOD_ID, "textures/item/insert_card.png"));
            addToLeftToolbar(ae2insertExportCard$upgradeCardButton[0]);
            ae2insertExportCard$upgradeCardButton[0].setMessage(Component.translatable(ModItems.INSERT_CARD.get().getDescriptionId()));

            ae2insertExportCard$upgradeCardButton[1] = new UpgradeItemButton(btn -> ((UpgradeInterface) ((MEStorageScreen<?>) (Object) this).getMenu()).ae2InsertExportCard$openMenu(UpgradeType.EXPORT),
                    new ResourceLocation(AE2InsertExportCard.MOD_ID, "textures/item/export_card.png"));
            addToLeftToolbar(ae2insertExportCard$upgradeCardButton[1]);
            ae2insertExportCard$upgradeCardButton[1].setMessage(Component.translatable(ModItems.EXPORT_CARD.get().getDescriptionId()));
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if(ae2insertExportCard$upgradeCardButton[0] != null) {
            ae2insertExportCard$upgradeCardButton[0].setVisibility(((MEStorageScreen<?>) (Object) this).getMenu().getHost().getInstalledUpgrades(ModItems.INSERT_CARD.get()) > 0);
            ae2insertExportCard$upgradeCardButton[1].setVisibility(((MEStorageScreen<?>) (Object) this).getMenu().getHost().getInstalledUpgrades(ModItems.EXPORT_CARD.get()) > 0);
        }
    }
}
